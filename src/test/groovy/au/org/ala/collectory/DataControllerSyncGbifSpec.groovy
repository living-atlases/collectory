package au.org.ala.collectory

import grails.async.Promises
import grails.converters.JSON
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

/**
 * GET /ws/syncGBIF in DataController.
 */
class DataControllerSyncGbifSpec extends Specification implements ControllerUnitTest<DataController> {

    def asyncService = Mock(AsyncServiceContract)

    interface AsyncServiceContract {
        def updateAllResources()
        def updateAllRegistrations()
    }

    def setup() {
        controller.asyncGbifRegistryService = asyncService
    }

    def "#200 /ws/syncGBIF syncs all data resources, not only those of the data providers"() {
        when:
        controller.syncGBIF()

        then:
        1 * asyncService.updateAllResources() >> Promises.createBoundPromise([:])
        0 * asyncService.updateAllRegistrations()
        JSON.parse(response.text).status == 'STARTED'
    }
}
