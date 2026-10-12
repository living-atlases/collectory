package au.org.ala.collectory

import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification
import spock.lang.Unroll

/**
 * cl:formattedText renders descriptions (entered by editors or synced from IPT/EML) with a
 * small wiki-like markup.
 */
class CollectoryTagLibFormattedTextSpec extends Specification implements TagLibUnitTest<CollectoryTagLib> {

    private String fmt(String text, Map attrs = [:]) {
        tagLib.formattedText([body: text] + attrs).toString()
    }

    def "empty text renders nothing"() {
        expect:
        fmt(null) == ''
        fmt('') == ''
    }

    def "plain paragraphs are wrapped in <p>"() {
        expect:
        fmt('First paragraph\nSecond paragraph') == '<p>First paragraph</p><p>Second paragraph</p>'
        fmt('Text', [pClass: 'lead']) == "<p class='lead'>Text</p>"
    }

    def "an @ in plain text is not altered"() {
        expect:
        fmt('Contact curator@example.org for access') == '<p>Contact curator@example.org for access</p>'
    }

    @Unroll
    def "wiki markup: '#input' -> '#expected'"() {
        expect:
        fmt(input, [noList: true]) == expected

        where:
        input                          | expected
        'the _Eucalyptus_ genus'       | 'the <em>Eucalyptus</em> genus'
        'a +bold+ word'                | 'a <b>bold</b> word'
        'ñandú _cañón_ +año+'          | 'ñandú <em>cañón</em> <b>año</b>'
        'snake_case_name stays'        | 'snake_case_name stays'
    }

    def "bullet lines become a list"() {
        expect:
        fmt('Intro\n*one\n*two\nOutro') == "<p>Intro</p><ul class='simple'><li>one</li><li>two</li></ul><p>Outro</p>"
        fmt('*only item') == "<ul class='simple'><li>only item</li></ul>"
    }

    def "urls become links; external links are nofollow and open in a new tab"() {
        when:
        def html = fmt('See https://example.org/page for details', [noList: true])

        then:
        html.contains("<a rel='nofollow' class='external' target='_blank' href='https://example.org/page'>https://example.org/page</a>")
    }

    def "wiki style links use the label"() {
        expect:
        fmt('Visit [https://www.ala.org.au the ALA] now', [noList: true]) ==
                "Visit <a href='https://www.ala.org.au'>the ALA</a> now"
    }

    def "noLink leaves urls as text"() {
        expect:
        fmt('https://example.org', [noLink: true, noList: true]) == 'https://example.org'
    }

    def "unicode, RTL and 4-byte characters survive"() {
        given:
        def text = '漢字 مرحبا 𠮷田 𝔸 é'

        expect:
        fmt(text) == "<p>${text}</p>"
    }
}
