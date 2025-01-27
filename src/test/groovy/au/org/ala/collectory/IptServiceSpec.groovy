package au.org.ala.collectory

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import grails.testing.gorm.DomainUnitTest

import java.sql.Timestamp

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
                    "guid"                 : { eml -> eml.@packageId.toString() },
                    "name"                 : { eml -> eml.dataset.title.toString() },
                    "pubDescription"       : { eml -> "Sample description" },
                    "methodStepDescription": { eml -> "Sample method step description" }
            ].keySet()
        }
    }

    void "test merge updates existing resource and adds contacts"() {
        given: "A provider, an existing resource, and updates with new contacts"
        def provider = new DataProvider(
                uid: "dp1",
                name: "Test Provider",
                gbifCountryToAttribute: "AU",
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
                        resource       : new DataResource(websiteUrl: "http://example.org/resource"),
                        contacts       : [existingContact, newContact],
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
                        resource       : newResource,
                        contacts       : [newContact],
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
                        resource       : new DataResource(websiteUrl: "http://example.org/duplicate-resource"),
                        contacts       : [existingContact],
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
                gbifCountryToAttribute: "AU",
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
                        resource       : existingResource,
                        contacts       : [extractedContact],
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
                        resource       : existingResource,
                        contacts       : [existingContact],
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

    void "test merge cleans up obsolete contacts"() {
        given: "A resource with existing contacts"
        def provider = new DataProvider(uid: "dp1", name: "Provider with Obsolete Contacts",
                gbifCountryToAttribute: "AU",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def oldContact = new Contact(firstName: "Old", lastName: "Contact", email: "old.contact@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)
        def validContact = new Contact(firstName: "Valid", lastName: "Contact", email: "valid.contact@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)

        def resource = new DataResource(uid: "dr1", name: "Test", websiteUrl: "http://example.org/resource", userLastModified: "testUser").save(flush: true, failOnError: true)
        resource.addToContacts(oldContact, null, false, false, "testUser")
        resource.addToContacts(validContact, null, false, false, "testUser")
        provider.addToResources(resource)
        provider.save(flush: true, failOnError: true)

        def updates = [
                [
                        resource       : resource,
                        contacts       : [validContact],
                        primaryContacts: []
                ]
        ]

        when: "The merge method is called"
        def result = service.merge(provider, updates, true, false, "testUser", true)

        then: "Obsolete contacts are removed"
        result.size() == 1
        result[0].contacts.size() == 1
        result[0].contacts[0].contact.email == "valid.contact@example.org"
    }

    void "test merge keeps contacts unchanged if data is the same"() {
        given: "A resource with existing contacts"
        def provider = new DataProvider(uid: "dp2", name: "Provider with Same Data",
                gbifCountryToAttribute: "AU",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def contact = new Contact(firstName: "Same", lastName: "Contact", email: "same.contact@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)

        def resource = new DataResource(uid: "dr2", name: "Test Same Data", websiteUrl: "http://example.org/resource-same", userLastModified: "testUser").save(flush: true, failOnError: true)
        resource.addToContacts(contact, null, false, false, "testUser")
        provider.addToResources(resource)
        provider.save(flush: true, failOnError: true)

        def updates = [
                [
                        resource       : resource,
                        contacts       : [contact],
                        primaryContacts: []
                ]
        ]

        when: "The merge method is called with identical data"
        def result = service.merge(provider, updates, true, false, "testUser", true)

        then: "Contacts remain unchanged"
        result.size() == 1
        result[0].contacts.size() == 1
        result[0].contacts[0].contact.email == "same.contact@example.org"
    }

    void "test merge keeps contacts unchanged if data is the same"() {
        given: "A resource with existing contacts"
        def provider = new DataProvider(uid: "dp2", name: "Provider with Same Data",
                gbifCountryToAttribute: "AU",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def contact = new Contact(firstName: "Same", lastName: "Contact", email: "same.contact@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)

        def resource = new DataResource(uid: "dr2", name: "Test Same Data", websiteUrl: "http://example.org/resource-same", userLastModified: "testUser").save(flush: true, failOnError: true)
        resource.addToContacts(contact, null, false, false, "testUser")
        provider.addToResources(resource)
        provider.save(flush: true, failOnError: true)

        def updates = [
                [
                        resource       : resource,
                        contacts       : [contact],
                        primaryContacts: []
                ]
        ]

        when: "The merge method is called with identical data"
        def result = service.merge(provider, updates, true, false, "testUser", true)

        then: "Contacts remain unchanged"
        result.size() == 1
        result[0].contacts.size() == 1
        result[0].contacts[0].contact.email == "same.contact@example.org"
    }

    void "test merge does not remove shared contact from other resources"() {
        given: "Two resources sharing a contact"
        def provider = new DataProvider(uid: "dp3", name: "Provider with Shared Contact",
                gbifCountryToAttribute: "AU",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def sharedContact = new Contact(firstName: "Shared", lastName: "Contact", email: "shared.contact@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)

        def resource1 = new DataResource(uid: "dr3", name: "Resource 1", websiteUrl: "http://example.org/resource1", userLastModified: "testUser").save(flush: true, failOnError: true)
        resource1.addToContacts(sharedContact, null, false, false, "testUser")

        def resource2 = new DataResource(uid: "dr4", name: "Resource 2", websiteUrl: "http://example.org/resource2", userLastModified: "testUser").save(flush: true, failOnError: true)
        resource2.addToContacts(sharedContact, null, false, false, "testUser")

        provider.addToResources(resource1)
        provider.addToResources(resource2)
        provider.save(flush: true, failOnError: true)

        def updates = [
                [
                        resource       : resource1,
                        contacts       : [],
                        primaryContacts: []
                ]
        ]

        when: "The merge method is called and a contact is removed from one resource"
        def result = service.merge(provider, updates, true, false, "testUser", true)

        then: "The contact is removed from the first resource but remains in the second"
        result.size() == 1
        result[0].contacts.size() == 0
        resource2.contacts.size() == 1
        resource2.contacts[0].contact.email == "shared.contact@example.org"
    }

    void "test merge preserves all current contacts"() {
        given: "A resource with multiple existing contacts"
        def provider = new DataProvider(uid: "dp6", name: "Provider with Multiple Contacts",
                gbifCountryToAttribute: "AU",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def contact1 = new Contact(firstName: "Contact", lastName: "One", email: "contact.one@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)
        def contact2 = new Contact(firstName: "Contact", lastName: "Two", email: "contact.two@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)

        def resource = new DataResource(uid: "dr7", name: "Test Multiple Contacts", websiteUrl: "http://example.org/resource-multiple", userLastModified: "testUser").save(flush: true, failOnError: true)
        resource.addToContacts(contact1, null, false, false, "testUser")
        resource.addToContacts(contact2, null, false, false, "testUser")
        provider.addToResources(resource)
        provider.save(flush: true, failOnError: true)

        def updates = [
                [
                        resource       : resource,
                        contacts       : [contact1, contact2],
                        primaryContacts: []
                ]
        ]

        when: "The merge method is called"
        def result = service.merge(provider, updates, true, false, "testUser", true)

        then: "All contacts are preserved"
        result.size() == 1
        result[0].contacts.size() == 2
        result[0].contacts*.contact.email.containsAll(["contact.one@example.org", "contact.two@example.org"])
    }

    void "test merge does not leave orphaned contacts"() {
        given: "Two resources sharing a contact and one exclusive contact for each resource"
        def provider = new DataProvider(uid: "dp7", name: "Provider with Orphan Check",
                gbifCountryToAttribute: "AU",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def sharedContact = new Contact(firstName: "Shared", lastName: "Contact", email: "shared.contact@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)
        def exclusiveContact1 = new Contact(firstName: "Exclusive", lastName: "One", email: "exclusive.one@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)
        def exclusiveContact2 = new Contact(firstName: "Exclusive", lastName: "Two", email: "exclusive.two@example.org", userLastModified: "testUser").save(flush: true, failOnError: true)

        def resource1 = new DataResource(uid: "dr8", name: "Resource 1", websiteUrl: "http://example.org/resource1", userLastModified: "testUser").save(flush: true, failOnError: true)
        def resource2 = new DataResource(uid: "dr9", name: "Resource 2", websiteUrl: "http://example.org/resource2", userLastModified: "testUser").save(flush: true, failOnError: true)

        resource1.addToContacts(sharedContact, null, false, false, "testUser")
        resource1.addToContacts(exclusiveContact1, null, false, false, "testUser")
        resource2.addToContacts(sharedContact, null, false, false, "testUser")
        resource2.addToContacts(exclusiveContact2, null, false, false, "testUser")

        provider.addToResources(resource1)
        provider.addToResources(resource2)
        provider.save(flush: true, failOnError: true)

        def updatesForResource1 = [
                [
                        resource       : resource1,
                        contacts       : [sharedContact], // Delete the exclusive contact for resource1
                        primaryContacts: []
                ]
        ]

        def updatesForResource2 = [
                [
                        resource       : resource2,
                        contacts       : [sharedContact], // Delete the exclusive contact for resource2
                        primaryContacts: []
                ]
        ]

        when: "The merge method is called for both resources"
        def result1 = service.merge(provider, updatesForResource1, true, false, "testUser", true)
        def result2 = service.merge(provider, updatesForResource2, true, false, "testUser", true)

        then: "No orphaned contacts remain"
        result1.size() == 1
        result2.size() == 1

        // Check that the exclusive contacts were removed
        result1[0].contacts.size() == 1
        result1[0].contacts[0].contact.email == "shared.contact@example.org"
        result2[0].contacts.size() == 1
        result2[0].contacts[0].contact.email == "shared.contact@example.org"

        ContactFor.findByContact(exclusiveContact1) == null
        ContactFor.findByContact(exclusiveContact2) == null
        ContactFor.findAllByContact(sharedContact).size() == 2
    }

    void "test updateFields removes values when new value is null"() {
        given: "A resource with existing values"
        def resource = new DataResource(
                uid: "dr1",
                name: "Test Resource",
                websiteUrl: "http://example.org/resource",
                pubDescription: "Description to be removed",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def update = [
                resource: new DataResource(
                        websiteUrl: "http://example.org/resource",
                        pubDescription: null // Null value should clear the field
                )
        ]

        when: "The updateFields method is called"
        service.updateFields(resource, update, "testUser")

        then: "The field with null value is cleared"
        resource.pubDescription == null
    }

    void "test updateFields updates and clears fields correctly"() {
        given: "A resource with existing values and an update with new values"
        def resource = new DataResource(
                uid: "dr1",
                name: "Original Name",
                websiteUrl: "http://example.org/resource",
                pubDescription: "Original Description",
                methodStepDescription: "Original Set Description",
                userLastModified: "originalUser",
                lastChecked: new Timestamp(System.currentTimeMillis() - 10000)
        ).save(flush: true, failOnError: true)

        def update = [
                resource: new DataResource(
                        uid: "dr1",
                        websiteUrl: "http://example.org/resource",
                        name: "Updated Name",
                        pubDescription: null, // This field should be cleared
                        methodStepDescription: "Updated Set Description" // This field should be updated
                )
        ]

        when: "updateFields is called"
        def fieldsToUpdate = service.allFields()
        assert fieldsToUpdate.containsAll(["name", "pubDescription", "methodStepDescription"]) // Ensure the fields are present
        service.updateFields(resource, update.resource.properties, "testUser")

        then: "The fields are updated and cleared"
        resource.refresh()
        resource.name == "Updated Name"
        resource.pubDescription == null
        resource.methodStepDescription == "Updated Set Description"
        resource.userLastModified == "testUser"

        and: "The lastChecked field is updated"
        resource.lastChecked.time >= System.currentTimeMillis() - 1000
    }

    void "test syncContacts removes orphaned contacts"() {
        given: "A resource with existing contacts, one of which is shared with another resource"
        def resource1 = new DataResource(
                uid: "dr1",
                name: "Resource 1",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def resource2 = new DataResource(
                uid: "dr2",
                name: "Resource 2",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def sharedContact = new Contact(
                firstName: "Shared",
                lastName: "Contact",
                email: "shared.contact@example.org",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        def orphanContact = new Contact(
                firstName: "Orphan",
                lastName: "Contact",
                email: "orphan.contact@example.org",
                userLastModified: "testUser"
        ).save(flush: true, failOnError: true)

        resource1.addToContacts(sharedContact, null, false, false, "testUser")
        resource1.addToContacts(orphanContact, null, false, false, "testUser")
        resource1.save(flush: true, failOnError: true)

        resource2.addToContacts(sharedContact, null, false, false, "testUser")
        resource2.save(flush: true, failOnError: true)

        def newContacts = [sharedContact] // Only keep the shared contact

        when: "syncContacts is called"
        service.syncContacts(resource1, newContacts, [], "testUser", true)

        then: "The orphaned contact is removed from the database"
        ContactFor.findByContact(orphanContact) == null
        Contact.findByEmail("orphan.contact@example.org") == null

        and: "The shared contact is still present and associated with resource2"
        ContactFor.findByContact(sharedContact) != null
        resource2.contacts.size() == 1
        resource2.contacts[0].contact.email == "shared.contact@example.org"
    }

}

