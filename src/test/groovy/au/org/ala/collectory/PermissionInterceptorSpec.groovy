package au.org.ala.collectory

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import java.security.Principal

/**
 * PermissionInterceptor enforces @PermissionRequired / @SkipPermissionCheck on every action.
 * API callers get a JSON 403; browser callers are redirected with a flash message.
 */
class PermissionInterceptorSpec extends Specification implements InterceptorUnitTest<PermissionInterceptor> {

    CollectoryAuthService authService = Mock()

    def setup() {
        grailsApplication.addArtefact('Controller', DataController)
        grailsApplication.addArtefact('Controller', TempDataResourceController)
        grailsApplication.addArtefact('Controller', GbifController)
        interceptor.collectoryAuthService = authService
    }

    private void route(String controller, String action) {
        webRequest.controllerName = controller
        webRequest.actionName = action
    }

    private void signIn(String name = 'user') {
        request.userPrincipal = { name } as Principal
    }

    def "interceptor runs after TokenInterceptor and matches every request"() {
        expect:
        interceptor.order > new TokenInterceptor().order
        interceptor.doesMatch()
    }

    def "actions without @PermissionRequired are open"() {
        given:
        route('data', 'getEntity')

        when:
        def result = interceptor.before()

        then:
        result
        0 * authService._
    }

    def "@SkipPermissionCheck overrides a controller level annotation"() {
        given:
        route('tempDataResource', 'getEntity')

        when:
        def result = interceptor.before()

        then:
        result
        0 * authService._
    }

    def "anonymous API request to a protected action gets a JSON 403 without consulting roles"() {
        given:
        route('data', 'saveEntity')
        request.addHeader('Accept', 'application/json')

        when:
        def result = interceptor.before()

        then:
        !result
        response.status == 403
        response.contentType.startsWith('application/json')
        response.json.error
        0 * authService._
    }

    def "signed in user is checked against the roles and scopes of the annotation"() {
        given:
        route('data', 'saveEntity')
        request.addHeader('Accept', 'application/json')
        signIn()

        when:
        def result = interceptor.before()

        then:
        1 * authService.isAuthorised({ it as List == ['ROLE_EDITOR', 'ROLE_ADMIN'] }, { it as List == ['REQUIRED_SCOPES'] }) >> allowed
        result == allowed
        response.status == (allowed ? 200 : 403)

        where:
        allowed << [true, false]
    }

    @Unroll
    def "request is treated as API when #description"() {
        given:
        route('data', 'delete')
        setup.call(request)

        when:
        interceptor.before()

        then:
        response.status == 403
        response.redirectedUrl == null

        where:
        description             | setup
        'Accept is JSON'        | { r -> r.addHeader('Accept', 'application/json') }
        'format is json'        | { r -> r.format = 'json' }
        'format is xml'         | { r -> r.format = 'xml' }
        'the URI is under /ws/' | { r -> r.forwardURI = '/ws/dataResource/dr1' }
        'the URI is under /api/'| { r -> r.forwardURI = '/api/thing' }
        'it is an AJAX call'    | { r -> r.addHeader('X-Requested-With', 'XMLHttpRequest') }
    }

    def "browser request to a protected action is redirected to the public map with a message"() {
        given:
        route('data', 'delete')
        request.forwardURI = '/dataResource/delete/1'

        when:
        def result = interceptor.before()

        then:
        !result
        response.status == 302
        response.redirectedUrl == applicationContext.getBean('grailsLinkGenerator').link(controller: 'public', action: 'map')
        response.redirectedUrl == '/'   // UrlMappings: "/"(controller: 'public', action: 'map')
        flash.message == 'You do not have permission to access this resource.'
    }
}
