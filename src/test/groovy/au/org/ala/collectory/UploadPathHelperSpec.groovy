package au.org.ala.collectory

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests for the upload path construction introduced in #313 (DR uid in upload path)
 * and the filename sanitisation used for multipart uploads.
 */
class UploadPathHelperSpec extends Specification {

    static final String SEP = File.separator

    @Unroll
    def "sanitizeFilename strips path segments: '#input' -> '#expected'"() {
        expect:
        UploadPathHelper.sanitizeFilename(input) == expected

        where:
        input                          | expected
        null                           | null
        ''                             | null
        'occurrences.csv'              | 'occurrences.csv'
        '../../etc/passwd'             | 'passwd'
        '/abs/path/data.zip'           | 'data.zip'
        'C:\\Users\\me\\dwca.zip'      | 'dwca.zip'
        'dir/sub\\mixed.txt'           | 'mixed.txt'
        'ñandú_datos_漢字.csv'          | 'ñandú_datos_漢字.csv'
        'Screenshot 10.15.22\u202FAM.png' | 'Screenshot 10.15.22\u202FAM.png'
        'with space.csv'               | 'with space.csv'
        '.hidden'                      | '.hidden'
    }

    @Unroll
    def "sanitizeFilename rejects remaining '..' sequences: '#input'"() {
        when:
        UploadPathHelper.sanitizeFilename(input)

        then:
        thrown(IllegalArgumentException)

        where:
        input << ['..', '...', 'a/..', 'file..csv']
    }

    def "#313 upload directory includes the resource uid and file id"() {
        expect:
        UploadPathHelper.getUploadDirectory('/data/upload', 'dr123', '1700000000') ==
                "/data/upload${SEP}dr123${SEP}1700000000${SEP}"
        UploadPathHelper.getUploadDirectory('/data/upload/', 'dr123', '1700000000') ==
                "/data/upload/dr123${SEP}1700000000${SEP}"
    }

    def "#313 upload file path never escapes the upload directory"() {
        when:
        def path = UploadPathHelper.getUploadFilePath('/data/upload/', 'dr1', '42', '../../../etc/passwd')

        then:
        path == "/data/upload/dr1${SEP}42${SEP}passwd"
        new File(path).canonicalPath.startsWith(new File('/data/upload').canonicalPath + SEP)
    }

    def "temp paths are nested under uid/tmp"() {
        expect:
        UploadPathHelper.getTempDirectory('/data/upload/', 'dr1', 'dl9') == "/data/upload/dr1${SEP}tmp${SEP}dl9${SEP}"
        UploadPathHelper.getTempFilePath('/data/upload/', 'dr1', 'dl9', 'x/y.csv') == "/data/upload/dr1${SEP}tmp${SEP}dl9${SEP}y.csv"
    }

    @Unroll
    def "external url joins with exactly one slash: '#base'"() {
        expect:
        UploadPathHelper.getExternalUrl(base, 'dr1', '42', 'file.zip') == 'http://example.org/upload/dr1/42/file.zip'

        where:
        base << ['http://example.org/upload', 'http://example.org/upload/']
    }

    def "external url keeps unicode filenames untouched"() {
        expect:
        UploadPathHelper.getExternalUrl('http://example.org/upload', 'dr1', '42', 'año 漢.zip') ==
                'http://example.org/upload/dr1/42/año 漢.zip'
    }

    def "combined directory is uid/fileId"() {
        expect:
        UploadPathHelper.getCombinedDirectory('dr1', '42') == 'dr1/42'
    }

    def "extractUid reads the uid of any resource and tolerates null"() {
        expect:
        UploadPathHelper.extractUid(null) == null
        UploadPathHelper.extractUid([uid: 'dp7']) == 'dp7'
    }

    @Unroll
    def "#method rejects missing arguments #args"() {
        when:
        UploadPathHelper."$method"(*args)

        then:
        thrown(IllegalArgumentException)

        where:
        method               | args
        'getUploadDirectory' | [null, 'dr1', '1']
        'getUploadDirectory' | ['/base', '', '1']
        'getUploadDirectory' | ['/base', 'dr1', null]
        'getUploadFilePath'  | ['/base', 'dr1', '1', null]
        'getTempDirectory'   | ['/base', null, 't']
        'getTempFilePath'    | ['/base', 'dr1', 't', '']
        'getExternalUrl'     | ['', 'dr1', '1', 'f']
        'getExternalUrl'     | ['http://x', 'dr1', '1', null]
        'getCombinedDirectory' | ['dr1', '']
    }
}
