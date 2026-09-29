package au.org.ala.collectory

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests for the shared logic of the ProviderGroup trait, exercised through DataResource.
 */
class ProviderGroupSpec extends Specification implements DataTest {

    def setupSpec() {
        mockDomains(DataResource, DataProvider, Contact, ContactFor, Attribution)
    }

    private DataResource savedResource(Map props = [:]) {
        def dr = new DataResource([uid: 'dr1', name: 'Resource', userLastModified: 'test'] + props)
        dr.save(validate: false, flush: true)
        dr
    }

    private ContactFor addContact(DataResource dr, Map contactProps, boolean primary) {
        def c = new Contact([lastName: 'Person', userLastModified: 'test'] + contactProps).save(validate: false, flush: true)
        new ContactFor(contact: c, entityUid: dr.uid, primaryContact: primary, administrator: false,
                userLastModified: 'test').save(validate: false, flush: true)
    }

    @Unroll
    def "entityTypeFromUid('#uid') == '#type'"() {
        expect:
        new DataResource().entityTypeFromUid(uid) == type

        where:
        uid     | type
        null    | ''
        ''      | ''
        'in1'   | 'Institution'
        'co12'  | 'Collection'
        'dp3'   | 'DataProvider'
        'dr42'  | 'DataResource'
        'dh1'   | 'DataHub'
        'zz9'   | null
    }

    @Unroll
    def "textFormOfEntityType('#uid') == '#text'"() {
        expect:
        new DataResource().textFormOfEntityType(uid).trim() == text

        where:
        uid    | text
        'dr1'  | 'data resource'
        'co1'  | 'collection'
        'dh1'  | 'data hub'
    }

    @Unroll
    def "trimLength('#input', #len) == '#expected'"() {
        expect:
        new DataResource().trimLength(input, len) == expected

        where:
        input                 | len  | expected
        null                  | 10   | null
        'short'               | 10   | 'short'
        'exactly10!'          | 10   | 'exactly10!'
        'abcdefghijklmnop'    | 10   | 'abcdefg...'
        'one two three four'  | 10   | 'one...'
        'anything'            | null | 'anything'
        'anything'            | 0    | 'anything'
        'ñandú ñandú ñandú'   | 12   | 'ñandú...'
    }

    def "makeAbstract prefers pubDescription, then techDescription, then focus"() {
        expect:
        new DataResource(pubDescription: 'pub', techDescription: 'tech', focus: 'focus').makeAbstract() == 'pub'
        new DataResource(techDescription: 'tech', focus: 'focus').makeAbstract() == 'tech'
        new DataResource(focus: 'focus').makeAbstract() == 'focus'
        new DataResource().makeAbstract() == ''
    }

    def "makeAbstract stops at the first newline but joins a short first line with the second"() {
        expect:
        new DataResource(pubDescription: 'A first line that is clearly longer than forty characters\nsecond').makeAbstract() ==
                'A first line that is clearly longer than forty characters'
        new DataResource(pubDescription: 'Short\nsecond line\nthird').makeAbstract() == 'Short second line'
    }

    def "makeAbstract trims long unicode descriptions to the requested length"() {
        given:
        def text = ('Descripción con acentos y 漢字 ' * 20).trim()

        when:
        def result = new DataResource(pubDescription: text).makeAbstract(50)

        then:
        result.length() <= 50
        result.endsWith('...')
        text.startsWith(result - '...')
    }

    @Unroll
    def "generatePermalink prefers uid, then an LSID guid, then acronym: #props"() {
        expect:
        new DataResource(props).generatePermalink() == expected

        where:
        props                                                | expected
        [uid: 'dr1', guid: 'urn:lsid:x', acronym: 'ABC']     | 'dr1'
        [guid: 'urn:lsid:x', acronym: 'ABC']                 | 'urn:lsid:x'
        [guid: 'not-an-lsid', acronym: 'ABC']                | 'ABC'
    }

    def "toString truncates the name to 60 characters"() {
        expect:
        new DataResource(name: 'n' * 100).toString() == 'n' * 60
        new DataResource(name: 'ñ').toString() == 'ñ'
    }

    @Unroll
    def "isMemberOf('#network') is #expected for networkMembership #membership"() {
        expect:
        new DataResource(networkMembership: membership).isMemberOf(network) == expected

        where:
        membership           | network | expected
        null                 | 'CHAH'  | false
        ''                   | 'CHAH'  | false
        '["CHAH","AMRRN"]'   | 'CHAH'  | true
        '["CHAH","AMRRN"]'   | 'CAMD'  | false
    }

    def "attributions can be added once, queried and removed"() {
        given:
        def dr = new DataResource()

        when:
        dr.addAttribution('at1')
        dr.addAttribution('at2')
        dr.addAttribution('at1')

        then:
        dr.attributions == 'at1 at2'
        dr.hasAttribution('at1')
        !dr.hasAttribution('at')

        when:
        dr.removeAttribution('at1')

        then:
        dr.attributions == 'at2'
        !dr.hasAttribution('at1')
    }

    def "getPrimaryContact returns the contact flagged as primary"() {
        given:
        def dr = savedResource()
        addContact(dr, [lastName: 'Other'], false)
        def primary = addContact(dr, [lastName: 'Primary'], true)

        expect:
        dr.primaryContact.id == primary.id
        dr.contactsPrimaryFirst[0].id == primary.id
    }

    def "#262 getPrimaryPublicContact skips unpublished contacts when there are several"() {
        given:
        def dr = savedResource()
        addContact(dr, [lastName: 'Hidden', publish: false], true)
        def visible = addContact(dr, [lastName: 'Visible', publish: true], false)

        expect:
        dr.primaryPublicContact.id == visible.id
        dr.publicContactsPrimaryFirst*.id == [visible.id]
    }

    def "isAuthorised is true only for administrator contacts of the entity"() {
        given:
        def dr = savedResource()
        def admin = new Contact(lastName: 'Admin', email: 'admin@example.org', userLastModified: 't').save(validate: false, flush: true)
        def user = new Contact(lastName: 'User', email: 'user@example.org', userLastModified: 't').save(validate: false, flush: true)
        new ContactFor(contact: admin, entityUid: dr.uid, administrator: true, userLastModified: 't').save(validate: false, flush: true)
        new ContactFor(contact: user, entityUid: dr.uid, administrator: false, userLastModified: 't').save(validate: false, flush: true)

        expect:
        dr.isAuthorised('admin@example.org')
        !dr.isAuthorised('user@example.org')
        !dr.isAuthorised('stranger@example.org')
    }
}
