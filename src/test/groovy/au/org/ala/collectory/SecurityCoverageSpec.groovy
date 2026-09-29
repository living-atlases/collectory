package au.org.ala.collectory

import au.org.ala.PermissionRequired
import au.org.ala.SkipPermissionCheck
import au.org.ala.grails.AnnotationMatcher
import grails.core.GrailsControllerClass
import grails.testing.spring.AutowiredTest
import spock.lang.Specification

import java.lang.reflect.Modifier

/**
 * Security matrix guard: every controller action that looks like it changes data must be
 * protected by @PermissionRequired (directly or at class level), unless it is listed in
 * ALLOWED_UNPROTECTED with a reason. This stops a new endpoint from being published open
 * by accident.
 *
 * Protection is resolved with the same AnnotationMatcher call PermissionInterceptor uses.
 */
class SecurityCoverageSpec extends Specification implements AutowiredTest {

    static final File CONTROLLER_DIR = new File('grails-app/controllers/au/org/ala/collectory')

    static final WRITE_ACTION = ~/(?i)^(save|update|delete|create|insert|add|remove|upload|sync|scan|import|merge|edit|generate|register|approve|reject|change|clear|reset|load|harvest|ingest|notif|refresh|rebuild|send|submit|transfer|set).*/

    /** controller.action -> why it is acceptable without @PermissionRequired */
    static final Map<String, String> ALLOWED_UNPROTECTED = [
            'data.notification'  : 'write-only activity notification; only logs to the activity log',
            'data.fileDownload'  : 'protected in the action by the IP whitelist / API key check (#327), see DataControllerFileDownloadSpec',
            'data.notifyList'    : 'read only list of contacts to notify',
            // public helper closures/methods of DataController that Grails also exposes as actions;
            // they only set response headers or a status, they do not change data
            'data.addContentLocation'   : 'response header helper',
            'data.addETagHeader'        : 'response header helper',
            'data.addLastModifiedHeader': 'response header helper',
            'data.addLocation'          : 'response header helper',
            'data.addVaryAcceptHeader'  : 'response header helper',
            'data.created'              : 'response status helper',
    ]

    Map<String, GrailsControllerClass> controllers = [:]

    def setup() {
        controllerClasses().each { Class c ->
            def artefact = grailsApplication.addArtefact('Controller', c) as GrailsControllerClass
            controllers[artefact.logicalPropertyName] = artefact
        }
    }

    static List<Class> controllerClasses() {
        CONTROLLER_DIR.listFiles().findAll { it.name.endsWith('Controller.groovy') }.collect {
            Class.forName("au.org.ala.collectory.${it.name - '.groovy'}")
        }.findAll { !Modifier.isAbstract(it.modifiers) }
    }

    private boolean isProtected(String controller, String action) {
        def match = AnnotationMatcher.getAnnotation(grailsApplication, null, controller, action, PermissionRequired, SkipPermissionCheck)
        match.effectiveAnnotation() != null && match.overrideAnnotation == null
    }

    private List<String> writeActions() {
        controllers.collectMany { String name, GrailsControllerClass cc ->
            cc.actions.findAll { it ==~ WRITE_ACTION }.collect { "${name}.${it}".toString() }
        }.sort()
    }

    def "all controllers under grails-app/controllers are inspected"() {
        expect:
        controllers.size() >= 20
        controllers.keySet().containsAll(['data', 'dataResource', 'collection', 'lookup', 'public', 'gbif', 'ipt'])
    }

    def "every action that changes data requires a permission"() {
        when:
        def unprotected = writeActions().findAll { !isProtected(it.tokenize('.')[0], it.tokenize('.')[1]) } - ALLOWED_UNPROTECTED.keySet()

        then: 'the heuristic still finds the known write actions'
        writeActions().containsAll(['data.saveEntity', 'data.delete', 'data.updateContact', 'collection.updateBase',
                                    'dataResource.delete', 'licence.delete', 'gbif.syncAllResources', 'ipt.scan'])

        and:
        unprotected == []
    }

    def "allow-list entries refer to existing actions"() {
        expect:
        ALLOWED_UNPROTECTED.keySet().every { key ->
            def (c, a) = key.tokenize('.')
            controllers[c]?.actions?.contains(a)
        }
    }

    def "every action of the editor-only controllers requires a permission, except explicit skips"() {
        given:
        def editorOnly = ['collection', 'institution', 'dataProvider', 'dataResource', 'dataHub', 'contact', 'admin',
                          'manage', 'providerCode', 'providerMap', 'reports', 'tempDataResource', 'entity']

        when:
        def open = editorOnly.collectMany { c ->
            controllers[c].actions.findAll { a ->
                def match = AnnotationMatcher.getAnnotation(grailsApplication, null, c, a, PermissionRequired, SkipPermissionCheck)
                match.effectiveAnnotation() == null
            }.collect { "${c}.${it}".toString() }
        }

        then:
        open == []
    }

    def "manage actions are admin only"() {
        expect:
        ManageController.getAnnotation(PermissionRequired).roles() as List == ['ROLE_ADMIN']
    }

    def "GBIF registration actions require the GBIF registration role"() {
        expect:
        ['healthCheck', 'healthCheckLinked', 'downloadCSV', 'syncAllResources', 'scan'].every { a ->
            def ann = GbifController.getMethod(a).getAnnotation(PermissionRequired)
            ann && 'gbifRegistrationRole' in ann.roles()
        }
    }

    def "read-only public web services stay anonymous (#329)"() {
        expect:
        ['getEntity', 'listEntity', 'findEntities', 'eml', 'index'].every { !isProtected('data', it) }
        ['collection', 'institution', 'dataProvider', 'dataResource', 'name', 'citations', 'summary'].every { !isProtected('lookup', it) }
        !isProtected('tempDataResource', 'getEntity')
    }

    def "uid generation is limited to editors and M2M clients"() {
        expect:
        ['generateCollectionUid', 'generateInstitutionUid', 'generateDataProviderUid', 'generateDataResourceUid', 'generateDataHubUid'].every {
            isProtected('lookup', it)
        }
    }
}
