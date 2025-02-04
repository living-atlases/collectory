package au.org.ala.collectory

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Collect datasets from an IPT service and update the metadata.
 * <p>
 * The IPT service is associated with a {@link DataProvider} that identifies the sources of the service.
 * When invoked, the service scans the RSS feed supplied by the service and uses it to identify new and
 * updated {@link DataResource}s. New datasets are collected and then supplied to the collectory for
 * loading.
 */
class IptService {

    static transactional = true
    def grailsApplication
    def idGeneratorService, emlImportService, collectoryAuthService, activityLogService

    /** The standard IPT service namespace for XML documents */
    static final NAMESPACES = [
            ipt:"http://ipt.gbif.org/",
            dc:"http://purl.org/dc/elements/1.1/",
            foaf:"http://xmlns.com/foaf/0.1/",
            geo:"http://www.w3.org/2003/01/geo/wgs84_pos#",
            eml:"eml://ecoinformatics.org/eml-2.1.1"
    ]
    /** Source of the RSS feed */
    static final RSS_PATH = "rss.do"
    /** Parse RFC 822 date/times */
    static final RFC822_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME

    /** Fields that we can derive from the RSS feed */
    protected rssFields = [
            guid: { item -> item.link.text() },
            name: { item -> item.title.text() },
            pubDescription: { item -> item.description.text() },
            websiteUrl: { item -> item.link.text() },
            dataCurrency: { item ->
                def pd = item.pubDate?.text()

                pd == null || pd.isEmpty() ? null : Timestamp.valueOf(LocalDateTime.parse(pd, RFC822_FORMATTER))
            },
            lastChecked: { item -> new Timestamp(System.currentTimeMillis()) },
            provenance: { item -> "Published dataset" },
            contentTypes: { item -> "[ \"point occurrence data\" ]" },
            gbifRegistryKey: { item ->
                def guid = item.guid?.text()
                if(guid && !guid.startsWith("http")) {
                    def versionMarker = guid.indexOf("/")
                    if (versionMarker > 0) {
                        guid = guid.substring(0, versionMarker)
                    }
                    guid
                } else {
                    null
                }
            }
    ]

    /** All field names */
    def allFields() {
        rssFields.keySet() + emlImportService.emlFields.keySet()
    }

    /**
     * See what needs updating for a data provider.
     *
     * @param provider The provider
     * @param create Update existing datasets and create data resources for new datasets
     * @param check Check to see whether a resource needs updating by looking at the data currency
     * @param keyName The term to use as a key when creating new resources
     *
     * @return A list of data resources that need re-loading
     */
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    def scan(DataProvider provider, boolean create, boolean check, String keyName, String username, boolean admin, boolean shareWithGbif) {
        activityLogService.log username, admin, provider.uid, Action.SCAN
        def updates = this.rss(provider, keyName, shareWithGbif)

        return merge(provider, updates, create, check, username, admin)
    }

    /**
     * Merge the datasets known to the dataprovider with a list of updates.
     *
     * @param provider The provider
     * @param updates The updates
     * @param create Update existing data resources and create any new resources
     * @param check Check against existing data for currency
     *
     * @return A list of merged data resources
     */
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    def merge(DataProvider provider, List updates, boolean create, boolean check, String username, boolean admin) {
        def currentResources = provider.resources.inject([:]) { map, resource ->
            map[resource.websiteUrl] = resource
            map
        }
        def mergedResources = []
        updates.each { update ->
            def existingResource = currentResources[update.resource.websiteUrl]

            if (existingResource) {
                if (!check || existingResource.dataCurrency == null || update.resource.dataCurrency == null || update.resource.dataCurrency.after(existingResource.dataCurrency)) {
                    DataResource.withTransaction {
                        updateFields(existingResource, update, username)
                        syncContacts(existingResource, update.contacts, update.primaryContacts, username, admin)
                        activityLogService.log(username, admin, Action.EDIT_SAVE, "Updated IPT data resource ${existingResource.uid} from scan")
                    }
                }
                mergedResources << existingResource
            } else {
                if (create) {
                    createNewResource(provider, update, username, admin)
                }
                mergedResources << update.resource
            }
        }

        mergedResources
    }

    private void updateFields(DataResource existingResource, Map update, String username) {
        def fieldsToUpdate = allFields() // Retrieves all fields that can be updated
        log.info("Fields to update: ${fieldsToUpdate}")

        fieldsToUpdate.each { fieldName ->
            if (update.containsKey(fieldName)) {
                def newValue = update[fieldName]
                existingResource.setProperty(fieldName, newValue) // Update the field with the new value (even if it's null)
            } else {
                existingResource.setProperty(fieldName, null) // Clear the field if it doesn't exist in the update
            }
        }

        existingResource.userLastModified = username
        existingResource.lastChecked = new Timestamp(System.currentTimeMillis())

        existingResource.save(flush: true)

        if (existingResource.hasErrors()) {
            existingResource.errors.each { error ->
                log.debug(error.toString())
            }
        }
    }

    private void syncContacts(DataResource resource, List<Contact> newContacts, List<Contact> primaryContacts, String username, boolean admin) {
        def existingContactForEntries = resource.getContacts()

        // Index current contacts by ID for quick lookup
        def existingContactsById = existingContactForEntries.collectEntries {
            [(it.contact.id): it]
        }

        // Index new contacts by ID (ensuring they are already persisted)
        def newContactsById = newContacts.findAll { it.id != null }.collectEntries {
            [(it.id): it]
        }

        // Identify contacts to remove (those that are in existingContacts but not in newContacts)
        def obsoleteContactsFor = existingContactForEntries.findAll { contactFor ->
            !newContactsById.containsKey(contactFor.contact.id)
        }

        // Remove obsolete contact associations first
        obsoleteContactsFor.each { contactFor ->
            def contact = contactFor.contact
            contactFor.delete(flush: true)

            activityLogService.log(username, admin, Action.DELETE, "Removed obsolete contact ${contact.buildName()} from resource ${resource.uid}")

            // Ensure no other resources use this contact before deleting it
            if (ContactFor.countByContact(contact) == 0) {
                contact.delete(flush: true)
                activityLogService.log(username, admin, Action.DELETE, "Deleted orphaned contact ${contact.buildName()}")
            }
        }

        // Identify contacts to add (only if they already exist in DB and are not already associated)
        newContacts.each { newContact ->
            def existingAssociation = ContactFor.findByContactAndEntityUid(newContact, resource.uid)
            if (!existingAssociation) {
                resource.addToContacts(newContact, null, false, primaryContacts.contains(newContact), username)
            } else {
                log.info("Skipping contact ${newContact.buildName()} for resource ${resource.uid} - already associated.")
            }
        }

        activityLogService.log(username, admin, Action.EDIT_SAVE, "Synced contacts for resource ${resource.uid}")
    }

    private void createNewResource(DataProvider provider, Map update, String username, boolean admin) {
        update.resource.uid = idGeneratorService.getNextDataResourceId()
        update.resource.userLastModified = username
        update.resource.dataProvider = provider

        DataResource.withTransaction {
            update.resource.save(flush: true, failOnError: true)
            syncContacts(update.resource, update.contacts, update.primaryContacts, username, admin)
        }
        activityLogService.log(username, admin, Action.CREATE, "Created new IPT data resource for provider ${provider.uid} with uid ${update.resource.uid} for dataset ${update.resource.websiteUrl}")
    }

    /**
     * Scan an IPT data provider's RSS stream and build a set of datasets.
     * <p>
     * The data sets are not saved, unless the need to create a new dataset comes into play
     *
     * @param provider The provider
     * @param keyName The term to use as a key when creating new resources
     *
     * @return A list of (possibly new providers)
     */
    def rss(DataProvider provider, String keyName, Boolean isShareableWithGBIF) {

        def url = provider.websiteUrl
        if(!url.endsWith("/")){
            url = url + "/"
        }

        URL base = new URL(url)
        URL rsspath = new URL(base, RSS_PATH)
        log.info("Scanning ${rsspath} from ${base}")
        def rss = new XmlSlurper().parse(rsspath.openStream())
        rss.declareNamespace(NAMESPACES)
        def items = rss.channel.item
        items.collect { item -> this.createDataResource(provider, item, keyName, isShareableWithGBIF) }
    }

    /**
     * Construct from an RSS item
     *
     * @param provider The data provider
     * @param rssItem The RSS item
     * @param keyName The term to use as a key when creating new resources
     *
     * @return A created resource matching the information provided
     */
    def createDataResource(DataProvider provider, rssItem, String keyName, Boolean isShareableWithGBIF) {
        def resource = new DataResource()
        def eml = rssItem."ipt:eml"?.text()
        def dwca = rssItem."ipt:dwca"?.text()

        resource.dataProvider = provider
        rssFields.each { name, accessor -> resource.setProperty(name, accessor(rssItem))}

        resource.connectionParameters =  dwca == null || dwca.isEmpty() ? null : """{ "protocol": "DwCA", "url": "${dwca}", "automation": true, "termsForUniqueKey": [ "${keyName}" ] }"""
        resource.isShareableWithGBIF = isShareableWithGBIF

        def contacts = []
        def primaryContacts = []
        if (eml != null && eml != "") {
            def result = retrieveEml(resource, eml)
            contacts = result.contacts
            primaryContacts = result.primaryContacts
        }

        [resource: resource, contacts: contacts, primaryContacts: primaryContacts]
    }

    /**
     * Retrieve Eco-informatics metadata for the dataset and put it into the resource description.
     *
     * @param resource The resource
     * @param url The URL of the metadata
     *
     */
    def retrieveEml(DataResource resource, String url) {
        try {
            log.info("Retrieving EML from " + url)
            def http = new URL(url)
            XmlSlurper xmlSlurper = new XmlSlurper()
            xmlSlurper.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            def eml = xmlSlurper.parse(http.openStream()).declareNamespace(NAMESPACES)
            emlImportService.extractContactsFromEml(eml, resource)
        } catch (Exception e){
            log.error("Problem retrieving EML from: " + url, e)
            []
        }
    }
}