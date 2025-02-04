package au.org.ala.collectory

import grails.testing.services.ServiceUnitTest
import grails.testing.gorm.DomainUnitTest
import groovy.util.slurpersupport.NodeChild
import spock.lang.Specification

class EmlImportServiceSpec extends Specification implements ServiceUnitTest<EmlImportService>, DomainUnitTest<Contact> {

    def setup() {
        service.dataLoaderService = Mock(DataLoaderService)
        service.collectoryAuthService = Mock(CollectoryAuthService)
        service.collectoryAuthService.username() >> "testUser"
        service.metaClass.getLicence = { eml ->
            [licenseType: 'CC-BY', licenseVersion: '4.0']
        }
    }

    private NodeChild parseEml(String baseEml, String contactsXml) {
        XmlSlurper xmlSlurper = new XmlSlurper()
        return xmlSlurper.parseText(baseEml.replace("%CONTACTS%", contactsXml))
    }

    void "test extractContactsFromEml with creator and contact"() {
        given: "A base EML input loaded from a file and with a custom contact"
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
        <creator>
            <individualName>
                <givenName>John</givenName>
                <surName>Doe</surName>
            </individualName>
            <organizationName>Example Organization</organizationName>
            <electronicMailAddress>john.doe@example.org</electronicMailAddress>
        </creator>
        <contact>
            <individualName>
                <givenName>Jane</givenName>
                <surName>Smith</surName>
            </individualName>
            <electronicMailAddress>jane.smith@example.com</electronicMailAddress>
        </contact>
    '''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when: "Contacts are extracted"
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "The contacts are created successfully"
        result.contacts.size() == 2
        result.contacts*.email.contains('jane.smith@example.com')
    }

    void "test extractContactsFromEml with multiple contacts"() {
        given: "A base EML input loaded from a file with multiple contacts"
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
     <individualName>
          <givenName>John</givenName>
          <surName>Doe</surName>
     </individualName>
     <organizationName>Example Organization</organizationName>
     <electronicMailAddress>john.doe@example.org</electronicMailAddress>
</creator>
<creator>
    <individualName>
        <givenName>A.</givenName>
        <surName>Smith</surName>
    </individualName>
    <organizationName>Example Organization</organizationName>
    <address>
        <city>Example City</city>
        <administrativeArea>Example State</administrativeArea>
        <postalCode>12345</postalCode>
        <country>US</country>
    </address>
    <userId directory="https://orcid.org/">0000-0000-0000-0001</userId>
</creator>
<creator>
    <individualName>
        <givenName>B.</givenName>
        <surName>Johnson</surName>
    </individualName>
    <organizationName>Example Organization</organizationName>
    <address>
        <city>Example City</city>
        <administrativeArea>Example State</administrativeArea>
        <postalCode>12345</postalCode>
        <country>US</country>
    </address>
    <userId directory="https://orcid.org/">0000-0000-0000-0002</userId>
</creator>
<creator>
    <individualName>
        <givenName>C.</givenName>
        <surName>Williams</surName>
    </individualName>
    <organizationName>Example Organization</organizationName>
    <address>
        <city>Example City</city>
        <administrativeArea>Example State</administrativeArea>
        <postalCode>12345</postalCode>
        <country>US</country>
    </address>
    <userId directory="https://orcid.org/">0000-0000-0000-0003</userId>
</creator>
<metadataProvider>
    <individualName>
        <givenName>A.</givenName>
        <surName>Smith</surName>
    </individualName>
    <organizationName>Example Organization</organizationName>
    <address>
        <city>Example City</city>
        <administrativeArea>Example State</administrativeArea>
        <postalCode>12345</postalCode>
        <country>US</country>
    </address>
    <userId directory="https://orcid.org/">0000-0000-0000-0001</userId>
</metadataProvider>
<metadataProvider>
    <individualName>
        <givenName>B.</givenName>
        <surName>Johnson</surName>
    </individualName>
    <organizationName>Example Organization</organizationName>
    <address>
        <city>Example City</city>
        <administrativeArea>Example State</administrativeArea>
        <postalCode>12345</postalCode>
        <country>US</country>
    </address>
    <userId directory="https://orcid.org/">0000-0000-0000-0002</userId>
</metadataProvider>
<metadataProvider>
    <individualName>
        <givenName>C.</givenName>
        <surName>Williams</surName>
    </individualName>
    <organizationName>Example Organization</organizationName>
    <address>
        <city>Example City</city>
        <administrativeArea>Example State</administrativeArea>
        <postalCode>12345</postalCode>
        <country>US</country>
    </address>
    <userId directory="https://orcid.org/">0000-0000-0000-0003</userId>
</metadataProvider>
    '''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when: "Contacts are extracted"
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "All contacts are created successfully"
        result.contacts.size() == 4
        result.contacts*.lastName.containsAll(['Doe', 'Williams', 'Smith', 'Johnson'])
    }

    void "test extractContactsFromEml avoids duplicates between creator and metadataProvider"() {
        given: "A base EML input with overlapping contacts in creator and metadataProvider"
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
     <individualName>
          <givenName>John</givenName>
          <surName>Doe</surName>
     </individualName>
     <organizationName>Example Organization</organizationName>
     <electronicMailAddress>john.doe@example.org</electronicMailAddress>
</creator>
<contact>
    <individualName>
        <givenName>A.</givenName>
        <surName>Smith</surName>
    </individualName>
    <electronicMailAddress>a.smith@example.com</electronicMailAddress>
</contact>
<metadataProvider>
    <individualName>
        <givenName>A.</givenName>
        <surName>Smith</surName>
    </individualName>
    <electronicMailAddress>a.smith@example.com</electronicMailAddress>
</metadataProvider>
    '''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when: "Contacts are extracted"
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "Duplicate contacts are ignored"
        result.contacts.size() == 2
        result.primaryContacts.size() == 1
        result.contacts[1].email == 'a.smith@example.com'
        result.contacts[1].lastName == 'Smith'
    }

    void "test extractContactsFromEml processes metadataProvider without email"() {
        given: "A base EML input with metadataProvider missing electronicMailAddress"
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
     <individualName>
          <givenName>John</givenName>
          <surName>Doe</surName>
     </individualName>
     <organizationName>Example Organization</organizationName>
     <electronicMailAddress>john.doe@example.org</electronicMailAddress>
</creator>
<metadataProvider>
    <individualName>
        <givenName>B.</givenName>
        <surName>Johnson</surName>
    </individualName>
    <organizationName>Example Organization</organizationName>
</metadataProvider>
    '''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when: "Contacts are extracted"
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "Contacts without email are processed based on other fields"
        result.contacts.size() == 2
        result.contacts[0].lastName == 'Doe'
        result.contacts[0].email == 'john.doe@example.org'
        result.contacts[1].lastName == 'Johnson'
        result.contacts[1].email == ''
    }

    void "test extractContactsFromEml processes unique contacts in creator and metadataProvider"() {
        given: "A base EML input with unique contacts in creator and metadataProvider"
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
     <individualName>
          <givenName>John</givenName>
          <surName>Doe</surName>
     </individualName>
     <organizationName>Example Organization</organizationName>
     <electronicMailAddress>john.doe@example.org</electronicMailAddress>
</creator>
<contact>
    <individualName>
        <givenName>A.</givenName>
        <surName>Smith</surName>
    </individualName>
    <electronicMailAddress>a.smith@example.com</electronicMailAddress>
</contact>
<metadataProvider>
    <individualName>
        <givenName>B.</givenName>
        <surName>Johnson</surName>
    </individualName>
    <electronicMailAddress>b.johnson@example.com</electronicMailAddress>
</metadataProvider>
    '''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when: "Contacts are extracted"
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "All unique contacts are processed correctly"
        result.contacts.size() == 3
        result.primaryContacts.size() == 1
        result.contacts*.email.containsAll(['john.doe@example.org', 'a.smith@example.com', 'b.johnson@example.com'])
        result.contacts*.lastName.containsAll(['Doe', 'Smith', 'Johnson'])
    }

    void "test extractContactsFromEml with organizationName only"() {
        given:
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
    <organizationName>Example Organization</organizationName>
</creator>
'''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when:
        def result = service.extractContactsFromEml(eml, dataResource)

        then:
        result.contacts.size() == 1
        result.contacts[0].organizationName == 'Example Organization'
    }

    void "test extractContactsFromEml with positionName only"() {
        given:
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
    <positionName>Data Manager</positionName>
</creator>
'''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when:
        def result = service.extractContactsFromEml(eml, dataResource)

        then:
        result.contacts.size() == 1
        result.contacts[0].positionName == 'Data Manager'
    }

    void "test extractContactsFromEml with individualName only"() {
        given:
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
    <individualName>
        <givenName>Jane</givenName>
        <surName>Doe</surName>
    </individualName>
</creator>
        '''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when:
        def result = service.extractContactsFromEml(eml, dataResource)

        then:
        result.contacts.size() == 1
        result.contacts[0].lastName == 'Doe'
        result.contacts[0].firstName == 'Jane'
    }

    void "test extractContactsFromEml skips invalid contact"() {
        given:
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
    <individualName>
        <givenName></givenName>
        <surName></surName>
    </individualName>
</creator>
    '''
        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when:
        def result = service.extractContactsFromEml(eml, dataResource)

        then:
        result.contacts.isEmpty()
    }

    void "test extractContactsFromEml skips contact without individualName or organizationName"() {
        given:
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
    <positionName></positionName>
</creator>
    '''
        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when:
        def result = service.extractContactsFromEml(eml, dataResource)

        then:
        result.contacts.size == 0
    }

    void "test extractContactsFromEml skips contact with no valid fields"() {
        given:
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
<creator>
    <positionName></positionName>
    <organizationName></organizationName>
</creator>
    '''
        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when:
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "The contacts is empty"
        result.contacts.size() == 0
    }

    void "test extractContactsFromEml with only giveName"() {
        given: "A base EML input loaded from a file and with a custom contact"
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
        <creator>
            <individualName>
                <surName>Doe</surName>
            </individualName>
            <organizationName>Example Organization</organizationName>
            <electronicMailAddress>john.doe@example.org</electronicMailAddress>
        </creator>
    '''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when: "Contacts are extracted"
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "The contact is created successfully"
        result.contacts.size() == 1
        result.contacts*.lastName.contains('Doe')
        result.contacts*.email.contains('john.doe@example.org')
    }

    void "test extractContactsFromEml without surName"() {
        given: "A base EML input loaded from a file and with a custom contact"
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
        <creator>
            <individualName>
                <givenName>John</givenName>
            </individualName>
        </creator>
    '''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when: "Contacts are extracted"
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "The contact is empty"
        result.contacts.size() == 0

    }

    void "test extractContactsFromEml with surName and phone"() {
        given: "A base EML input loaded from a file and with a custom contact"
        def baseEml = getClass().getResourceAsStream("/base_eml.xml").text
        def contactsXml = '''
        <creator>
            <individualName>
                <surName>Doe</surName>
            </individualName>
            <phone>+1 234567890</phone>
        </creator>
    '''

        def eml = parseEml(baseEml, contactsXml)
        def dataResource = new DataResource()

        when: "Contacts are extracted"
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "The contact is empty"
        result.contacts.size() == 1
        result.contacts*.lastName.contains('Doe')
        result.contacts*.phone.contains('+1 234567890')
    }

    void "test addOrUpdateContact updates phone if contact exists or creates new contact with phone"() {
        given: "An existing contact and an EML element with updated phone"
        def existingContact = new Contact(
                firstName: "John",
                lastName: "Doe",
                email: "john.doe@example.org",
                phone: "123456789",
                userLastModified: "originalUser"
        ).save(flush: true, failOnError: true)

        def emlElement = new XmlSlurper().parseText('''        
<creator>
    <individualName>
        <givenName>John</givenName>
        <surName>Doe</surName>
    </individualName>
    <electronicMailAddress>john.doe@example.org</electronicMailAddress>
    <phone>987654321</phone>
</creator>
''')

        when: "addOrUpdateContact is called"
        def result = service.addOrUpdateContact(emlElement)

        then: "The existing contact is updated with the new phone"
        result != null
        result.email == "john.doe@example.org"
        result.firstName == "John"
        result.lastName == "Doe"
        result.phone == "987654321"
        result.userLastModified == "testUser"

        and: "No duplicate contact is created"
        Contact.count() == 1

        when: "A new contact is created with a phone number"
        def newEmlElement = new XmlSlurper().parseText('''      
        <creator>
            <individualName>
                <givenName>Jane</givenName>
                <surName>Smith</surName>
            </individualName>
            <electronicMailAddress>jane.smith@example.org</electronicMailAddress>
            <phone>555123456</phone>
        </creator>
    ''')

        def newContact = service.addOrUpdateContact(newEmlElement)

        then: "The new contact is created successfully"
        newContact != null
        newContact.email == "jane.smith@example.org"
        newContact.firstName == "Jane"
        newContact.lastName == "Smith"
        newContact.phone == "555123456"
        newContact.userLastModified == "testUser"

        and: "There are now two contacts in the system"
        Contact.count() == 2
    }

    void "test process userIds from EML"() {
        given: "An EML input with creators and userIds"
        def emlXml = '''
        <eml:eml xmlns:eml="https://eml.ecoinformatics.org/eml-2.2.0" xmlns:dc="http://purl.org/dc/terms/">
            <dataset>
                <creator>
                    <individualName>
                        <givenName>John</givenName>
                        <surName>Doe</surName>
                    </individualName>
                    <organizationName>Sample Organization</organizationName>
                    <userId directory="https://orcid.org/">0000-0001-2345-6789</userId>
                </creator>
                <creator>
                    <individualName>
                        <givenName>Jane</givenName>
                        <surName>Smith</surName>
                    </individualName>
                    <organizationName>Another Organization</organizationName>
                    <userId directory="https://orcid.org/">0000-0002-9876-5432</userId>
                </creator>
            </dataset>
        </eml:eml>
    '''

        def eml = new XmlSlurper().parseText(emlXml)
        def dataResource = new DataResource()

        when: "Contacts are extracted from EML"
        def result = service.extractContactsFromEml(eml, dataResource)

        then: "Contacts are created with correct userIdUrl and organizationName"
        result.contacts.size() == 2

        and: "First contact contains correct userIdUrl and organizationName"
        result.contacts[0].userId == "https://orcid.org/0000-0001-2345-6789"
        result.contacts[0].organizationName == "Sample Organization"

        and: "Second contact contains correct userIdUrl and organizationName"
        result.contacts[1].userId == "https://orcid.org/0000-0002-9876-5432"
        result.contacts[1].organizationName == "Another Organization"
    }

    void "test addOrUpdateContact allows same email with different names"() {
        given: "An existing contact with a specific name and email"
        def existingContact = new Contact(
                firstName: "Alice",
                lastName: "Smith",
                email: "shared@example.org",
                userLastModified: "originalUser"
        ).save(flush: true, failOnError: true)

        def emlElement = new XmlSlurper().parseText('''        
    <creator>
        <individualName>
            <givenName>Bob</givenName>
            <surName>Johnson</surName>
        </individualName>
        <electronicMailAddress>shared@example.org</electronicMailAddress>
    </creator>
''')

        when: "addOrUpdateContact is called with a different name but the same email"
        def result = service.addOrUpdateContact(emlElement)

        then: "A new contact is created instead of overwriting the existing one"
        result != null
        result.email == "shared@example.org"
        result.firstName == "Bob"
        result.lastName == "Johnson"
        result.userLastModified == "testUser"

        and: "Both contacts exist in the database"
        Contact.count() == 2

        and: "The original contact remains unchanged"
        def originalContact = Contact.findByFirstNameAndLastName("Alice", "Smith")
        originalContact.email == "shared@example.org"
    }

    void "test addOrUpdateContact does not overwrite contact with only orgName"() {
        given: "A contact with both a name and organizationName exists"
        def existingContactEml = new XmlSlurper().parseText('''        
    <creator>
        <individualName>
            <givenName>John</givenName>
            <surName>Doe</surName>
        </individualName>
        <organizationName>Acme Corp</organizationName>
    </creator>
    ''')

        def newContactEml = new XmlSlurper().parseText('''        
    <creator>
        <organizationName>Acme Corp</organizationName>
    </creator>
    ''')

        when: "The first contact is added"
        def contactWithName = service.addOrUpdateContact(existingContactEml)

        and: "A new contact with only the organization is added"
        def contactWithOnlyOrg = service.addOrUpdateContact(newContactEml)

        then: "Both contacts exist separately"
        contactWithName != null
        contactWithOnlyOrg != null
        contactWithName.organizationName == "Acme Corp"
        contactWithOnlyOrg.organizationName == "Acme Corp"

        and: "They are stored as separate contacts"
        Contact.count() == 2

        and: "The original contact's name is not removed"
        Contact.findByFirstNameAndLastName("John", "Doe") != null
    }

    void "test addOrUpdateContact does not overwrite contact with only positionName"() {
        given: "A contact with both a name and positionName exists"
        def existingContactEml = new XmlSlurper().parseText('''        
    <creator>
        <individualName>
            <givenName>Jane</givenName>
            <surName>Smith</surName>
        </individualName>
        <positionName>Data Manager</positionName>
    </creator>
    ''')

        def newContactEml = new XmlSlurper().parseText('''        
    <creator>
        <positionName>Data Manager</positionName>
    </creator>
    ''')

        when: "The first contact is added"
        def contactWithName = service.addOrUpdateContact(existingContactEml)

        and: "A new contact with only the position is added"
        def contactWithOnlyPosition = service.addOrUpdateContact(newContactEml)

        then: "Both contacts exist separately"
        contactWithName != null
        contactWithOnlyPosition != null
        contactWithName.positionName == "Data Manager"
        contactWithOnlyPosition.positionName == "Data Manager"

        and: "They are stored as separate contacts"
        Contact.count() == 2

        and: "The original contact's name is not removed"
        Contact.findByFirstNameAndLastName("Jane", "Smith") != null
    }

    void "test addOrUpdateContact updates positionName and organizationName case sensitivity"() {
        given:
        def existingContact = new Contact(
                firstName: "John",
                lastName: "Doe",
                positionName: "data manager",
                organizationName: "example org",
                email: "john.doe@example.com",
                userLastModified: "originalUser"
        ).save(flush: true, failOnError: true)

        def emlElement = new XmlSlurper().parseText('''
        <creator>
            <individualName>
                <givenName>John</givenName>
                <surName>Doe</surName>
            </individualName>
            <positionName>Data Manager</positionName>
            <organizationName>Example Org</organizationName>
            <electronicMailAddress>john.doe@example.com</electronicMailAddress>
        </creator>
    ''')

        when:
        def updatedContact = service.addOrUpdateContact(emlElement)

        then:
        updatedContact != null
        updatedContact.id == existingContact.id
        updatedContact.positionName == "Data Manager"
        updatedContact.organizationName == "Example Org"
    }


}
