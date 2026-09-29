package au.org.ala.collectory

import grails.testing.gorm.DataTest
import spock.lang.PendingFeature
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Boundary tests for every String property that declares a maxSize.
 *
 * Widths are read from the domain constraints themselves, so a new or changed maxSize
 * is covered automatically. Each property is exercised with ASCII, 2-byte and 3-byte
 * UTF-8 characters at exactly the limit and one character over it.
 *
 * Note: these tests check GORM validation only. Whether the database column is wide
 * enough (and uses a charset that can store the characters) is checked by the MySQL
 * integration tests.
 */
class DomainConstraintsSpec extends Specification implements DataTest {

    static final List<Class> DOMAINS = [
            Collection, Institution, DataProvider, DataResource, DataHub,
            Contact, ContactFor, TempDataResource, Attribution, ProviderCode
    ]

    // one character of each UTF-8 width that fits in a single UTF-16 unit
    static final Map<String, String> CHARS = [
            ascii    : 'a',
            twoBytes : 'ñ',
            threeByte: '漢'
    ]

    @Shared
    List<List> maxSizeRows

    def setupSpec() {
        mockDomains(*DOMAINS)
        maxSizeRows = []
        DOMAINS.each { Class domain ->
            constrainedProperties(domain).each { String name, cp ->
                if (cp.propertyType == String && cp.maxSize != null && !isSpecialCase(domain, name)) {
                    maxSizeRows << [domain, name, cp.maxSize as int]
                }
            }
        }
    }

    private static Map constrainedProperties(Class domain) {
        domain.constrainedProperties
    }

    /** Properties with extra validators whose valid values cannot be arbitrary text. */
    private static boolean isSpecialCase(Class domain, String name) {
        (name == 'websiteUrl') ||
                (domain == Collection && name in ['kingdomCoverage']) ||
                (domain == Contact && name == 'email') ||
                (domain == TempDataResource && name in ['license', 'status'])
    }

    private static String fill(String ch, int n) {
        ch * n
    }

    private static boolean fieldValid(Class domain, String field, value) {
        def instance = domain.newInstance()
        instance."$field" = value
        instance.validate([field])
        !instance.errors.hasFieldErrors(field)
    }

    private static String fieldErrorCode(Class domain, String field, value) {
        def instance = domain.newInstance()
        instance."$field" = value
        instance.validate([field])
        instance.errors.getFieldError(field)?.code
    }

    def "every domain exposes at least one String maxSize constraint"() {
        expect:
        maxSizeRows.size() > 40
        DOMAINS.findAll { d -> !maxSizeRows.any { it[0] == d } } == []
    }

    @Unroll
    def "#domain.simpleName.#field accepts exactly #max #charName characters"() {
        expect:
        fieldValid(domain, field, fill(ch, max))

        where:
        [domain, field, max, charName, ch] << maxSizeRowsWithChars()
    }

    @Unroll
    def "#domain.simpleName.#field rejects #max + 1 #charName characters with maxSize.exceeded"() {
        expect:
        fieldErrorCode(domain, field, fill(ch, max + 1)) == 'maxSize.exceeded'

        where:
        [domain, field, max, charName, ch] << maxSizeRowsWithChars()
    }

    /**
     * Characterisation: GORM counts UTF-16 code units, so a 4-byte character (e.g. CJK Extension B
     * characters used in some Japanese and Chinese names, or mathematical letters pasted from documents) uses two units and only max/2 of them fit, while MySQL counts code points.
     */
    @Unroll
    def "#domain.simpleName.#field counts supplementary characters as two units"() {
        given:
        String supplementary = '𠮷'  // U+20BB7, as in the surname 𠮷田
        int fits = max.intdiv(2)

        expect:
        supplementary.length() == 2
        fieldValid(domain, field, supplementary * fits)
        max < 2 || fieldErrorCode(domain, field, supplementary * fits + 'ab') == 'maxSize.exceeded'

        where:
        [domain, field, max] << rows()
    }

    static List<List> rows() {
        DOMAINS.collectMany { Class domain ->
            domain.constrainedProperties.findAll { name, cp ->
                cp.propertyType == String && cp.maxSize != null && !isSpecialCase(domain, name)
            }.collect { name, cp -> [domain, name, cp.maxSize as int] }
        }
    }

    static List<List> maxSizeRowsWithChars() {
        rows().collectMany { row -> CHARS.collect { charName, ch -> row + [charName, ch] } }
    }

    @Unroll
    def "#domain.simpleName requires a non blank uid and name"() {
        expect:
        fieldErrorCode(domain, 'uid', '') == 'blank'
        fieldErrorCode(domain, 'name', '') == 'blank'
        fieldErrorCode(domain, 'uid', null) == 'nullable'
        fieldErrorCode(domain, 'name', null) == 'nullable'

        where:
        domain << [Collection, Institution, DataProvider, DataResource, DataHub]
    }

    @Unroll
    def "#315/#306 #domain.simpleName websiteUrl '#url' valid=#valid"() {
        expect:
        fieldValid(domain, 'websiteUrl', url) == valid

        where:
        [domain, url, valid] << [[Collection, Institution, DataProvider, DataResource, DataHub],
                                 [[null, true], ['', true], ['http://example.org', true],
                                  ['https://ñandú.example.org/ruta?q=漢', true],
                                  ['www.vifm.org', false], ['ftp://example.org', false],
                                  ['javascript:alert(1)', false]]]
                .combinations().collect { d, pair -> [d] + pair }
    }

    @Unroll
    def "#315 #domain.simpleName pubShortDescription over 100 chars is a validation error, not a DB error"() {
        expect:
        fieldErrorCode(domain, 'pubShortDescription', 'x' * 101) == 'maxSize.exceeded'
        fieldValid(domain, 'pubShortDescription', 'x' * 100)

        where:
        domain << [Collection, Institution, DataProvider, DataResource, DataHub]
    }

    @Unroll
    def "Contact.email '#email' valid=#valid"() {
        expect:
        fieldValid(Contact, 'email', email) == valid

        where:
        email                              | valid
        null                               | true
        'someone@example.org'              | true
        'first.last+tag@sub.example.org'   | true
        'not-an-email'                     | false
        'a@b'                              | false
        'x' * 117 + '@example.org'         | false  // 129 chars > maxSize 128
    }

    @Unroll
    def "Collection.kingdomCoverage '#value' valid=#valid"() {
        expect:
        fieldValid(Collection, 'kingdomCoverage', value) == valid

        where:
        value                    | valid
        null                     | true
        'Plantae'                | true
        'Animalia Plantae Fungi' | true
        'Plantae Martians'       | false
        'plantae'                | false
    }

    @Unroll
    def "Collection.collectionType accepts known types: #value"() {
        expect:
        fieldValid(Collection, 'collectionType', value)

        where:
        value << [null, '', '["preserved"]', '["living","tissue","seedbank"]']
    }

    @Unroll
    def "Collection coordinate #field must be within +-360"() {
        expect:
        fieldValid(Collection, field, 360.0)
        fieldValid(Collection, field, -360.0)
        !fieldValid(Collection, field, 360.1)
        !fieldValid(Collection, field, -360.1)

        where:
        field << ['eastCoordinate', 'westCoordinate', 'northCoordinate', 'southCoordinate']
    }

    @Unroll
    def "TempDataResource.#field only accepts listed values"() {
        given:
        def allowed = TempDataResource.constrainedProperties[field].inList

        expect:
        allowed
        allowed.every { fieldValid(TempDataResource, field, it) }
        !fieldValid(TempDataResource, field, 'definitely-not-a-valid-value')

        where:
        field << ['license', 'status']
    }

    def "DataProvider.gbifCountryToAttribute is a 3 letter code that cannot be null"() {
        expect:
        fieldValid(DataProvider, 'gbifCountryToAttribute', 'AUS')
        fieldErrorCode(DataProvider, 'gbifCountryToAttribute', 'AUST') == 'maxSize.exceeded'
        fieldErrorCode(DataProvider, 'gbifCountryToAttribute', null) == 'nullable'
    }

    @Unroll
    def "Contact stores '#text' unchanged in notes"() {
        given:
        def contact = new Contact(notes: text)

        expect:
        contact.validate(['notes'])
        contact.notes == text

        where:
        text << ['Ñoño – “quoted” ’apostrophe’', '漢字かなカナ', 'مرحبا بالعالم', 'é (NFD) vs é (NFC)',
                 'narrow no-break', '<script>alert(1)</script>', 'tab\tnew\nline', '4-byte 𠮷田 𝔸']
    }
}
