package au.org.ala.collectory

import com.opencsv.CSVWriter
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.http.HttpEntity
import org.apache.http.HttpException
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext

import java.text.MessageFormat

import java.util.concurrent.Executors

/**
 * Services required to register and update organisation and datasets in GBIF.
 *
 * This is intended to be used only when the ALA is the publishing gateway to GBIF and not when the ALA installation is
 * sourcing it's data from GBIF.  The service was originally written for the needs of the UK ALA installation.
 */
class GbifRegistryService {

    def grailsApplication
    def isoCodeService

    // URL templates for the GBIF API relative to the base GBIF API url (e.g. https://api.gbif.org)
    static final String API_ORGANIZATION = "organization"
    static final String API_ORGANIZATION_COUNTRY_LIMIT = "organization?country={0}&limit={1}"
    static final String API_ORGANIZATION_DETAIL = "organization/{0}"
    static final String API_ORGANIZATION_CONTACT = "organization/{0}/contact"
    static final String API_ORGANIZATION_CONTACT_DETAIL = "organization/{0}/contact/{1}"

    static final String API_DATASET = "dataset"
    static final String API_DATASET_DETAIL = "dataset/{0}"
    static final String API_DATASET_ENDPOINT = "dataset/{0}/endpoint"
    static final String API_DATASET_ENDPOINT_DETAIL = "dataset/{0}/endpoint/{1}"

    static def pool = Executors.newFixedThreadPool(1) // very conservative

    Boolean isDryRun(){
        grailsApplication.config.gbifRegistrationDryRun.toBoolean()
    }

    /**
     * Updates all registrations of data providers and data resources with GBIF.  This will create missing datasets
     * in GBIF, update the organisation metadata in GBIF, and set DOIs in the datasets in the Collectory if configured
     * to do so (i.e. config useGbifDoi=true).
     */
    def updateAllRegistrations() {
        def providers = []
        try {
            DataProvider.withTransaction {
                providers = DataProvider.list()
                providers.each { dp ->
                    log.info("Update of registration of ${dp.uid}: ${dp.name} ${pool}")
                    updateRegistration(dp, true, true)
                }
                providers
            }
        } catch (Exception e){
          log.error(e.getMessage(), e)
        }
        providers
    }

    /**
     * Updates the registration in GBIF for the DataProvider.
     * This updates the key metadata and the contacts which is typically all publishers provide to GBIF.
     */
    def updateRegistration(ProviderGroup dp, Boolean syncContacts, Boolean syncDataResources) throws Exception {
        if (dp.gbifRegistryKey) {
            boolean success = updateRegistrationMetadata(dp)
            if(success){
                log.info("Successfully updated provider in GBIF: ${dp.gbifRegistryKey}")
                if (syncContacts){
                    syncContactsForProviderGroup(dp)
                }
                log.info("Successfully synced contacts: ${dp.gbifRegistryKey}")
                if (syncDataResources) {
                    syncDataResourcesForProviderGroup(dp)
                    log.info("Successfully synced resources in GBIF: ${dp.gbifRegistryKey}")
                }
            }
        } else {
            log.info("No GBIF registration exists for dp[${dp.uid}] - nothing to update")
        }
    }

    /**
     * Update the registration metadata.
     *
     * @param dp
     * @return boolean indicating success
     */
    private boolean updateRegistrationMetadata(ProviderGroup dp) {
        log.info("Updating GBIF organisation ${dp.uid}: ${dp.gbifRegistryKey}")

        boolean success = false

        // load the current GBIF entry to get the endorsing node key
        def organisation = loadOrganization(dp.gbifRegistryKey)
        if (!organisation){
            return false
        }

        // apply mutations
        populateOrganisation(organisation, dp)

        if (!isDryRun()) {
            // update mutated version in GBIF
            def httpclient = newHttpInstance()
            HttpPut httpPut = new HttpPut(
                    grailsApplication.config.gbifApiUrl +
                            MessageFormat.format(API_ORGANIZATION_DETAIL, dp.gbifRegistryKey)
            )
            httpPut.setHeader("Accept", "application/json")
            httpPut.setHeader("Content-Type", "application/json")
            StringEntity stringEntity = new StringEntity((organisation as JSON).toString());
            httpPut.setEntity(stringEntity);
            HttpResponse response = httpclient.execute(httpPut);
            if (isSuccess(response)) {
                success = true
            }
        } else {
            log.info("[DRY-RUN] Registration request for ${dp.uid} - ${dp.name}")
            log.info((organisation as JSON).toString())
        }
        success
    }

    private boolean isSuccess(HttpResponse response) {
        response.getStatusLine().getStatusCode() in [200, 201, 202, 203, 204]
    }

    /**
     * Creates a new registration in GBIF for the DataProvider as a publishing organization, endorsed by the relevant
     * node.  Note: the GBIF Country to Attribute is used to instruct GBIF which country should be credited with
     * publishing the data.
     */
    def register(ProviderGroup dp, Boolean syncContacts, Boolean syncDataResources) throws Exception {
        // create the entity with the mandatory fields in GBIF
        def organisation = [
                "endorsingNodeKey": grailsApplication.config.gbifEndorsingNodeKey,
                "endorsementApproved": true,
                "language": "eng" // required by GBIF
        ]
        populateOrganisation(organisation, dp)

        // create the organization and update the collectory DB
        if (!isDryRun()) {

            def http = newHttpInstance();
            HttpPost httpPost = new HttpPost(grailsApplication.config.gbifApiUrl + API_ORGANIZATION)
            httpPost.setEntity(new StringEntity((organisation as JSON).toString()))
            httpPost.setHeader("Content-Type", "application/json")
            HttpResponse httpResponse = http.execute(httpPost)
            HttpEntity entity = httpResponse.getEntity()
            String responseString = EntityUtils.toString(entity, "UTF-8")

            if (isSuccess(httpResponse)){
                dp.gbifRegistryKey = responseString.replaceAll('"', "") // more sloppy GBIF responses
                log.info("Successfully created provider in GBIF: ${dp.gbifRegistryKey}")
                DataProvider.withTransaction {
                    dp.save(flush: true)
                }

                if (syncContacts) {
                    log.info("Attempting to sync contacts: ${dp.gbifRegistryKey}")
                    syncContactsForProviderGroup(dp)
                    log.info("Successfully created contacts: ${dp.gbifRegistryKey}")
                }

                if (syncDataResources) {
                    log.info("Attempting to sync resources: ${dp.gbifRegistryKey}")
                    syncDataResourcesForProviderGroup(dp)
                    log.info("Successfully created resources: ${dp.gbifRegistryKey}")
                }
            } else {
                log.info("Unable to register organisation = ${httpResponse.getStatusLine().getStatusCode()}: ${responseString}")
                log.debug((organisation as JSON).toString() )

            }
        } else {
            log.info("[DRY RUN] Registration request for ${dp.uid} - ${dp.name}")
            log.info((organisation as JSON).toString())
        }
    }

    /**
     * Favours institutions, and then looks for a data provider link.
     *
     * @param dataResource
     * @return
     */
    def registerDataResource(DataResource dataResource){

        def result = [success:false, message:""]

        def publisherGbifRegistryKey = "" //data provider or institution

        def institution = dataResource.institution
        def dataProvider = dataResource.dataProvider

        if (!institution) {

            //get the data provider if available...
            def dataLinks = DataLink.findAllByProvider(dataResource.uid)
            def institutionDataLink

            if (dataLinks) {
                //do we have institution link ????
                institutionDataLink = dataLinks.find { it.consumer.startsWith("in") }
                if (institutionDataLink) {
                    institution = Institution.findByUid(institutionDataLink.consumer)
                }
            }
        }

        if (institution) {
            // sync institution

            if(institution.gbifRegistryKey){
                updateRegistrationMetadata(institution)
            } else {
                register(institution, true, false)
            }

            publisherGbifRegistryKey = institution.gbifRegistryKey

        } else if (dataProvider) {

            // sync institution
            if (dataProvider.gbifRegistryKey){
                updateRegistrationMetadata(dataProvider)
            } else {
                register(dataProvider, true, false)
            }

            publisherGbifRegistryKey = dataProvider.gbifRegistryKey

        } else if (grailsApplication.config.gbifOrphansPublisherID){
            log.info("Unable to sync resource: ${dataResource.uid} -  ${dataResource.name}. No publishing organisation associated.")
            publisherGbifRegistryKey = grailsApplication.config.gbifOrphansPublisherID
        } else {
            log.info("Unable to sync resource: ${dataResource.uid} -  ${dataResource.name}. No publishing organisation associated.")
            result.success = false
            result.message = "Unable to sync resource: ${dataResource.uid} -  ${dataResource.name}. No publishing organisation associated."
        }

        //if no institution, get the data provider and create in GBIF
        if (publisherGbifRegistryKey) {
            //create the resource in GBIF
            log.info("Syncing data resource ${dataResource.uid} -  ${dataResource.name}")
            syncDataResource(dataResource, publisherGbifRegistryKey)
            log.info("Sync complete for data resource ${dataResource.uid} -  ${dataResource.name}")
            result.success = true
            result.message = "Data resource sync-ed with GBIF."
        }

        result
    }

    /**
     * Syncs the contacts with the GBIF registry.
     */
    private def syncContactsForProviderGroup(ProviderGroup dp) {
        // load the current value from GBIF and remove the contacts
        def organisation = loadOrganization(dp.gbifRegistryKey)
        if (!organisation){
            log.error("Unable to sync contacts for GBIF id ${dp.gbifRegistryKey}")
            return
        }
        if (organisation.contacts) {
            log.info("Removing contacts")
            organisation.contacts.each {
                if (!isDryRun()) {
                    def http = newHttpInstance();
                    HttpDelete httpDelete = new HttpDelete(
                            grailsApplication.config.gbifApiUrl +
                                    MessageFormat.format(API_ORGANIZATION_CONTACT_DETAIL, dp.gbifRegistryKey, it.key as String)
                    )
                    HttpResponse httpResponse = http.execute(httpDelete)
                    if (isSuccess(httpResponse)){
                        log.info("Removed contact ${it.key as String}")
                    }
                }
            }
        }

        // now add the current ones from the collectory
        if (dp.contacts) {
            def http = newHttpInstance();

            dp.contacts.each {
                log.info("Adding contact ${it.contact}")
                def gbifContact = [
                        "firstName": it.contact.firstName,
                        "lastName": it.contact.lastName,
                        "type": "ADMINISTRATIVE_POINT_OF_CONTACT",
                        "email": [it.contact.email],
                        "phone": [it.contact.phone]
                ]

                if (!isDryRun()) {
                    HttpPost httpPost = new HttpPost(
                            grailsApplication.config.gbifApiUrl +
                                    MessageFormat.format(API_ORGANIZATION_CONTACT, dp.gbifRegistryKey)
                    )
                    httpPost.setHeader("Content-Type", "application/json")
                    httpPost.setEntity(new StringEntity((gbifContact as JSON).toString()))
                    HttpResponse httpResponse = http.execute(httpPost)
                    if (isSuccess(httpResponse)){
                        log.info("Added contact")
                    }
                }
            }
        }
    }

    /**
     * This creates any missing data resources and updates endpoints for all datasets.
     * Deletions are not propogated at this point instead deferring to the current helpdesk@gbif.org process.
     */
    private def syncDataResourcesForProviderGroup(ProviderGroup dp) {

        if (dp.gbifRegistryKey) {
            if (dp instanceof DataProvider) {
                def resources = dp.getResources()
                resources.each { resource ->

                    def skipSync = false
                    // if theres an institution link
                    if (resource.institution){
                        //dont sync
                        log.warn("${resource.uid} is sourced from an institution [${resource.institution.uid}]... not syncing  ")
                        skipSync = true
                    }

                    def dataLinks = DataLink.findAllByProvider(dp.uid)
                    dataLinks.each { dataLink ->
                        if (dataLink.consumer.startsWith("in")){
                            skipSync = true
                            log.warn("${resource.uid} is linked to an institution [${dataLink.consumer}]... not syncing  ")
                        }
                    }

                    if (!skipSync) {
                        syncDataResource(resource, dp.gbifRegistryKey)
                    }
                }
            } else {
                log.warn("Need to add syncing of resources for institution....via datalinks...")
                def dataLinks = DataLink.findAllByConsumer(dp.uid)
                if (dataLinks) {
                    dataLinks.each { dataLink ->
                        DataResource dr = DataResource.findByUid(dataLink.provider)
                        if (dr) {
                            syncDataResource(dr, dp.gbifRegistryKey)
                        }
                    }
                }
            }
        } else {
            log.warn("Not syncing resources for ${dp}. Not registered with GBIF....")
        }
    }

    /**
     * Sync the data resource with the provided ProviderGroup instance.
     *
     * @param dataResource the resource to sync.
     * @param organisationRegistryKey the gbif key for the publishing institution or a data provider.
     * @return
     */
    def syncDataResource(DataResource dataResource, String organisationRegistryKey){
        // register the missing datasets
        if (!dataResource.gbifRegistryKey) {
            log.info("Creating GBIF resource for ${dataResource.uid}")
            def dataset = newGBIFDatasetInstance(dataResource, organisationRegistryKey)
            log.info("Creating dataset in GBIF: ${dataset}")

            if (dataset) {
                if (!isDryRun()) {
                    def http = newHttpInstance();
                    HttpPost httpPost = new HttpPost(grailsApplication.config.gbifApiUrl + MessageFormat.format(API_DATASET, organisationRegistryKey))
                    httpPost.setHeader("Content-Type", "application/json")
                    httpPost.setEntity(new StringEntity((dataset as JSON).toString()))
                    HttpResponse httpResponse = http.execute(httpPost)
                    HttpEntity entity = httpResponse.getEntity()

                    String responseString = EntityUtils.toString(entity, "UTF-8")
                    if (isSuccess(httpResponse) && responseString){
                        dataResource.gbifRegistryKey = responseString.replaceAll('"', "") // more sloppy GBIF responses
                        log.info("Added dataset ${dataResource.gbifRegistryKey}")
                        log.info("Successfully created dataset in GBIF: ${dataResource.gbifRegistryKey}")
                        DataProvider.withTransaction {
                            dataResource.save(flush: true)
                        }
                    } else {
                        log.error("Unable to add dataset ${dataResource.uid}, " +
                                "status code:${httpResponse.getStatusLine().getStatusCode()}, " +
                                "response string: ${responseString}")
                    }

                    if (Boolean.valueOf(grailsApplication.config.useGbifDoi) && dataResource.gbifRegistryKey) {
                        def created = loadDataset(dataResource.gbifRegistryKey)
                        if (created) {
                            dataResource.gbifDoi = created.doi
                            DataProvider.withTransaction {
                                dataResource.save(flush: true)
                            }
                        } else {
                            log.error("Unable to load GBIF ID ${dataResource.gbifRegistryKey}")
                        }
                    }
                } else {
                    log.info("[DRY RUN] Registration request for ${dataset.uid} - ${dataset.name}")
                    log.info((dataset as JSON).toString())
                }
            } else {
                log.warn("Unable to register dataset - please check license: ${dataResource.uid} :  ${dataResource.name} :  ${dataResource.licenseType}")
            }
        } else {
            // ensure the organisation is correct in GBIF as ownership varies over time, and that the DOI
            // is used if configured
            def dataset = loadDataset(dataResource.gbifRegistryKey)
            if (dataset) {
                if (!isDryRun()) {
                    if (Boolean.valueOf(grailsApplication.config.useGbifDoi) && dataResource.gbifDoi != dataset.doi) {
                        log.info("Setting resource[${dataResource.uid}] to use gbifDOI[${dataset.doi}]")
                        dataResource.gbifDoi = dataset.doi
                        DataProvider.withTransaction {
                            dataResource.save(flush: true)
                        }
                    }

                    log.info("Updating the GBIF registry dataset[${dataResource.gbifRegistryKey}] " +
                            "to point to " +
                            "organisation[${organisationRegistryKey}]")
                    dataset.publishingOrganizationKey = organisationRegistryKey
                    dataset.deleted = null
                    dataset.license = getGBIFCompatibleLicence(dataResource.licenseType)
                    dataset.title = dataResource.name
                    dataset.description = dataResource.pubDescription
                    if (dataResource.buildLogoUrl()) {
                        dataset.logoUrl = dataResource.buildLogoUrl()
                    } else {
                        dataset.logoUrl = null
                    }
                    if (dataResource.websiteUrl) {
                        dataset.homepage = dataResource.websiteUrl
                    } else {
                        dataset.homepage = null
                    }
                    if (dataset.license) {
                        def http = newHttpInstance();
                        def datasetKey = dataResource.gbifRegistryKey
                        HttpPut httpPut = new HttpPut(grailsApplication.config.gbifApiUrl + MessageFormat.format(API_DATASET_DETAIL, datasetKey))
                        httpPut.setHeader("Content-Type", "application/json")
                        httpPut.setEntity(new StringEntity((dataset as JSON).toString()))
                        HttpResponse httpResponse = http.execute(httpPut)
                        if (isSuccess(httpResponse)) {
                            log.info("Successfully updated dataset in GBIF: ${datasetKey}")
                        }
                    } else {
                        log.warn("Unable to update dataset - please check license: ${dataResource.uid} :  ${dataResource.name} :  ${dataResource.licenseType}")
                    }
                } else {
                    log.info("[DRYRUN] Updating data resource ${dataset}")
                }
            } else {
                log.error("Unable to update ${dataResource.uid} as GBIF lookup failed for ${dataResource.gbifRegistryKey}")
            }
        }

        syncEndpoints(dataResource)
    }

    def deleteDataResource(DataResource resource){

        def http = newHttpInstance()
        if (!isDryRun()) {
            HttpDelete httpDelete = new HttpDelete(
                    grailsApplication.config.gbifApiUrl +
                            MessageFormat.format(API_DATASET_DETAIL, resource.gbifRegistryKey))
            HttpResponse httpResponse = http.execute(httpDelete)
            if (isSuccess(httpResponse)){
                log.info("Deleted  Dataset[${resource.gbifRegistryKey}] from GBIF")
                resource.gbifRegistryKey = null
                DataProvider.withTransaction {
                    resource.save(flush: true)
                }
            } else {
                log.info("The delete of ${resource.uid} from GBIF was unsuccessful: ${httpResponse.getStatusLine()}")
            }
        } else {
            log.info("[DryRun] Deleting ${resource.uid}")
        }
        resource
    }

    /**
     * Checks that the GBIF registry holds the single endpoint for the data resource creating it or updating if required.
     */
    private def syncEndpoints(DataResource resource) {
        if (resource.gbifRegistryKey) {
            log.info("Syncing endpoints for resource[${resource.id}], gbifKey[${resource.gbifRegistryKey}]")

            def http = newHttpInstance()
            def dataset = loadDataset(resource.gbifRegistryKey)
            if (dataset) {

                if (!isDryRun()) {

                    def dwcaUrl = grailsApplication.config.resource.gbifExport.url.template.replaceAll("@UID@", resource.getUid());

                    if (dataset.endpoints && dataset.endpoints.size() == 1 && dwcaUrl.equals(dataset.endpoints.get(0).url)) {
                        log.info("Dataset[${resource.gbifRegistryKey}] has correct URL[${dwcaUrl}]")
                    } else {

                        // delete the existing ones
                        if (dataset.endpoints) {
                            dataset.endpoints.each {

                                HttpDelete httpDelete = new HttpDelete(
                                        grailsApplication.config.gbifApiUrl +
                                                MessageFormat.format(API_DATASET_ENDPOINT_DETAIL, resource.gbifRegistryKey, it.key as String)
                                )
                                HttpResponse httpResponse = http.execute(httpDelete)
                                if (isSuccess(httpResponse)){
                                    log.info("Removed endpoint ${it.key as String}")
                                }
                            }
                        }

                        // now add the correct one
                        def endpoint = [
                                "type": "DWC_ARCHIVE",
                                "url" : dwcaUrl
                        ]

                        HttpPost httpPost = new HttpPost(
                                grailsApplication.config.gbifApiUrl +
                                        MessageFormat.format(API_DATASET_ENDPOINT, resource.gbifRegistryKey)
                        )
                        httpPost.setHeader("Content-Type", "application/json")
                        httpPost.setEntity(new StringEntity((endpoint as JSON).toString()))
                        HttpResponse httpResponse = http.execute(httpPost)

                        if (isSuccess(httpResponse)){
                            log.info("Created endpoint for Dataset[${resource.gbifRegistryKey}] with URL[${endpoint.url}]")
                        }
                    }
                } else {
                    log.info("[DRYRUN] syncing endpoints with registry key ${resource.gbifRegistryKey}, for resource ${resource.uid}")
                }
            } else {
                log.info("Unable to load dataset info from GBIF with registry key ${resource.gbifRegistryKey}, for resource ${resource.uid}. Not syncing.....")
            }
        } else {
            log.info("Registry key not set for resource: ${resource.uid}. Not syncing.....")
        }
    }

    def getGBIFCompatibleLicence(String licenseType){

        if (grailsApplication.config.gbifLicenceMappingUrl && grailsApplication.config.gbifLicenceMappingUrl != 'null'){
            def jsonLicense = new JsonSlurper().parse(new URL(grailsApplication.config.gbifLicenceMappingUrl))
            return jsonLicense.get(licenseType)
        } else {

            // map to GBIF, recognising GBIF are particular about the correct name
            switch (licenseType) {
                case 'CC0':
                    return 'https://creativecommons.org/publicdomain/zero/1.0/legalcode'
                case 'CC-BY':
                    return 'https://creativecommons.org/licenses/by/4.0/legalcode'
                case 'CC-BY-NC':
                    return 'https://creativecommons.org/licenses/by-nc/4.0/legalcode'
                case 'CC-BY-NC':
                    return 'https://creativecommons.org/licenses/by-nc/4.0/legalcode'
                case 'OGL':
                    // See https://en.wikipedia.org/wiki/Open_Government_Licence
                    // Note that publisher has explicitly confirmed a desire to register in GBIF, knowing that GBIF support
                    // CC0, CC-BY and CC-BY-NC only.  This seems the most appropriate license to map to.
                    return 'https://creativecommons.org/licenses/by/4.0/legalcode'
                default:
                    log.info("Unsupported license ${licenseType} for GBIF so cannot be registered")
                    return null
            }
        }
    }


    /**
     * Creates the content for a dataset to POST to GBIF from the supplied resource or null if it can't be created.
     */
    private def newGBIFDatasetInstance(DataResource resource, String organisationRegistryKey) {
        def license = getGBIFCompatibleLicence(resource.licenseType)

        if (!license){
            return null
        }

        def dataset = [
                "type": "OCCURRENCE",
                "license": license,
                "installationKey": grailsApplication.config.gbifInstallationKey,
                "publishingOrganizationKey": organisationRegistryKey,
                "title": resource.name,
                "description": resource.pubDescription
        ]
        return dataset
    }

    /**
     * Loads all organizations for a specific country from the GBIF API.
     */
    def loadOrganizationsByCountry(String countryCode, int limit = 1000) {
        def http = newHttpInstance()
        HttpGet httpGet = new HttpGet(
                grailsApplication.config.gbifApiUrl +
                        MessageFormat.format(API_ORGANIZATION_COUNTRY_LIMIT, countryCode, limit.toString()))
        HttpResponse httpResponse = http.execute(httpGet)
        if (isSuccess(httpResponse)){
            ByteArrayOutputStream bos = new ByteArrayOutputStream()
            httpResponse.getEntity().writeTo(bos)
            String respText = bos.toString();
            JsonSlurper slurper = new JsonSlurper()
            return slurper.parseText(respText)?.results
        } else {
            log.error("Error response ${httpResponse.getStatusLine().getStatusCode()} for ${API_ORGANIZATION_COUNTRY_LIMIT} with ${countryCode} and ${limit}")
            [:]
        }
    }

    /**
     * Loads an organization from the GBIF API.
     */
    private def loadOrganization(gbifRegistryKey) {
        def http = newHttpInstance()
        HttpGet httpGet = new HttpGet(
                grailsApplication.config.gbifApiUrl +
                        MessageFormat.format(API_ORGANIZATION_DETAIL, gbifRegistryKey))
        HttpResponse httpResponse = http.execute(httpGet)
        if (isSuccess(httpResponse)){
            ByteArrayOutputStream bos = new ByteArrayOutputStream()
            httpResponse.getEntity().writeTo(bos)
            String respText = bos.toString();
            JsonSlurper slurper = new JsonSlurper()
            return slurper.parseText(respText)
        } else {
            log.error("Error response ${httpResponse.getStatusLine().getStatusCode()} for ${API_ORGANIZATION_DETAIL} with ${gbifRegistryKey}")
            [:]
        }
    }

    /**
     * Loads a dataset from the GBIF API.
     */
    private def loadDataset(gbifRegistryKey) {
        def http = newHttpInstance()
        HttpGet httpGet = new HttpGet(
                grailsApplication.config.gbifApiUrl +
                        MessageFormat.format(API_DATASET_DETAIL, gbifRegistryKey)
        )
        HttpResponse httpResponse = http.execute(httpGet)
        if (isSuccess(httpResponse)){
            ByteArrayOutputStream bos = new ByteArrayOutputStream()
            httpResponse.getEntity().writeTo(bos)
            String respText = bos.toString();
            JsonSlurper slurper = new JsonSlurper()
            slurper.parseText(respText)
        } else {
            log.error("Error response ${httpResponse.getStatusLine().getStatusCode()} for ${API_DATASET_DETAIL} with ${gbifRegistryKey}")
            [:]
        }
    }

    /**
     * Takes the values from the DataProvider and populates them in the organisation object suitable for the GBIF API.
     */
    private def populateOrganisation(Object organisation, ProviderGroup dp) {
        organisation.title = dp.name
        // defensive coding follows to pass GBIF validation rules
        if (dp.acronym && dp.acronym.length()<=10) {
            organisation.abbreviation = dp.acronym
        }
        organisation.description = dp.pubDescription
        if (dp.email) {
            organisation.email = [dp.email]
        }
        if (dp.phone) {
            organisation.phone = [dp.phone]
        }
        if (dp.websiteUrl) {
            organisation.homepage = [dp.websiteUrl]
        }
        organisation.latitude = Math.floor(dp.latitude as float) == -1.0 ? null : dp.latitude
        organisation.longitude = Math.floor(dp.longitude as float) == -1.0 ? null : dp.longitude
        if (dp.buildLogoUrl()) {
            organisation.logoUrl = dp.buildLogoUrl()
        }

        // convert the 3 digit ISO code to the 2 digit ISO code GBIF needs
        // Note: GBIF use this for counting "data published by Country X".  There are cases where the postal Address
        // indicates the headquarters of an international organisation and the country it is located should not be
        // credited in GBIF as "owning the data".  For those cases, the country is left deliberately null.  This is a
        // GBIF specific requirement.
        organisation.country = null
        if (dp.gbifCountryToAttribute) {
            def iso2 = isoCodeService.iso3CountryCodeToIso2CountryCode(dp.gbifCountryToAttribute.toUpperCase())
            if (iso2) {
                log.info("Setting GBIF country of attribution to ${iso2}")
                organisation.country = iso2
            }
        }

        Address address = dp.getAddress()
        if (address) {
            organisation.province = address.state
            organisation.address = [address.street]
            organisation.city = address.city
            organisation.postalCode = address.postcode
        }
    }

    def populateDataProviderFromOrganization(DataProvider dp, String organisationKey) {
        def organisation = loadOrganization(organisationKey)
        if (!organisation){
            return
        }
        dp.gbifRegistryKey = organisation.key
        dp.name = organisation.title
        dp.acronym = organisation.abbreviation
        dp.pubDescription = organisation.description
        dp.email = organisation.email[0]
        dp.phone = organisation.phone[0]
        dp.websiteUrl = organisation.homepage[0]
        if (organisation.latitude) {
            dp.latitude = organisation.latitude
        }
        if (organisation.longitude) {
            dp.longitude = organisation.longitude
        }
        dp.gbifCountryToAttribute = isoCodeService.iso2CountryCodeToIso3CountryCode(organisation.country)
        dp.address = new Address()
        dp.address.state = organisation.province
        dp.address.street = organisation.address[0]
        dp.address.city = organisation.city
        dp.address.postcode = organisation.postalCode
    }

    def writeCSVReportForGBIF(outputStream) {

        log.debug("Starting report.....")
        def url = grailsApplication.config.biocacheServicesUrl + "/occurrences/search?q=*:*&facets=data_resource_uid&pageSize=0&facet=on&flimit=-1"

        def js = new JsonSlurper()
        def biocacheSearch = js.parse(new URL(url), "UTF-8")

        def csvWriter = new CSVWriter(new OutputStreamWriter(outputStream))

        String[] header = [
                "UID",
                "Data resource",
                "Data resource GBIF ID",
                "Record count",

                "Data provider UID",
                "Data provider name",
                "Data provider GBIF ID",

                "Institution UID",
                "Institution name",
                "Institution GBIF ID",

                "Licence",

                "Shareable with GBIF",
                "Licence Issues (preventing sharing)",

                "Not Shareable (no owner)",
                "Flagged as Not-Shareable",
                "Provided by GBIF",

                "Linked to Data Provider",
                "Linked to Institution",

                "Verified"
        ]

        csvWriter.writeNext(header)

        biocacheSearch.facetResults[0].fieldResult.each { result ->
            def uid = result.fq.replaceAll("\"","").replaceAll("data_resource_uid:","")

            //retrieve current licence
            def dataResource = DataResource.findByUid(uid)
            if (dataResource) {

                def isShareable = true
                def licenceIssues = false
                def flaggedAsNotShareable = false
                def providedByGBIF = false
                def notShareableNoOwner = false

                //retrieve current licence
                def dataProvider
                def institution

                //get the data provider if available...
                def dataLinks = DataLink.findAllByProvider(uid)
                def institutionDataLink

                if (dataLinks){
                    //do we have institution link ????
                    institutionDataLink = dataLinks.find { it.consumer.startsWith("in")}
                    if(institutionDataLink){
                        //we have an institution
                        institution = Institution.findByUid(institutionDataLink.consumer)
                    }
                }

                if(!institutionDataLink) {
                    dataProvider = dataResource.getDataProvider()
                    if(!dataProvider){
                        notShareableNoOwner = true
                        isShareable = false //no institution and no data provider
                    }
                }

                if (dataResource.licenseType == null || !getGBIFCompatibleLicence(dataResource.licenseType)) {
                    licenceIssues = true
                    isShareable = false
                }

                if (!dataResource.isShareableWithGBIF) {
                    flaggedAsNotShareable = true
                    isShareable = false
                }

                if (dataResource.gbifDataset) {
                    providedByGBIF = true
                    isShareable = false
                }

                String[] row = [
                        dataResource.uid,
                        dataResource.name,
                        dataResource.gbifRegistryKey,

                        result.count,

                        dataProvider?.uid,
                        dataProvider?.name,
                        dataProvider?.gbifRegistryKey,

                        institution?.uid,
                        institution?.name,
                        institution?.gbifRegistryKey,

                        dataResource.licenseType,

                        isShareable ? "yes" : "no",
                        licenceIssues ? "yes" : "no",
                        notShareableNoOwner ? "yes" : "no",
                        flaggedAsNotShareable ? "yes" : "no",
                        providedByGBIF ? "yes" : "no",

                        institution ? "yes" : "no",
                        dataProvider ? "yes" : "no",

                        dataResource.isVerified() ? "yes" : "no"

                ]
                csvWriter.writeNext(row)
            }
        }
        csvWriter.close()
    }

    /**
     * Synchronise all resources with GBIF.
     *
     * @return a map of statistics showing number of updates.
     */
    def syncAllResources(){

        def results = generateSyncBreakdown()

        def resourcesRegistered= 0
        def resourcesUpdated = 0
        def dataProviderRegistered = 0
        def dataProviderUpdated = 0
        def institutionsRegistered = 0
        def institutionsUpdated = 0

        log.info("Attempting to sync ${results.shareable.size()} data resources.....")

        results.shareable.keySet().each { dataResource ->

            def publisherGbifRegistryKey = "" //data provider or institution

            //get the institution, and check it has been created in GBIF
            Institution institution = results.linkedToInstitution.get(dataResource)
            DataProvider dataProvider = results.linkedToDataProvider.get(dataResource)
            if (institution) {
                // sync institution
                if (institution.gbifRegistryKey){
                    updateRegistrationMetadata(institution)
                    institutionsUpdated ++
                } else {
                    register(institution, true, false)
                    institutionsRegistered ++
                }

                publisherGbifRegistryKey = institution.gbifRegistryKey

            } else if (dataProvider) {
                // sync institution
                if(dataProvider.gbifRegistryKey){
                    updateRegistrationMetadata(dataProvider)
                    dataProviderUpdated ++
                } else {
                    register(dataProvider, true, false)
                    dataProviderRegistered ++
                }
                publisherGbifRegistryKey = dataProvider.gbifRegistryKey
            } else if (grailsApplication.config.gbifOrphansPublisherID){
                publisherGbifRegistryKey = grailsApplication.config.gbifOrphansPublisherID
                log.info("Using orphans publisher ID  to sync resource: ${dataResource.uid}")
            } else {
                log.info("Unable to sync resource: ${dataResource.uid} -  ${dataResource.name}. " +
                        "No publishing organisation associated." +
                        "gbifOrphansPublisherID = ${grailsApplication.config.gbifOrphansPublisherID}")
            }

            // if no institution, get the data provider and create in GBIF
            if (publisherGbifRegistryKey) {
                //create the resource in GBIF
                log.info("Syncing data resource ${dataResource.uid} -  ${dataResource.name}")
                if (dataResource.gbifRegistryKey){
                    resourcesUpdated ++
                } else {
                    resourcesRegistered ++
                }

                try {
                    syncDataResource(dataResource, publisherGbifRegistryKey)
                    log.info("Sync complete for data resource ${dataResource.uid} -  ${dataResource.name}")
                } catch (Exception e){
                    log.error("Sync error for data resource ${dataResource.uid} -  ${dataResource.name} - " + e.getMessage(), e)
                }
            }
        }
        [
                resourcesRegistered : resourcesRegistered,
                resourcesUpdated : resourcesUpdated,
                dataProviderRegistered : dataProviderRegistered,
                dataProviderUpdated : dataProviderUpdated,
                institutionsRegistered : institutionsRegistered,
                institutionsUpdated : institutionsUpdated
        ]
    }

    /**
     * Retrieves a breakdown of data resources with available data.
     *
     * @return
     */
    def generateSyncBreakdown(){
        def url = grailsApplication.config.biocacheServicesUrl + "/occurrences/search?q=*:*&facets=data_resource_uid&pageSize=0&facet=on&flimit=-1"

        def js = new JsonSlurper()
        def biocacheSearch = js.parse(new URL(url), "UTF-8")

        def dataResourcesWithData = [:]
        def shareable = [:]
        def licenceIssues = [:]
        def notShareable = [:]
        def providedByGBIF = [:]
        def notShareableNoOwner = [:]
        def linkedToDataProvider = [:]
        def linkedToInstitution = [:]
        def recordsShareable = 0

        biocacheSearch.facetResults[0].fieldResult.each { result ->
            def uid = result.fq.replaceAll("\"","").replaceAll("data_resource_uid:","")

            def isShareable = true

            //retrieve current licence
            def dataResource = DataResource.findByUid(uid)
            if(dataResource) {

                dataResourcesWithData[dataResource] = result.count

                //find links to institutions
                def institution = dataResource.institution

                if (institution){
                    linkedToInstitution[dataResource] = dataResource.institution

                } else {

                    //get the data provider if available...
                    def dataLinks = DataLink.findAllByProvider(uid)
                    def institutionDataLink

                    if (dataLinks) {
                        //do we have institution link ????
                        institutionDataLink = dataLinks.find { it.consumer.startsWith("in") }
                        if (institutionDataLink) {

                            institution = Institution.findByUid(institutionDataLink.consumer)

                            //we have an institution
                            linkedToInstitution[dataResource] = institution
                        }
                    }
                }

                if (!institution) {
                    def dataProvider = dataResource.getDataProvider()
                    if (dataProvider){
                        linkedToDataProvider[dataResource] = dataProvider
                    } else {

                        // if there is not a orphans publisher ID configured, theres no home
                        if (!grailsApplication.config.gbifOrphansPublisherID) {
                            notShareableNoOwner[dataResource] = result.count
                            isShareable = false //no institution and no data provider
                        }
                    }
                }

                if (dataResource.licenseType == null || !getGBIFCompatibleLicence(dataResource.licenseType)) {
                    licenceIssues[dataResource] = result.count
                    isShareable = false
                }

                if (!dataResource.isShareableWithGBIF) {
                    notShareable[dataResource] = result.count
                    isShareable = false
                }

                if (dataResource.gbifDataset) {
                    providedByGBIF[dataResource] = result.count
                    isShareable = false
                }

                if (isShareable) {
                    shareable[dataResource] = result.count
                    recordsShareable += result.count
                }
            }
        }
        [
                indexedRecords : biocacheSearch.totalRecords,
                recordsShareable: recordsShareable,
                dataResourcesWithData:dataResourcesWithData,
                shareable:shareable,
                licenceIssues:licenceIssues,
                notShareable:notShareable,
                providedByGBIF:providedByGBIF,
                notShareableNoOwner:notShareableNoOwner,
                linkedToDataProvider: linkedToDataProvider,
                linkedToInstitution: linkedToInstitution,
        ]
    }

    /**
     * Creates a new instance of an HTTP builder configured with the standard error handling.
     * By default, use the basic authentication account
     */
    HttpClient newHttpInstance() {
        HttpClientBuilder builder = HttpClientBuilder.create()
        BasicCredentialsProvider basicCredentialsProvider =  new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(grailsApplication.config.gbifApiUser , grailsApplication.config.gbifApiPassword)
        basicCredentialsProvider.setCredentials(AuthScope.ANY, credentials);
        builder.setDefaultCredentialsProvider(basicCredentialsProvider);
        builder.addInterceptorFirst(new PreemptiveAuthInterceptor());
        builder.build()
    }

    static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
        @Override
        public void process (HttpRequest request, HttpContext context) throws HttpException {
            AuthState authState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);
            if (authState.getAuthScheme() == null) {
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);
                Credentials credentials = credsProvider.getCredentials(AuthScope.ANY);
                if (credentials == null) {
                    throw new HttpException("No credentials provided for preemptive authentication.");
                }
                authState.update(new BasicScheme(), credentials);
            }
        }
    }
}