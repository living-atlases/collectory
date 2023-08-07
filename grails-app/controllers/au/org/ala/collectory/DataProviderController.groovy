package au.org.ala.collectory

import grails.converters.JSON
import grails.converters.XML
import grails.web.http.HttpHeaders

class DataProviderController extends ProviderGroupController {

    def gbifRegistryService
    def authService
    def iptService

    DataProviderController() {
        entityName = "DataProvider"
        entityNameLower = "dataProvider"
    }

    def index = {
        redirect(action:"list")
    }

    // list all entities
    def list = {
        if (params.message) {
            flash.message = params.message
        }
        params.max = Math.min(params.max ? params.int('max') : 10000, 10000)
        params.sort = params.sort ?: "name"
        activityLogService.log username(), isAdmin(), Action.LIST
        [instanceList: DataProvider.list(params), entityType: 'DataProvider', instanceTotal: DataProvider.count()]
    }

    def show = {
        def instance = get(params.id)
        if (!instance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'dataProvider.label', default: 'Data Provider'), params.id])}"
            redirect(action: "list")
        }
        else {
            log.info "Ala partner = " + instance.isALAPartner
            activityLogService.log username(), isAdmin(), instance.uid, Action.VIEW

            [instance: instance, contacts: instance.getContacts(), changes: getChanges(instance.uid)]
        }
    }

    /**
     * Populate the GBIF Organizations / DataProvider list, by country, for the Import Data Provider from GBIF screen
     */
    def searchForOrganizations = {
        def countryMap = gbifService.getCountryMap()
        def countryName = null
        def lastCreatedUID = null
        def organizations = null

        def countryCode = (params.country) ? params.country : grailsApplication.config.countryCode
        if ((countryCode) && (countryCode != "NO_VALUE")) {
            log.info "Search for organizations for country = " + countryCode

            countryName = countryMap.get(countryCode)
            lastCreatedUID = params.lastCreatedUID

            organizations = gbifRegistryService.loadOrganizationsByCountry(countryCode)

            if (organizations){
                log.info "Search for organizations returned " + organizations.size()+" organizations for country = " + countryCode
                for (organization in organizations) {
                    def dp = DataProvider.findByGbifRegistryKey(organization.key)
                    if (dp) {
                        log.info "Organization "+organization.key+" is already imported as data provider = " + dp.uid
                        organization.uid = dp.uid
                        if (organization.uid == lastCreatedUID) {
                            organization.lastCreated = true
                        }
                    }
                    else {
                        log.info "Organization "+organization.key+" is not yet imported as data provider"
                        organization.statusAvailable = true
                    }
                }
            } else {
                log.info "Search for organizations returned no results for ${countryCode}"
            }
        }

        render(view: 'gbif/list',
                model: [
                    countryMap: countryMap,
                    country: countryCode,
                    countryName: countryName,
                    organizations: organizations,
                    entityType: 'DataProvider'
                ]
        )
    }

    def iptScan = {
        def create = params.create != null && params.create.equalsIgnoreCase("true")
        def check = params.check == null || !params.check.equalsIgnoreCase("false")
        def keyName = params.key ?: 'catalogNumber'
        def isShareableWithGBIF = params.isShareableWithGBIF ? params.isShareableWithGBIF.toBoolean(): true
        def provider = providerGroupService._get(params.uid)

        def username = collectoryAuthService.username()
        def admin =  collectoryAuthService.userInRole(grailsApplication.config.ROLE_ADMIN)
        try {
            def updates = provider == null ? null : iptService.scan(provider, create, check, keyName, username, admin, isShareableWithGBIF)
            log.info "${updates.size()} data resources to update for ${params.uid}"
            response.addHeader HttpHeaders.VARY, HttpHeaders.ACCEPT
            withFormat {
                text {
                    render updates.findAll({ dr -> dr.uid != null }).collect({ dr -> dr.uid }).join("\n")
                }
                xml {
                    render updates as XML
                }
                json {
                    render updates as JSON
                }
            }
        } catch (Exception e){
            log.error("Problem scanning IPT endpoint: " + e.getMessage(), e)
            render (status: 500, text: "Problem scanning data provider " + params.uid)
            return
        }
    }

    /**
     * Create a single data provider for a selected GBIF organization
     */
    def importFromOrganization = {
        def organizationKey = params.organizationKey
        log.info "Importing organization "+organizationKey+" as data provider"

        DataProvider dp = new DataProvider(uid: idGeneratorService.getNextDataProviderId(), userLastModified: collectoryAuthService?.username(), gbifCountryToAttribute: grailsApplication.config.gbifDefaultEntityCountry)
        gbifRegistryService.populateDataProviderFromOrganization(dp, organizationKey)

        if (!dp.hasErrors()) {
            DataProvider.withTransaction {
                dp.save(flush: true)
            }
            log.info "Data Provider "+dp.uid+" successfully created from organization = " + organizationKey
            flash.message = "${message(code: 'default.created.message', args: [message(code: "${dp.urlForm()}", default: dp.urlForm()), dp.uid])}"
            redirect(action: "searchForOrganizations", params: [country: params.country, lastCreatedUID: dp.uid])
        } else {
            log.error "Unable to create Data Provider from organization = " + organizationKey
            flash.message = message(code: "provider.group.controller.06", default: "Failed to create new") + " ${entityName}"
            redirect(controller: 'admin', action: 'index')
        }
    }

    /**
     * Create data providers for each GBIF organization of a selected country
     * Ignore organizations whose GBIF key is already associated to a data provider
     */
    def importAllFromOrganizations = {
        def countryCode = params.country
        log.info "Importing all organizations from country "+countryCode+" as data provider"

        def organizations = gbifRegistryService.loadOrganizationsByCountry(countryCode)
        log.info organizations.size() + " organizations found for country = " + countryCode

        def successCount = 0
        def errorCount = 0

        organizations.each { organization ->
            def dp = DataProvider.findByGbifRegistryKey(organization.key)
            if (!dp) {
                dp = new DataProvider(uid: idGeneratorService.getNextDataProviderId(), userLastModified: collectoryAuthService?.username(), gbifCountryToAttribute: grailsApplication.config.gbifDefaultEntityCountry)
                gbifRegistryService.populateDataProviderFromOrganization(dp, organization.key)

                if (!dp.hasErrors()) {
                    DataProvider.withTransaction {
                        dp.save(flush: true)
                    }

                    log.info "Data Provider "+dp.uid+" successfully created from organization = " + organization.key
                    successCount++
                } else {
                    log.error "Unable to create Data Provider from organization = " + organization.key
                    errorCount++
                }
            }
            else {
                log.info "Ignoring organization "+organization.key+" because already imported as data provider = " + dp.uid
            }
        }

        flash.message = "Success: " + successCount + " / Error: " + errorCount
        redirect(action: "searchForOrganizations", params: [country: params.country])
    }

    def editConsumers = {
        def pg = get(params.id)
        if (!pg) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: "${entityNameLower}.label", default: entityNameLower), params.id])}"
            redirect(action: "list")
        } else {
            // are they allowed to edit
            if (collectoryAuthService?.userInRole(grailsApplication.config.ROLE_ADMIN) || !grailsApplication.config.security.oidc.enabled.toBoolean()) {
                render(view: '../dataResource/consumers', model:[command: pg, source: params.source])
            } else {
                render("You are not authorised to edit these properties.")
            }
        }
    }

    def updateConsumers = {
        def pg = get(params.id)
        def newConsumers = params.consumers.tokenize(',')
        def oldConsumers = (pg.consumerCollections + pg.consumerInstitutions).collect { it.uid }
        // create new links
        newConsumers.each { String nc ->
            if (!(nc in oldConsumers)) {
                DataProvider.withTransaction {
                    if (nc[0..1] == 'co') {
                        Collection c = Collection.findByUid(nc)
                        pg.consumerCollections.add(c)
                    } else {
                        Institution i = Institution.findByUid(nc)
                        pg.consumerInstitutions.add(i)
                    }
                    pg.save(flush: true)
                    auditLog(pg, 'INSERT', 'consumer', '', nc, pg)
                    log.info "created link from ${pg.uid} to ${nc}"
                }
            }
        }
        // remove old links - NOTE only for the variety (collection or institution) that has been returned
        oldConsumers.each { String oc ->
            if (!(oc in newConsumers) && oc[0..1] == params.source) {
                DataProvider.withTransaction {
                    log.info "deleting link from ${pg.uid} to ${oc}"
                    if (oc[0..1] == 'co') {
                        Collection c = Collection.findByUid(oc)
                        pg.consumerCollections.remove(c)
                    } else {
                        Institution i = Institution.findByUid(oc)
                        pg.consumerInstitutions.remove(i)
                    }
                    pg.save(flush: true)
                    auditLog(pg, 'DELETE', 'consumer', oc, '', pg)
                }
            }
        }
        flash.message =
            "${message(code: 'default.updated.message', args: [message(code: "${pg.urlForm()}.label", default: pg.entityType()), pg.uid])}"
        redirect(action: "show", id: pg.uid)
    }

    def delete = {
        def instance = get(params.id)
        if (instance) {
            if (isAdmin()) {
                /* need to remove it as a parent from all children otherwise they will be deleted */
                DataProvider.withTransaction {
                    def resources = instance.resources as List
                    resources.each {
                        instance.removeFromResources it
                        it.userLastModified = username()
                        it.save()  // necessary?
                    }
                    // remove contact links (does not remove the contact)
                    ContactFor.findAllByEntityUid(instance.uid).each {
                        it.delete()
                    }
                    // now delete
                    try {
                        activityLogService.log username(), isAdmin(), params.id as long, Action.DELETE
                        instance.delete(flush: true)
                        flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'dataProvider.label', default: 'Data Provider'), params.id])}"
                        redirect(action: "list")
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'dataProvider.label', default: 'Data Provider'), params.id])}"
                        redirect(action: "show", id: params.id)
                    }
                }
            } else {
                render("You are not authorised to access this page.")
            }
        } else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'dataProvider.label', default: 'Data Provider'), params.id])}"
            redirect(action: "list")
        }
    }

    def updateAllGBIFRegistrations = {
        gbifRegistryService.updateAllRegistrations()
        flash.message = "${message(code: 'dataProvider.gbif.updateAll', default: 'Updating all GBIF registrations as a background task (please be patient).')}"
        redirect(action: "list")
    }

    def updateGBIFDetails = {
        def pg = get(params.id)
        genericUpdate pg, 'gbif'
    }

    /**
     * This will update the GBIF Registry with the metadata and contacts for the data provider.
     */
    def updateGBIF = {
        def instance = get(params.id)
        if (instance) {
            try {
                if(authService.userInRole(grailsApplication.config.gbifRegistrationRole)) {
                    log.info("[User ${authService.getUserId()}] has selected to update ${instance.uid} in GBIF...")
                    Boolean syncDataResources = params.syncDataResources?:"false".toBoolean()
                    Boolean syncContacts  = params.syncContacts?:"false".toBoolean()
                    gbifRegistryService.updateRegistration(instance, syncContacts, syncDataResources)

                    flash.message = "${message(code: 'dataProvider.gbif.update.success', default: 'GBIF Registration Updated')}"
                } else {
                    flash.message = "User does not have sufficient privileges to perform this. ${grailsApplication.config.gbifRegistrationRole} role required"
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e)
                flash.message = "${e.getMessage()}"
            }

            redirect(action: "show", id: params.id)
        }
    }

    /**
     * Register this data provider as an Organisation with GBIF.
     */
    def registerGBIF = {
        log.info("REGISTERING data partner ${collectoryAuthService.username()}")

        if(authService.userInRole(grailsApplication.config.gbifRegistrationRole)) {
            DataProvider instance = get(params.id)
            if (instance) {
                try {
                    log.info("REGISTERING ${instance.uid}, triggered by user: ${collectoryAuthService.username()}")
                    if (collectoryAuthService.userInRole(grailsApplication.config.gbifRegistrationRole)) {
                        log.info("[User ${authService.getUserId()}] has selected to register ${instance.uid} in GBIF...")

                        Boolean syncDataResources = params.syncDataResources?:"false".toBoolean()
                        Boolean syncContacts  = params.syncContacts?:"false".toBoolean()

                        gbifRegistryService.register(instance, syncContacts, syncDataResources)
                        flash.message = "${message(code: 'dataProvider.gbif.register.success', default: 'Successfully Registered in GBIF')}"
                        DataProvider.withTransaction {
                            instance.save()
                        }
                    } else {
                        log.info("REGISTERING FAILED for ${instance.uid}, triggered by user: ${collectoryAuthService.username()} - user not in role")
                        flash.message = "You don't have permission to do register this data partner."
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                    flash.message = "${e.getMessage()}"
                }

                redirect(action: "show", id: params.id)
            }
        } else {
            flash.message = "User does not have sufficient privileges to perform this. ${grailsApplication.config.gbifRegistrationRole} role required"
            redirect(action: "show", id: params.id)
        }
    }

    /**
     * Get the instance for this entity based on either uid or DB id.
     *
     * @param id UID or DB id
     * @return the entity of null if not found
     */
    protected ProviderGroup get(id) {
        if (id.size() > 2) {
            if (id[0..1] == DataProvider.ENTITY_PREFIX) {
                return providerGroupService._get(id)
            }
        }
        // else must be long id
        long dbId
        try {
            dbId = Long.parseLong(id)
        } catch (NumberFormatException e) {
            return null
        }
        return DataProvider.get(dbId)
    }

}
