package au.org.ala.collectory

import grails.testing.gorm.DataTest
import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class ContactsTemplateSpec extends Specification implements TagLibUnitTest<CollectoryTagLib>, DataTest {

    def setupSpec() {
        mockDomains(Contact, ContactFor)
    }

    def "template renders nothing when contact list has only unpublished contacts"() {
        given:
        def c = new Contact(lastName: 'Hidden', publish: false, userLastModified: 't').save(validate: false, flush: true)
        def cf = new ContactFor(contact: c, entityUid: 'dr1', userLastModified: 't').save(validate: false, flush: true)

        when:
        def output = applyTemplate('<g:render template="/public/contacts" bean="${contacts}"/>', [contacts: [cf]])

        then:
        !output.contains('Contact')
        !output.contains('Hidden')
        output.trim() == ''
    }

    def "template renders contact details when published"() {
        given:
        def c = new Contact(lastName: 'Visible', publish: true, userLastModified: 't').save(validate: false, flush: true)
        def cf = new ContactFor(contact: c, entityUid: 'dr1', userLastModified: 't').save(validate: false, flush: true)

        when:
        def output = applyTemplate('<g:render template="/public/contacts" bean="${contacts}"/>', [contacts: [cf]])

        then:
        output.contains('Contact')
        output.contains('Visible')
    }
}
