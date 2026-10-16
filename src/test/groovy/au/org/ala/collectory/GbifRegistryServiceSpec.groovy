package au.org.ala.collectory

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.HttpVersion
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicHttpResponse
import org.apache.http.util.EntityUtils
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Registration of organisations and datasets in the GBIF registry. The GBIF API is replaced by a mocked HttpClient.
 */
class GbifRegistryServiceSpec extends Specification implements ServiceUnitTest<GbifRegistryService>, DataTest {

    HttpClient client = Mock()

    /** GBIF registry entries returned by GET, by url path after the api url. */
    Map<String, Map> registry = [:]

    /** Requests with a body sent to GBIF. */
    List<HttpUriRequest> sent = []

    File biocache

    def setupSpec() {
        mockDomains(DataResource, DataProvider, Institution, Collection)
    }

    def setup() {
        new ConvertersConfigurationInitializer(grailsApplication: grailsApplication).initialize()
        config.gbifRegistrationDryRun = 'false'
        config.gbifApiUrl = 'https://api.gbif.test/v1/'
        config.gbifLicenceMappingUrl = null
        config.gbifOrphansPublisherID = null
        config.useGbifDoi = false
        config.grails.serverURL = 'https://collections.test'
        service.isoCodeService = Stub(IsoCodeService)
        service.metaClass.newHttpInstance = { -> client }
        client.execute(_ as HttpUriRequest) >> { HttpUriRequest request ->
            if (request instanceof HttpGet) {
                def entry = registry[request.URI.toString() - config.gbifApiUrl]
                return entry == null ? response(404, '') : response(200, JsonOutput.toJson(entry))
            }
            sent << request
            response(request instanceof HttpPost ? 201 : 200, '"new-key"')
        }
        biocache = File.createTempDir()
        config.biocacheServicesUrl = biocache.toURI().toString()
    }

    def cleanup() {
        biocache?.deleteDir()
    }

    private static HttpResponse response(int status, String body) {
        def response = new BasicHttpResponse(HttpVersion.HTTP_1_1, status, '')
        response.entity = new StringEntity(body, ContentType.APPLICATION_JSON)
        response
    }

    /** The JSON body of the only request sent to the given url path. */
    private Map body(String path) {
        def requests = sent.findAll { it.URI.toString() == config.gbifApiUrl + path }
        assert requests.size() == 1
        new JsonSlurper().parseText(EntityUtils.toString(requests[0].entity, 'UTF-8')) as Map
    }

    /** The biocache facet search that generateSyncBreakdown reads; a file url ignores the query string. */
    private void indexed(Map<String, Integer> counts) {
        def search = new File(biocache, 'occurrences/search')
        search.parentFile.mkdirs()
        search.text = JsonOutput.toJson([totalRecords: counts.values().sum(),
                facetResults: [[fieldResult: counts.collect { uid, count -> [fq: "data_resource_uid:\"${uid}\"", count: count] }]]])
    }

    def "#78 updating a dataset in GBIF sends its current title, description and homepage"() {
        given:
        registry['dataset/ds-78'] = [key: 'ds-78', title: 'Old title', description: 'Old description', homepage: 'https://old.example.org']
        def dr = new DataResource(uid: 'dr78', name: 'New title', pubDescription: 'New description',
                websiteUrl: 'https://example.org/dr78', licenseType: 'CC-BY', gbifRegistryKey: 'ds-78')

        when:
        service.syncDataResource(dr, 'org-1')

        then:
        def dataset = body('dataset/ds-78')
        dataset.title == 'New title'
        dataset.description == 'New description'
        dataset.homepage == 'https://example.org/dr78'
        dataset.publishingOrganizationKey == 'org-1'
    }

    @Unroll
    def "#133 the organisation sent to GBIF has latitude #expectedLatitude for #latitude"() {
        given:
        registry['organization/org-133'] = [key: 'org-133', title: 'Old', endorsingNodeKey: 'node-1']
        def dp = new DataProvider(uid: 'dp133', name: 'Provider', gbifRegistryKey: 'org-133',
                latitude: latitude, longitude: longitude)

        when:
        service.updateRegistration(dp, false, false)

        then:
        def organisation = body('organization/org-133')
        organisation.title == 'Provider'
        organisation.latitude == expectedLatitude
        organisation.longitude == expectedLongitude

        where: '-1 is the value of an unset coordinate'
        latitude | longitude || expectedLatitude | expectedLongitude
        null     | null      || null             | null
        -1d      | -1d       || null             | null
        -35.28d  | 149.13d   || -35.28d          | 149.13d
    }

    def "#117 a resource linked only to an institution is published by the institution"() {
        given:
        registry['organization/org-in'] = [key: 'org-in', title: 'OZCAM member']
        registry['dataset/ds-117'] = [key: 'ds-117', title: 'Specimens']
        def institution = new Institution(uid: 'in1', name: 'OZCAM member', gbifRegistryKey: 'org-in', userLastModified: 't').save(validate: false, flush: true)
        new DataResource(uid: 'dr117', name: 'Specimens', licenseType: 'CC-BY', gbifRegistryKey: 'ds-117',
                institution: institution, userLastModified: 't').save(validate: false, flush: true)
        indexed(dr117: 10)

        when:
        service.updateAllResources()

        then:
        body('dataset/ds-117').publishingOrganizationKey == 'org-in'
    }

    def "#117 a resource without institution or data provider is published by the orphans publisher"() {
        given:
        config.gbifOrphansPublisherID = 'org-orphans'
        registry['dataset/ds-orphan'] = [key: 'ds-orphan', title: 'Orphan']
        new DataResource(uid: 'dr118', name: 'Orphan', licenseType: 'CC-BY', gbifRegistryKey: 'ds-orphan',
                userLastModified: 't').save(validate: false, flush: true)
        indexed(dr118: 10)

        when:
        service.updateAllResources()

        then:
        body('dataset/ds-orphan').publishingOrganizationKey == 'org-orphans'
    }

    @PendingFeature(reason = '#240 the JSON is sent with new StringEntity(String), which encodes it as ISO-8859-1, so ’ and – reach GBIF as ?')
    def "#240 an apostrophe in the description of an institution reaches GBIF"() {
        given:
        registry['organization/org-240'] = [key: 'org-240', title: 'Old', endorsingNodeKey: 'node-1']
        def institution = new Institution(uid: 'in240', name: 'Museum', pubDescription: 'The museum’s collection',
                gbifRegistryKey: 'org-240')

        when:
        service.updateRegistration(institution, false, false)

        then:
        body('organization/org-240').description == 'The museum’s collection'
    }
}
