package au.org.ala.collectory

import groovy.util.slurpersupport.GPathResult

class EmlImportService {

    def serviceMethod() {}

    def dataLoaderService, collectoryAuthService

    /** Collect individual XML para elements together into a single block of text */
    protected def collectParas(GPathResult paras) {
        paras?.list().inject(null, { acc, para -> acc == null ? (para.text()?.trim() ?: "") : acc + " " + (para.text()?.trim() ?: "") })
    }

    public emlFields = [

        guid:  { eml -> eml.@packageId.toString() },
        pubDescription: { eml -> this.collectParas(eml.dataset.abstract?.para) },
        name: { eml -> eml.dataset.title.toString() },
        email: { eml ->  eml.dataset.contact.size() > 0 ? eml.dataset.contact[0]?.electronicMailAddress?.text(): null },
        rights: { eml ->  this.collectParas(eml.dataset.intellectualRights?.para) },
        citation: { eml ->  eml.additionalMetadata?.metadata?.gbif?.citation?.text() },
        state: { eml ->

            def state = ""

            def administrativeAreas = eml.dataset.contact.size() > 0 ? eml.dataset.contact[0]?.address?.administrativeArea: null
            if (administrativeAreas){

                if (administrativeAreas.size() > 1){
                    state = administrativeAreas.first().text()
                } else {
                    state = administrativeAreas.text()
                }
                if (state) {
                    state = this.dataLoaderService.massageState(state)
                }
            }
            state
        },
        phone: { eml ->  eml.dataset.contact.size() > 0 ? eml.dataset.contact[0]?.phone?.text(): null },

        //geographic coverage
        geographicDescription: { eml -> eml.dataset.coverage?.geographicCoverage?.geographicDescription?:'' },
        northBoundingCoordinate: { eml -> eml.dataset.coverage?.geographicCoverage?.boundingCoordinates?.northBoundingCoordinate?:''},
        southBoundingCoordinate: { eml -> eml.dataset.coverage?.geographicCoverage?.boundingCoordinates?.southBoundingCoordinate?:''},
        eastBoundingCoordinate : { eml -> eml.dataset.coverage?.geographicCoverage?.boundingCoordinates?.eastBoundingCoordinate?:''},
        westBoundingCoordinate: { eml -> eml.dataset.coverage?.geographicCoverage?.boundingCoordinates?.westBoundingCoordinate?:''},

        //temporal
        beginDate: { eml -> eml.dataset.coverage?.temporalCoverage?.rangeOfDates?.beginDate?.calendarDate?:''},
        endDate: { eml -> eml.dataset.coverage?.temporalCoverage?.rangeOfDates?.endDate?.calendarDate?:''},

        //additional fields
        purpose: { eml -> eml.dataset.purpose?.para?:''},
        methodStepDescription: { eml -> eml.dataset.methods?.methodStep?.description?.para?:''},
        qualityControlDescription: { eml -> eml.dataset.methods?.qualityControl?.description?.para?:''},

        gbifDoi: { eml ->
            def gbifDoi = null
            eml.dataset.alternateIdentifier?.each {
                def id = it.text()
                if (id && id.startsWith("doi")) {
                    gbifDoi = id
                }
            }
            gbifDoi
        },

        licenseType: { eml -> getLicence(eml).licenseType },
        licenseVersion: { eml -> getLicence(eml).licenseVersion }
    ]


    def getLicence(eml){

        def licenceInfo = [licenseType:'', licenseVersion:'']
        //try and match the acronym to licence
        def rights = this.collectParas(eml.dataset.intellectualRights?.para)

        def matchedLicence = Licence.findByAcronym(rights)
        if (!matchedLicence) {
            //attempt to match the licence
            def licenceUrl = eml.dataset.intellectualRights?.para?.ulink?.@url.text()
            def licence = Licence.findByUrl(licenceUrl)
            if (licence == null) {
                if (licenceUrl.contains("http://")) {
                    matchedLicence = Licence.findByUrl(licenceUrl.replaceAll("http://", "https://"))
                } else {
                    matchedLicence = Licence.findByUrl(licenceUrl.replaceAll("https://", "http://"))
                }
            } else {
                matchedLicence = licence
            }
        }

        if(matchedLicence){
            licenceInfo.licenseType = matchedLicence.acronym
            licenceInfo.licenseVersion = matchedLicence.licenceVersion
        }

        licenceInfo
    }

    /**
     * Extracts a set of properties from an EML document, populating the
     * supplied dataresource, connection params.
     *
     * @param xml
     * @param dataResource
     * @param connParams
     * @return
     */
    def extractContactsFromEml(eml, dataResource){

        def contacts = []
        def primaryContacts = []

        emlFields.each { name, accessor ->
            def val = accessor(eml)
            if (val != null) {
                dataResource.setProperty(name, val)
            }
        }

        //add contacts...
        if (eml.dataset.creator){
            eml.dataset.creator.each {
                def contact = addOrUpdateContact(it)
                if (contact){
                    contacts << contact
                }
            }
        }

        if (eml.dataset.metadataProvider
                && eml.dataset.metadataProvider.electronicMailAddress != eml.dataset.creator.electronicMailAddress){

            eml.dataset.metadataProvider.each {
                def contact = addOrUpdateContact(it)
                if (contact){
                    contacts << contact
                }
            }
        }

        // Add additional contacts
        if (eml.dataset.contact){
            eml.dataset.contact.each {
                def contact = addOrUpdateContact(it)
                if (contact){
                    contacts << contact
                    primaryContacts << contact
                }
            }
        }

        if (eml.dataset.associatedParty){
            eml.dataset.associatedParty.each {
                def contact = addOrUpdateContact(it)
                if (contact){
                    contacts << contact
                }
            }
        }

        [contacts: contacts, primaryContacts: primaryContacts]
    }

    private def addOrUpdateContact(emlElement) {
        def contact = null
        if (emlElement.electronicMailAddress && !emlElement.electronicMailAddress.isEmpty()) {
            String email = emlElement.electronicMailAddress.text().trim()
            contact = Contact.findByEmail(email)
        } else if (emlElement.individualName.givenName && emlElement.individualName.surName) {
            contact = Contact.findByFirstNameAndLastName(emlElement.individualName.givenName, emlElement.individualName.surName)
        } else if (emlElement.individualName.surName) {
            // surName is mandatory
            contact = Contact.findByLastName(emlElement.individualName.surName)
        }

        // Create the contact if it doesn't exist and it's a individualName with email or surName
        // to prevent empty contacts (e.g. with emlElement.organizationName only)
        boolean hasEmail = emlElement?.electronicMailAddress?.text()?.trim()?.isEmpty() == false
        boolean hasName = emlElement?.individualName?.surName?.text()?.trim()?.isEmpty() == false

        if (!contact && (hasEmail || hasName)) {
            contact = new Contact()
        } else {
            return null
        }

        // Update the contact details
        contact.firstName = emlElement.individualName.givenName
        contact.lastName = emlElement.individualName.surName
        // some email has leading/trailing spaces causing the email constrain regexp to fail, lets trim
        contact.email = emlElement.electronicMailAddress.text().trim()
        contact.setUserLastModified(collectoryAuthService.username())
        Contact.withTransaction {
            if (contact.validate()) {
                contact.save(flush: true, failOnError: true)
                return contact
            } else {
                contact.errors.each {
                    log.error("Problem creating contact: " + it)
                }
                return null
            }
        }

        contact
    }
}
