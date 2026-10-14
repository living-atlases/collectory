package au.org.ala.collectory

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

class MetadataServiceSpec extends Specification implements ServiceUnitTest<MetadataService> {

    def setup() {
        config.uploadFilePath = '/data/collectory/upload/'
    }

    @Unroll
    def "#312 the url of an uploaded file with uploadExternalUrlPath #external and serverURL #serverURL"() {
        given:
        config.uploadExternalUrlPath = external
        config.grails.serverURL = serverURL

        expect:
        service.convertPath('file:////data/collectory/upload/dr1/occurrences.zip') == expected

        where: 'application.yml has an absolute url, ala-install a path'
        external                  | serverURL                                        || expected
        'http://localhost/upload' | 'https://collections.ala.org.au'                 || 'https://collections.ala.org.au/upload/dr1/occurrences.zip'
        'http://localhost/upload' | 'https://collections.test.ala.org.au/collectory' || 'https://collections.test.ala.org.au/collectory/upload/dr1/occurrences.zip'
        '/upload/'                | 'https://collections.l-a.site'                   || 'https://collections.l-a.site/upload/dr1/occurrences.zip'
    }

    def "#312 local upload paths in a resource's connection parameters are shown as urls"() {
        given:
        config.uploadExternalUrlPath = '/upload/'
        config.grails.serverURL = 'https://collections.l-a.site'

        expect:
        service.convertAnyLocalPaths('{"protocol":"DwCA","url":"file:////data/collectory/upload/dr1/occurrences.zip"}') ==
                '{"protocol":"DwCA","url":"https://collections.l-a.site/upload/dr1/occurrences.zip"}'
    }
}
