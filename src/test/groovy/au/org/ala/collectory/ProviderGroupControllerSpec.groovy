package au.org.ala.collectory

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Shared editing logic of ProviderGroupController, exercised through DataResourceController.
 */
class ProviderGroupControllerSpec extends Specification implements ControllerUnitTest<DataResourceController> {

    @Unroll
    def "#188/#254 parseCoordinate('#input') == #expected"() {
        expect:
        controller.parseCoordinate(input) == expected

        where:
        input        | expected
        '-35.28'     | -35.28d
        '-35,28'     | -35.28d
        '149,1234567'| 149.1234567d
        ' 12.5 '     | 12.5d
        '0'          | 0d
        null         | -1d
        ''           | -1d
        '   '        | -1d
        'abc'        | -1d
    }
}
