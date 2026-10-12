package au.org.ala.collectory

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification
import spock.lang.Unroll

class ContactSpec extends Specification implements DomainUnitTest<Contact> {

    @Unroll
    def "parseName('#input') -> title '#title', first '#first', last '#last'"() {
        given:
        def c = new Contact()

        when:
        c.parseName(input)

        then:
        (c.title ?: null) == (title ?: null)
        c.firstName == first
        c.lastName == last

        where:
        input                           | title | first         | last
        null                            | null  | null          | null
        ''                              | null  | null          | null
        'Weir'                          | null  | null          | 'Weir'
        'Tom Weir'                      | null  | 'Tom'         | 'Weir'
        'Dr Tom Weir'                   | 'Dr'  | 'Tom'         | 'Weir'
        'Prof Tom A Weir'               | 'Prof'| 'Tom A'       | 'Weir'
        'Tom A Weir'                    | ''    | 'Tom A'       | 'Weir'
        'Mr Tom Weir (BSc (HONS))'      | 'Mr'  | 'Tom'         | 'Weir'
        'José Ñúñez'                    | null  | 'José'        | 'Ñúñez'
        'María José García Márquez'     | ''    | 'María José García' | 'Márquez'
        '王 小明'                        | null  | '王'          | '小明'
    }

    @Unroll
    def "buildName falls back through the available fields: #props -> '#expected'"() {
        expect:
        new Contact(props).buildName() == expected

        where:
        props                                                   | expected
        [title: 'Dr', firstName: 'Tom', lastName: 'Weir']       | 'Dr Tom Weir'
        [firstName: 'Tom', lastName: 'Weir']                    | 'Tom Weir'
        [lastName: 'Weir']                                      | 'Weir'
        [organizationName: 'CSIRO', email: 'x@example.org']     | 'CSIRO'
        [positionName: 'Collection manager']                    | 'Collection manager'
        [email: 'x@example.org', phone: '123']                  | 'x@example.org'
        [userId: '42', phone: '123']                            | '42'
        [phone: '+61 2 1234 5678']                              | '+61 2 1234 5678'
        [mobile: '0400 000 000']                                | '0400 000 000'
        [fax: '02 000']                                         | '02 000'
        [:]                                                     | ''
        [firstName: 'Ñandú', lastName: '漢字']                  | 'Ñandú 漢字'
    }

    def "toString is buildName"() {
        expect:
        new Contact(firstName: 'A', lastName: 'B').toString() == 'A B'
    }

    @Unroll
    def "hasContent is #expected for #props"() {
        expect:
        new Contact(props).hasContent() == expected

        where:
        props                        | expected
        [:]                          | false
        [title: 'Dr']                | false
        [firstName: 'Tom']           | false
        [lastName: 'Weir']           | true
        [email: 'x@example.org']     | true
        [positionName: 'Manager']    | true
    }

    def "publish defaults to true"() {
        expect:
        new Contact().publish
    }
}
