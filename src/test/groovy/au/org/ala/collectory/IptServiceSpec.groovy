package au.org.ala.collectory

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import grails.testing.gorm.DomainUnitTest

class IptServiceSpec extends Specification implements ServiceUnitTest<IptService>, DomainUnitTest<DataProvider> {

    def setupSpec() {
        mockDomain(DataProvider)
        mockDomain(DataResource)
        mockDomain(Contact)
        mockDomain(ContactFor)
    }

    def setup() {
        service.idGeneratorService = Mock(IdGeneratorService)
        service.collectoryAuthService = Mock(CollectoryAuthService)
        service.activityLogService = Mock(ActivityLogService)
        service.collectoryAuthService.username() >> "testUser"
        service.metaClass.allFields = {
            [
                    "guid": { eml -> eml.@packageId.toString() },
                    "name": { eml -> eml.dataset.title.toString() },
                    "pubDescription": { eml -> "Sample description" }
            ].keySet()
        }
    }

    void "test merge updates existing resource and adds contacts"() {
        given: "A provider, an existing resource, and updates with new contacts"
        def provider = new DataProvider(
                uid: "dp1",
                name: "Test Provider",
                gbifCountryToAttribute: "UK",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def existingContact = new Contact(
                firstName: "John",
                lastName: "Doe",
                email: "john.doe@example.org",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def existingResource = new DataResource(
                uid: "dr1",
                websiteUrl: "http://example.org/resource",
                name: "Test Resource",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        provider.addToResources(existingResource)
        provider.save(flush: true, failOnError: true)

        def newContact = new Contact(
                firstName: "Jane",
                lastName: "Smith",
                email: "jane.smith@example.org",
                userLastModified: "testUser"
        )

        def updates = [
                [
                        resource: new DataResource(websiteUrl: "http://example.org/resource"),
                        contacts: [existingContact, newContact],
                        primaryContacts: [newContact]
                ]
        ]

        when: "The merge method is called"
        def result = service.merge(provider, updates, true, false, "testUser", true)

        then: "The existing resource is updated and the new contact is added"
        result.size() == 1
        result[0].contacts.size() == 2
        result[0].contacts*.contact.email.containsAll(["john.doe@example.org", "jane.smith@example.org"])
        result[0].contacts*.contact.lastName.containsAll(["Doe", "Smith"])
    }

    void "test merge creates new resource and adds contacts"() {
        given: "A provider and updates with a new resource and contacts"
        def provider = new DataProvider(
                uid: "dp2",
                name: "New Provider",
                gbifCountryToAttribute: "AU",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def newContact = new Contact(
                firstName: "Alice",
                lastName: "Brown",
                email: "alice.brown@example.org",
                userLastModified: "testUser"
        )

        def newResource = new DataResource(
                websiteUrl: "http://example.org/new-resource",
                name: "New Resource",
                userLastModified: "testUser"
        )

        service.idGeneratorService.getNextDataResourceId() >> "dr2"

        def updates = [
                [
                        resource: newResource,
                        contacts: [newContact],
                        primaryContacts: [newContact]
                ]
        ]

        when: "The merge method is called"
        def result = service.merge(provider, updates, true, false, "testUser", true)

        then: "The new resource is created and the contact is added"
        result.size() == 1
        result[0].uid == "dr2"
        result[0].contacts.size() == 1
        result[0].contacts[0].contact.email == "alice.brown@example.org"
        result[0].contacts[0].contact.lastName == "Brown"
    }

    void "test merge skips existing contacts with identical details"() {
        given: "A provider, an existing resource, and updates with duplicate contacts"
        def provider = new DataProvider(
                uid: "dp3",
                name: "Duplicate Test Provider",
                gbifCountryToAttribute: "US",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def existingContact = new Contact(
                firstName: "Bob",
                lastName: "White",
                email: "bob.white@example.org",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def existingResource = new DataResource(
                uid: "dr3",
                websiteUrl: "http://example.org/duplicate-resource",
                name: "Duplicate Resource",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        provider.addToResources(existingResource)
        provider.save(flush: true, failOnError: true)

        def updates = [
                [
                        resource: new DataResource(websiteUrl: "http://example.org/duplicate-resource"),
                        contacts: [existingContact],
                        primaryContacts: []
                ]
        ]

        when: "The merge method is called"
        def result = service.merge(provider, updates, true, false, "testUser", true)

        then: "No duplicate contacts are added"
        result.size() == 1
        result[0].contacts.size() == 1
        result[0].contacts[0].contact.email == "bob.white@example.org"
    }

    void "test merge persists EML-extracted contacts into resources"() {
    given: "A provider, an existing resource, and contacts extracted from EML"
    def provider = new DataProvider(
            uid: "dp1",
            name: "Provider with EML Contacts",
            gbifCountryToAttribute: "UK",
            userLastModified: "testUser"
    ).save(flush: true, failOnError: true)

    def existingResource = new DataResource(
            uid: "dr1",
            websiteUrl: "http://example.org/eml-resource",
            name: "EML Resource",
            userLastModified: "testUser"
    ).save(flush: true, failOnError: true)

    provider.addToResources(existingResource)
    provider.save(flush: true, failOnError: true)

    def extractedContact = new Contact(
            firstName: "John",
            lastName: "Doe",
            email: "john.doe@example.org",
            userLastModified: "testUser"
    )

    def updates = [
            [
                    resource: existingResource,
                    contacts: [extractedContact],
                    primaryContacts: [extractedContact]
            ]
    ]

    when: "The merge method is called"
    def result = service.merge(provider, updates, true, false, "testUser", true)

    then: "The extracted contact is persisted and associated with the resource"
    result.size() == 1
    result[0].contacts.size() == 1
    result[0].contacts[0].contact.email == "john.doe@example.org"
}

    void "test merge skips duplicate EML-extracted contacts"() {
        given: "A provider and a resource with an existing contact"
        def provider = new DataProvider(
                uid: "dp2",
                name: "Provider with Duplicate Contacts",
                gbifCountryToAttribute: "US",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def existingContact = new Contact(
                firstName: "Jane",
                lastName: "Smith",
                email: "jane.smith@example.org",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def existingResource = new DataResource(
                uid: "dr2",
                websiteUrl: "http://example.org/duplicate-eml",
                name: "Duplicate EML Resource",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        existingResource.addToContacts(existingContact, null, false, true, "testUser")
        provider.addToResources(existingResource)
        provider.save(flush: true, failOnError: true)

        def updates = [
                [
                        resource: existingResource,
                        contacts: [existingContact],
                        primaryContacts: []
                ]
        ]

        when: "The merge method is called"
        def result = service.merge(provider, updates, true, false, "testUser", true)

        then: "No duplicate contacts are added"
        result.size() == 1
        result[0].contacts.size() == 1
        result[0].contacts[0].contact.email == "jane.smith@example.org"
    }

}
