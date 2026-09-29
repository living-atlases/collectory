package au.org.ala.collectory

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import spock.lang.Unroll

/**
 * GET /ws/{entity}/{uid} in DataController.
 */
class DataControllerEntitySpec extends Specification implements ControllerUnitTest<DataController>, DataTest {

    DataResource dr

    def setupSpec() {
        mockDomains(DataResource, TempDataResource)
    }

    def setup() {
        config.ROLE_ADMIN = 'ROLE_ADMIN'
        config.REQUIRED_SCOPES = 'ala/internal'
        dr = new DataResource(uid: 'dr1', name: 'One', userLastModified: 't').save(validate: false, flush: true)
        controller.providerGroupService = Stub(ProviderGroupService) {
            _getEager('dr1') >> dr
            _getEager(_) >> null
            _get(_, _) >> null
        }
        def auth = new CollectoryAuthService()
        auth.grailsApplication = grailsApplication
        controller.collectoryAuthService = auth
        controller.metadataService = Stub(MetadataService) { convertAnyLocalPaths(_) >> { args -> args[0] } }
        controller.crudService = Mock(CrudService)
        params.entity = 'dataResource'
        params.uid = 'dr1'
    }

    @Unroll
    def "#277 #who gets the connection url: #expected"() {
        given:
        granted.each { request.addUserRole(it) }

        when:
        controller.getEntity()

        then:
        1 * controller.crudService.readDataResource(dr, expected) >> '{"uid":"dr1"}'
        response.status == 200

        where:
        who                                        | granted            | expected
        'an ALA admin'                             | ['ROLE_ADMIN']     | true
        'an M2M client with the ala/internal scope'| ['ala/internal']   | true
        'an editor'                                | ['ROLE_EDITOR']    | false
        'an anonymous caller'                      | []                 | false
    }

    def "an unknown uid is a 404"() {
        given:
        params.uid = 'dr404'

        when:
        controller.getEntity()

        then:
        0 * controller.crudService._
        response.status == 404
    }
}
