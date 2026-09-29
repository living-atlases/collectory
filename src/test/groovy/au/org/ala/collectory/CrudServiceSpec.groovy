package au.org.ala.collectory

import grails.converters.JSON
import groovy.json.JsonOutput
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Unroll

/**
 * CrudService turns the JSON of the /ws/{entity} web services into domain objects and back.
 */
class CrudServiceSpec extends Specification implements ServiceUnitTest<CrudService>, DataTest {

    def setupSpec() {
        mockDomains(DataResource, DataProvider, Institution, Collection, ExternalIdentifier, Attribution, DataHub,
                ProviderMap, Contact, ContactFor)
    }

    def setup() {
        new ConvertersConfigurationInitializer(grailsApplication: grailsApplication).initialize()
        config.resource.publicArchive.url.template = 'http://archives.test/@UID@.zip'
        config.resource.gbifExport.url.template = 'http://archives.test/gbif/@UID@.zip'
        config.grails.serverURL = 'http://collectory.test'
        service.idGeneratorService = Stub(IdGeneratorService) { getNextDataResourceId() >> 'dr1' }
        service.dataHubService = Stub(DataHubService) { listDataHubs() >> [] }
    }

    /** A resource as it is loaded from the database: associations are empty collections, not null. */
    private DataResource loadedResource(Map props = [:]) {
        new DataResource([uid: 'dr1', name: 'Resource', userLastModified: 't',
                          consumerInstitutions: [] as Set, consumerCollections: [] as Set,
                          externalIdentifiers: [] as Set] + props)
    }

    def "#159 formatSpaceSeparatedList tolerates null"() {
        expect:
        service.formatSpaceSeparatedList(null) == null
        service.formatSpaceSeparatedList('Plantae Fungi') == ['Plantae', 'Fungi']
    }

    @Unroll
    def "insertDataResource stores '#text' unchanged"() {
        when:
        def dr = service.insertDataResource(JSON.parse(JsonOutput.toJson([name: text, pubDescription: text, citation: text, rights: text])))

        then:
        !dr.hasErrors()
        dr.uid == 'dr1'
        dr.name == text
        dr.pubDescription == text
        dr.citation == text
        dr.rights == text

        where:
        text << ['Ñandú – “quoted” ’apostrophe’', '漢字 かな カナ', 'مرحبا بالعالم', 'é (NFD) and é (NFC)',
                 'narrow no-break', '4-byte 𠮷田 𝔸', 'a "double" and \'single\' quote', 'back\\slash', '<b>markup</b> & amp']
    }

    def "insertDataResource keeps a 100 character pubShortDescription of 3-byte characters"() {
        given:
        def text = '漢' * 100

        when:
        def dr = service.insertDataResource(JSON.parse(JsonOutput.toJson([name: 'n', pubShortDescription: text])))

        then:
        !dr.hasErrors()
        dr.pubShortDescription == text
    }

    def "the author of a web service change is the 'user' field, or 'Data services'"() {
        expect:
        service.insertDataResource(JSON.parse('{"name":"n","user":"someone@example.org"}')).userLastModified == 'someone@example.org'
        service.insertDataResource(JSON.parse('{"name":"n"}')).userLastModified == 'Data services'
    }

    @Unroll
    def "JSON list fields are accepted as a JSON array or as its string form: #input"() {
        when:
        def dr = service.insertDataResource(JSON.parse('{"name":"n","contentTypes":' + input + '}'))

        then:
        JSON.parse(dr.contentTypes) as List == ['point occurrence data', 'images']

        where:
        input << ['["point occurrence data","images"]', '"[\\"point occurrence data\\",\\"images\\"]"']
    }

    @Unroll
    def "timestamp #value is accepted"() {
        when:
        def dr = service.insertDataResource(JSON.parse(JsonOutput.toJson([name: 'n', lastChecked: value])))

        then:
        dr.lastChecked != null
        value == 'now' || new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dr.lastChecked) == value

        where:
        value << ['now', '2024-05-01T10:20:30']
    }

    @PendingFeature(reason = 'adjustEmptyProperties is documented to turn JSON null into Java null, but the value is then stored with toString(), so the text "null" is saved and shown')
    @Unroll
    def "a JSON null for #field never stores the text 'null'"() {
        given:
        def dr = service.insertDataResource(JSON.parse(JsonOutput.toJson([name: 'n', (field): 'some text'])))

        when:
        service.updateDataResource(dr, JSON.parse('{"' + field + '": null}'))

        then: 'whether null clears the field or leaves it unchanged is not specified; the text "null" is never right'
        dr."$field" != 'null'

        where:
        field << ['pubDescription', 'email', 'citation', 'rights']
    }

    def "#277 connectionParameters url is only returned to authorised callers"() {
        given:
        def dr = loadedResource(connectionParameters: '{"protocol":"DwCA","url":"https://ipt.example.org/archive.do?r=x","termsForUniqueKey":["occurrenceID"]}')

        when:
        def authorised = JSON.parse(service.readDataResource(dr, true).toString())
        def anonymous = JSON.parse(service.readDataResource(dr, false).toString())

        then:
        authorised.connectionParameters.url == 'https://ipt.example.org/archive.do?r=x'
        !anonymous.connectionParameters.containsKey('url')
        anonymous.connectionParameters.protocol == 'DwCA'
        anonymous.connectionParameters.termsForUniqueKey == ['occurrenceID']
    }

    def "unset coordinates (-1) are left out of the JSON"() {
        when:
        def unset = JSON.parse(service.readDataResource(loadedResource(latitude: -1d, longitude: -1d), false).toString())
        def set = JSON.parse(service.readDataResource(loadedResource(latitude: -35.28d, longitude: 149.13d), false).toString())

        then:
        !unset.containsKey('latitude')
        !unset.containsKey('longitude')
        set.latitude == -35.28d
        set.longitude == 149.13d
    }

    def "archive urls are built from the configured templates"() {
        when:
        def json = JSON.parse(service.readDataResource(loadedResource(), false).toString())

        then:
        json.publicArchiveUrl == 'http://archives.test/dr1.zip'
        json.gbifArchiveUrl == 'http://archives.test/gbif/dr1.zip'
    }

    def "unicode survives the read side too"() {
        given:
        def text = 'Ñandú 漢字 مرحبا 𠮷田'

        when:
        def json = JSON.parse(service.readDataResource(loadedResource(name: text, pubDescription: text), false).toString())

        then:
        json.name == text
        json.pubDescription == text
    }
}
