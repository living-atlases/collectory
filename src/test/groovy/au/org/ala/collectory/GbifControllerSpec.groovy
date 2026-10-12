package au.org.ala.collectory

import grails.async.Promises
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class GbifControllerSpec extends Specification implements ControllerUnitTest<GbifController> {

    def asyncService = Mock(AsyncServiceContract)

    interface AsyncServiceContract {
        def updateAllResources()
    }

    def setup() {
        config.gbifRegistrationRole = 'ROLE_ADMIN'
        config.ROLE_ADMIN = 'ROLE_ADMIN'
        def auth = new CollectoryAuthService()
        auth.grailsApplication = grailsApplication
        controller.collectoryAuthService = auth
        controller.asyncGbifRegistryService = asyncService
    }

    def "syncAllResources starts the GBIF sync for a user with the GBIF registration role"() {
        given:
        request.addUserRole('ROLE_ADMIN')

        when:
        def model = controller.syncAllResources()

        then:
        1 * asyncService.updateAllResources() >> Promises.createBoundPromise([:])
        model.errorMessage == ''
    }

    def "syncAllResources reports insufficient privileges for other users"() {
        given:
        request.addUserRole('ROLE_EDITOR')

        when:
        def model = controller.syncAllResources()

        then:
        0 * asyncService.updateAllResources()
        model.errorMessage
    }
}
