package au.org.ala.collectory

import au.org.ala.web.AuthService
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.springframework.mock.web.MockHttpServletRequest
import grails.util.GrailsWebMockUtil
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Role and scope resolution used by PermissionInterceptor and by controllers that check
 * permissions directly. Roles come from the signed in user; scopes come from M2M tokens,
 * and both are exposed through request.isUserInRole().
 */
class CollectoryAuthServiceSpec extends Specification implements ServiceUnitTest<CollectoryAuthService>, DataTest {

    MockHttpServletRequest request

    def setupSpec() {
        mockDomains(Contact, ContactFor, DataResource, DataProvider)
    }

    def setup() {
        config.ROLE_ADMIN = 'ROLE_ADMIN'
        config.ROLE_EDITOR = 'ROLE_EDITOR'
        config.REQUIRED_SCOPES = 'ala/internal'
        config.gbifRegistrationRole = 'ROLE_ADMIN'
        config.MULTI_SCOPES = 'scope/a; scope/b ,scope/c'
        config.security = [oidc: [enabled: true]]
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()
        request = webRequest.currentRequest as MockHttpServletRequest
        service.authService = Stub(AuthService)
    }

    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    private void grant(String... roles) {
        roles.each { request.addUserRole(it) }
    }

    @Unroll
    def "resolveRoles(#keys) == #expected"() {
        expect:
        service.resolveRoles(keys as String[]).toList().toSet() == expected.toSet()

        where:
        keys                                  | expected
        ['ROLE_ADMIN']                        | ['ROLE_ADMIN']
        ['REQUIRED_SCOPES']                   | ['ala/internal']
        ['gbifRegistrationRole']              | ['ROLE_ADMIN']
        ['gbifRegistrationRole', 'ROLE_ADMIN']| ['ROLE_ADMIN']
        ['MULTI_SCOPES']                      | ['scope/a', 'scope/b', 'scope/c']
        ['not.a.config.key']                  | ['not.a.config.key']
        ['', null, 'ROLE_EDITOR']             | ['ROLE_EDITOR']
        []                                    | []
    }

    @Unroll
    def "isAuthorised(roles=#roles, scopes=#scopes) with granted #granted is #expected"() {
        given:
        grant(*granted)

        expect:
        service.isAuthorised(roles as String[], scopes as String[]) == expected

        where:
        roles                            | scopes                | granted                   | expected
        ['ROLE_EDITOR', 'ROLE_ADMIN']    | ['REQUIRED_SCOPES']   | []                        | false
        ['ROLE_EDITOR', 'ROLE_ADMIN']    | ['REQUIRED_SCOPES']   | ['ROLE_USER']             | false
        ['ROLE_EDITOR', 'ROLE_ADMIN']    | ['REQUIRED_SCOPES']   | ['ROLE_EDITOR']           | true
        ['ROLE_EDITOR', 'ROLE_ADMIN']    | ['REQUIRED_SCOPES']   | ['ROLE_ADMIN']            | true
        ['ROLE_EDITOR', 'ROLE_ADMIN']    | ['REQUIRED_SCOPES']   | ['ala/internal']          | true
        ['ROLE_EDITOR', 'ROLE_ADMIN']    | ['REQUIRED_SCOPES']   | ['ala/other']             | false
        ['ROLE_EDITOR', 'ROLE_ADMIN']    | ['REQUIRED_SCOPES']   | ['REQUIRED_SCOPES']       | false
        ['gbifRegistrationRole']         | []                    | ['ROLE_ADMIN']            | true
        ['gbifRegistrationRole']         | []                    | ['ROLE_EDITOR']           | false
        []                               | ['*']                 | []                        | true
        []                               | []                    | ['ROLE_ADMIN']            | false
        []                               | ['MULTI_SCOPES']      | ['scope/b']               | true
    }

    def "isAdmin uses the configured admin role"() {
        expect:
        !service.isAdmin()

        when:
        grant('ROLE_ADMIN')

        then:
        service.isAdmin()
    }

    def "userInRole with OIDC enabled requires the role or admin"() {
        expect:
        !service.userInRole('ROLE_EDITOR')

        when:
        grant('ROLE_EDITOR')

        then:
        service.userInRole('ROLE_EDITOR')
        !service.userInRole('ROLE_OTHER')

        when:
        grant('ROLE_ADMIN')

        then:
        service.userInRole('ROLE_OTHER')
    }

    def "userInRole grants every role when OIDC is disabled (characterisation: only safe for local development)"() {
        given:
        config.security.oidc.enabled = false

        expect:
        service.userInRole('ROLE_ADMIN')
    }

    def "username and email come from the ALA auth service"() {
        given:
        service.authService = Stub(AuthService) {
            getDisplayName() >> 'Ñandú Pérez'
            getEmail() >> 'nandu@example.org'
        }

        expect:
        service.username() == 'Ñandú Pérez'
        service.userEmail() == 'nandu@example.org'
    }

    def "username falls back to 'not available' when nobody is signed in"() {
        expect:
        service.username() == 'not available'
    }

    def "authorisedForUser lists the entities a contact administers, sorted by name"() {
        given:
        service.providerGroupService = Stub(ProviderGroupService) {
            _get('dr2') >> new DataResource(uid: 'dr2', name: 'Zeta')
            _get('dr1') >> new DataResource(uid: 'dr1', name: 'Alfa')
        }
        def c = new Contact(lastName: 'Admin', email: 'admin@example.org', userLastModified: 't').save(validate: false, flush: true)
        new ContactFor(contact: c, entityUid: 'dr2', administrator: true, userLastModified: 't').save(validate: false, flush: true)
        new ContactFor(contact: c, entityUid: 'dr1', administrator: true, userLastModified: 't').save(validate: false, flush: true)
        new ContactFor(contact: c, entityUid: 'dr3', administrator: false, userLastModified: 't').save(validate: false, flush: true)

        when:
        def result = service.authorisedForUser('admin@example.org')

        then:
        result.keys == ['dr1', 'dr2']
        result.sorted*.name == ['Alfa', 'Zeta']
    }

    def "authorisedForUser merges the entities of every contact sharing the email (dataset-specific contacts, #291)"() {
        given:
        service.providerGroupService = Stub(ProviderGroupService) {
            _get('dr1') >> new DataResource(uid: 'dr1', name: 'Alfa')
            _get('dr2') >> new DataResource(uid: 'dr2', name: 'Beta')
        }
        def c1 = new Contact(lastName: 'Same', email: 'same@example.org', userLastModified: 't').save(validate: false, flush: true)
        def c2 = new Contact(lastName: 'Same', email: 'same@example.org', phone: '123', userLastModified: 't').save(validate: false, flush: true)
        new ContactFor(contact: c1, entityUid: 'dr1', administrator: true, userLastModified: 't').save(validate: false, flush: true)
        new ContactFor(contact: c2, entityUid: 'dr2', administrator: true, userLastModified: 't').save(validate: false, flush: true)

        when:
        def result = service.authorisedForUser('same@example.org')

        then:
        result.keys.sort() == ['dr1', 'dr2']
        result.sorted*.name.sort() == ['Alfa', 'Beta']
    }

    def "authorisedForUser for an unknown email is empty"() {
        expect:
        service.authorisedForUser('nobody@example.org') == [sorted: [], keys: [], latestMod: null]
    }

    @PendingFeature(reason = 'DELETE /ws/{entity}/{uid} does not remove ContactFor links (the admin UI delete does), and authorisedForUser then calls pg.children() on null, breaking the user entity list in ManageController')
    def "authorisedForUser ignores admin links to entities that no longer exist"() {
        given:
        service.providerGroupService = Stub(ProviderGroupService) { _get(_) >> null }
        def c = new Contact(lastName: 'Admin', email: 'admin@example.org', userLastModified: 't').save(validate: false, flush: true)
        new ContactFor(contact: c, entityUid: 'dr404', administrator: true, userLastModified: 't').save(validate: false, flush: true)

        expect:
        service.authorisedForUser('admin@example.org').keys == []
    }
}
