package au.org.ala.collectory

import grails.testing.services.ServiceUnitTest
import grails.testing.gorm.DomainUnitTest
import groovy.util.XmlSlurper
import org.xml.sax.SAXException
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Unroll

class EmlRenderServiceSpec extends Specification implements ServiceUnitTest<EmlRenderService>, DomainUnitTest<DataResource> {

    private static class TestDataResource extends DataResource {
        ContactFor primaryPublicContact
        Date fixedLastUpdated = new Date(0)

        @Override
        ContactFor inheritPrimaryPublicContact() {
            return primaryPublicContact
        }

        @Override
        def createdBy() {
            return this
        }

        @Override
        Date getLastUpdated() {
            return fixedLastUpdated
        }

        @Override
        long dbId() {
            return 1L
        }
    }

    def setup() {
        mockDomain(DataResource)
        mockDomain(Contact)
        mockDomain(ContactFor)
        mockDomain(Licence)
        service.grailsApplication = [
                config: [
                        grails: [serverURL: 'http://example.org'],
                        eml   : [:]
                ]
        ] as Expando
        service.providerGroupService = Stub(ProviderGroupService) {
            resolveAddress(_) >> null
        }
        service.metaClass.getLicence = { -> null }
    }

    void "getTaxonomicCoverage accepts lax taxonomy hints"() {
        given:
        def hints = "{coverage:[{kingdom:'plantae'},{kingdom:'fungi'},{kingdom:'chromista'},{kingdom:'protozoa'},{kingdom:'bacteria'}]}"

        when:
        def xml = service.getTaxonomicCoverage(hints)

        then:
        xml.toString().contains('<taxonRankValue>plantae</taxonRankValue>')
        xml.toString().contains('<taxonRankValue>bacteria</taxonRankValue>')
    }

    void "getTaxonomicCoverage tolerates missing range"() {
        given:
        def hints = "{coverage:[{kingdom:'plantae'}]}"

        when:
        def xml = service.getTaxonomicCoverage(hints)

        then:
        xml.toString().contains('<taxonRankValue>plantae</taxonRankValue>')
    }

    void "emlForResource emits packageId and omits empty GBIF sections"() {
        given:
        def resource = new TestDataResource(
                uid: 'dr1',
                name: 'Test Resource',
                version: 3,
                externalIdentifiers: [],
                consumerInstitutions: [],
                consumerCollections: [],
                userLastModified: 'tester',
                makeContactPublic: true,
                gbifDataset: true,
                isShareableWithGBIF: true
        )

        when:
        def eml = new XmlSlurper().parseText(service.emlForResource(resource))

        then:
        eml.@packageId.text()
        eml.dataset.title.text() == 'Test Resource'
        eml.depthFirst().findAll { it.name() == 'coverage' }.isEmpty()
        eml.depthFirst().findAll { it.name() == 'methods' }.isEmpty()
        eml.depthFirst().findAll { it.name() == 'intellectualRights' }.isEmpty()
        eml.depthFirst().findAll { it.name() == 'citation' }.isEmpty()
    }

    void "emlForResource renders rights citation and methods when present"() {
        given:
        new Licence(id: 1L, url: 'https://creativecommons.org/licenses/by/4.0/', name: 'Creative Commons', acronym: 'CC BY', licenceVersion: '4.0').save(validate: false)
        def resource = new TestDataResource(
                uid: 'dr2',
                name: 'Rights Resource',
                version: 1,
                externalIdentifiers: [],
                consumerInstitutions: [],
                consumerCollections: [],
                rights: 'Use with attribution',
                citation: 'Smith et al. 2024',
                methodStepDescription: 'Sampling method',
                qualityControlDescription: 'QC method',
                geographicDescription: 'Australia',
                westBoundingCoordinate: '1',
                eastBoundingCoordinate: '2',
                northBoundingCoordinate: '3',
                southBoundingCoordinate: '4',
                beginDate: '2024-01-01',
                endDate: '2024-12-31',
                taxonomyHints: '{"coverage":[{"kingdom":"plantae"}]}',
                userLastModified: 'tester',
                makeContactPublic: true,
                gbifDataset: true,
                isShareableWithGBIF: true
        )

        when:
        def xml = service.emlForResource(resource)
        def eml = new XmlSlurper().parseText(xml)

        then:
        xml.contains('Use with attribution')
        xml.contains('Smith et al. 2024')
        xml.contains('<citation>Smith et al. 2024</citation>')
        eml.depthFirst().findAll { it.name() == 'methods' }.size() == 1
        eml.depthFirst().findAll { it.name() == 'coverage' }.size() == 1
    }

    void "emlForResource renders the primary public contact"() {
        given:
        def resource = new TestDataResource(
                uid: 'dr3',
                name: 'Contact Resource',
                version: 1,
                externalIdentifiers: [],
                consumerInstitutions: [],
                consumerCollections: [],
                userLastModified: 'tester',
                makeContactPublic: true,
                gbifDataset: true,
                isShareableWithGBIF: true
        )
        def contact = new Contact(
                firstName: 'Jane',
                lastName: 'Doe',
                organizationName: 'Example Org',
                positionName: 'Data Manager',
                phone: '+1 234567890',
                email: 'jane.doe@example.org',
                publish: true
        )
        def contactFor = new ContactFor(contact, resource.uid, 'creator', false, true)
        resource.primaryPublicContact = contactFor

        when:
        def eml = new XmlSlurper().parseText(service.emlForResource(resource))

        then:
        def contactNode = eml.depthFirst().find { it.name() == 'contact' && it.individualName.givenName.text() == 'Jane' }
        contactNode
        contactNode.individualName.givenName.text() == 'Jane'
        contactNode.individualName.surName.text() == 'Doe'
        contactNode.organizationName.text() == 'Example Org'
        contactNode.positionName.text() == 'Data Manager'
        contactNode.phone.text() == '+1 234567890'
        contactNode.electronicMailAddress.text() == 'jane.doe@example.org'
    }

    /** A resource as loaded from the database, where Hibernate returns lastUpdated as a java.sql.Timestamp. */
    private TestDataResource resource(Map props = [:]) {
        new TestDataResource([fixedLastUpdated: java.sql.Timestamp.valueOf('2024-05-01 10:20:30'),
                              uid: 'dr1', name: 'Resource', version: 1, externalIdentifiers: [], consumerInstitutions: [],
                              consumerCollections: [], userLastModified: 'tester', makeContactPublic: true,
                              gbifDataset: true, isShareableWithGBIF: true] + props)
    }

    /** The eml block of grails-app/conf/application.yml; ala() needs at least the organizationName. */
    private void useDefaultEmlConfig() {
        service.grailsApplication.config.eml = [organizationName: 'Atlas of Living Australia',
                deliveryPoint: 'CSIRO Ecosystems Services', city: 'Canberra', administrativeArea: 'ACT',
                postalCode: 2601, country: 'Australia']
    }

    private static final Map FULL = [rights: 'Use with attribution', citation: 'Smith et al. 2024',
            pubDescription: 'A description', purpose: 'A purpose',
            methodStepDescription: 'Sampling method', qualityControlDescription: 'QC method',
            geographicDescription: 'Australia', westBoundingCoordinate: '1', eastBoundingCoordinate: '2',
            northBoundingCoordinate: '3', southBoundingCoordinate: '4', beginDate: '2024-01-01', endDate: '2024-12-31',
            taxonomyHints: '{"coverage":[{"kingdom":"plantae"}],"range":["Plantae"]}']

    void "the remote imports of the GBIF EML profile schema are read from the local copies"() {
        expect: 'any other remote location is refused, see EmlSchema.LOCAL_PROTOCOLS'
        EmlSchema.resolved
    }

    @Unroll
    void "#303 the EML of a #description resource is valid against the GBIF EML profile"() {
        given:
        useDefaultEmlConfig()
        def dr = resource(props)
        if (withContact) {
            def contact = new Contact(firstName: 'Jane', lastName: 'Doe', organizationName: 'Example Org',
                    email: 'jane.doe@example.org', publish: true)
            dr.primaryPublicContact = new ContactFor(contact, dr.uid, 'creator', false, true)
        }

        when:
        def xml = service.emlForResource(dr)
        EmlSchema.validate(xml)

        then:
        noExceptionThrown()
        new XmlSlurper().parseText(xml).dataset.title.text() == dr.name

        where:
        description             | props                                                            | withContact
        'minimal'               | [:]                                                              | false
        'fully described'       | FULL                                                             | false
        'contact'               | [:]                                                              | true
        'unicode and markup'    | FULL + [name: 'Ñandú 漢字 𠮷 <b>&amp;</b>', rights: 'a < b & "c"'] | true
    }

    @Unroll
    void "the schema check rejects EML with #error"() {
        given:
        useDefaultEmlConfig()
        def valid = service.emlForResource(resource(FULL))
        EmlSchema.validate(valid)

        when:
        EmlSchema.validate(breakIt(valid))

        then: 'the error codes reported in #303'
        def e = thrown(SAXException)
        e.message.startsWith(code + ':')

        where:
        error                  | code                     | breakIt
        'no packageId'         | 'cvc-complex-type.4'     | { String xml -> xml.replaceFirst(/ packageId="[^"]*"/, '') }
        'an empty <coverage/>' | 'cvc-complex-type.2.4.b' | { String xml -> xml.replaceFirst(/(?s)<coverage>.*<\/coverage>/, '<coverage/>') }
        'an empty <citation>'  | 'cvc-pattern-valid'      | { String xml -> xml.replaceFirst('<citation>Smith et al. 2024</citation>', '<citation></citation>') }
    }

    @PendingFeature(reason = 'getTaxonomicCoverage always writes <generalTaxonomicCoverage>, empty without a range, and <taxonomicCoverage> needs a <taxonomicClassification>; the editor saves coverage (updateTaxonomyHints) and range (updateTaxonomicRange) separately')
    @Unroll
    void "#303 taxonomy hints with #description give a valid EML"() {
        given:
        useDefaultEmlConfig()

        when:
        EmlSchema.validate(service.emlForResource(resource(taxonomyHints: hints)))

        then:
        noExceptionThrown()

        where:
        description           | hints
        'no taxonomic range'  | '{"coverage":[{"kingdom":"plantae"}]}'
        'only a range'        | '{"range":["Plantae"]}'
    }
}
