package au.org.ala.collectory

import grails.converters.JSON
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.PendingFeature
import spock.lang.Specification

/**
 * Contact web services: /ws/{entity}/{uid}/contacts and /ws/{entity}/{uid}/contacts/{id}.
 */
class DataControllerContactsSpec extends Specification implements ControllerUnitTest<DataController>, DataTest {

    DataResource dr1
    DataResource dr2

    def setupSpec() {
        mockDomains(DataResource, DataProvider, Contact, ContactFor, TempDataResource)
    }

    def setup() {
        config.grails.serverURL = 'http://collectory.test'
        dr1 = new DataResource(uid: 'dr1', name: 'One', userLastModified: 't').save(validate: false, flush: true)
        dr2 = new DataResource(uid: 'dr2', name: 'Two', userLastModified: 't').save(validate: false, flush: true)
        controller.providerGroupService = Stub(ProviderGroupService) {
            _getEager('dr1') >> dr1
            _getEager('dr2') >> dr2
            _getEager(_) >> null
            _get(_, _) >> null
        }
        controller.collectoryAuthService = Stub(CollectoryAuthService)
        response.format = 'json'
    }

    private Contact contact(Map props) {
        new Contact([userLastModified: 't'] + props).save(validate: false, flush: true)
    }

    private ContactFor link(Contact c, DataResource dr, Map props = [:]) {
        new ContactFor([contact: c, entityUid: dr.uid, userLastModified: 't'] + props).save(validate: false, flush: true)
    }

    def "#325 contact for a data resource that does not exist is a 404, not a server error"() {
        given:
        params.entity = 'dataResource'
        params.uid = 'dr404'
        params.id = '1'

        when:
        controller.contactForEntity()

        then:
        response.status == 404
        response.text.contains('no entity with uid = dr404')
    }

    def "#325 contacts for a data resource that does not exist is a 404"() {
        given:
        params.entity = 'dataResource'
        params.uid = 'dr404'

        when:
        controller.contactsForEntity()

        then:
        response.status == 404
    }

    def "entity of the wrong type is a 404"() {
        given:
        params.entity = 'dataProvider'
        params.uid = 'dr1'

        when:
        controller.contactsForEntity()

        then:
        response.status == 404
        response.text.contains('is not a dataProvider')
    }

    def "#262 contacts list only includes published contacts"() {
        given:
        link(contact(lastName: 'Público', email: 'pub@example.org', publish: true), dr1, [primaryContact: true])
        link(contact(lastName: 'Privado', email: 'priv@example.org', phone: '+34 600 000 000', publish: false), dr1)
        params.entity = 'dataResource'
        params.uid = 'dr1'

        when:
        controller.contactsForEntity()
        def json = JSON.parse(response.text)

        then:
        response.status == 200
        json*.contact*.lastName == ['Público']
        !response.text.contains('priv@example.org')
    }

    def "a single published contact is returned with a uri for the requested entity"() {
        given:
        def c = contact(firstName: 'Ñandú', lastName: '漢字', email: 'n@example.org')
        def cf = link(c, dr1, [role: 'Curator'])
        params.entity = 'dataResource'
        params.uid = 'dr1'
        params.id = c.id.toString()

        when:
        controller.contactForEntity()
        def json = JSON.parse(response.text)

        then:
        json.contact.firstName == 'Ñandú'
        json.contact.lastName == '漢字'
        json.role == 'Curator'
        json.uri == "http://collectory.test/ws/dataResource/dr1/contacts/${cf.id}"
    }

    @PendingFeature(reason = '#54 contactForEntity looks up ContactFor by contact only, so for a contact linked to several entities it may describe another entity')
    def "#54 contact linked to several entities is described for the requested entity"() {
        given:
        def c = contact(lastName: 'Shared', email: 's@example.org')
        link(c, dr2, [role: 'Other entity'])
        def cf1 = link(c, dr1, [role: 'This entity'])
        params.entity = 'dataResource'
        params.uid = 'dr1'
        params.id = c.id.toString()

        when:
        controller.contactForEntity()
        def json = JSON.parse(response.text)

        then:
        json.role == 'This entity'
        json.uri.endsWith("/ws/dataResource/dr1/contacts/${cf1.id}")
    }

    def "contacts can be rendered as CSV with unicode intact"() {
        given:
        def c = contact(firstName: 'José', lastName: 'Ñúñez', email: 'j@example.org')
        link(c, dr1, [role: 'Manager'])
        params.entity = 'dataResource'
        params.uid = 'dr1'
        params.id = c.id.toString()
        response.format = 'csv'

        when:
        controller.contactForEntity()

        then:
        response.contentType.startsWith('text/csv')
        response.text.contains('"José","Ñúñez","Manager"')
    }

    @PendingFeature(reason = 'DELETE /ws/{entity}/{uid} only deletes the entity; the admin UI delete (ProviderGroupController.delete) removes the ContactFor links first')
    def "deleting an entity through the web service removes its contact links, like the admin UI does"() {
        given:
        link(contact(lastName: 'Admin', email: 'a@example.org'), dr1, [administrator: true])
        controller.providerGroupService = Stub(ProviderGroupService) { _get('dr1') >> dr1 }
        params.uid = 'dr1'
        request.method = 'DELETE'

        when:
        controller.delete()

        then:
        DataResource.findByUid('dr1') == null
        ContactFor.findAllByEntityUid('dr1') == []
    }
}
