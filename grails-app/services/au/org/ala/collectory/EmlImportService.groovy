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
    def extractContactsFromEml(eml, dataResource) {
        def contacts = []
        def primaryContacts = []
        def processedContacts = []

        emlFields.each { name, accessor ->
            def val = accessor(eml)
            if (val != null) {
                dataResource.setProperty(name, val)
            }
        }

        def addUniqueContact = { provider ->
            def providerName = "${provider.individualName?.givenName?.text()?.trim()}_${provider.individualName?.surName?.text()?.trim()}"
            def providerOrg = provider.organizationName?.text()?.trim()
            def providerPosition = provider.positionName?.text()?.trim()

            def uniqueKey = providerName ?: providerOrg ?: providerPosition
            if (uniqueKey && !processedContacts.contains(uniqueKey)) {
                def contact = addOrUpdateContact(provider)
                if (contact) {
                    contacts << contact
                    processedContacts << uniqueKey
                    return contact
                }
            }
        }

        // EML Schema
        // https://eml.ecoinformatics.org/images/eml-party.png

        if (eml.dataset.creator) {
            eml.dataset.creator.each { addUniqueContact(it) }
        }

        if (eml.dataset.contact) {
            eml.dataset.contact.each {
                def contact = addUniqueContact(it)
                if (contact) {
                    primaryContacts << contact
                }
            }
        }

        if (eml.dataset.metadataProvider) {
            eml.dataset.metadataProvider.each { addUniqueContact(it) }
        }

        if (eml.dataset.associatedParty) {
            eml.dataset.associatedParty.each { addUniqueContact(it) }
        }

        [contacts: contacts, primaryContacts: primaryContacts]
    }

    private def addOrUpdateContact(emlElement) {
        def contact = null
        if (emlElement.individualName?.givenName && emlElement.individualName?.surName) {
            contact = Contact.findByFirstNameAndLastName(
                    emlElement.individualName.givenName.text()?.trim(),
                    emlElement.individualName.surName.text()?.trim()
            )
        } else if (emlElement.individualName?.surName) {
            contact = Contact.findByLastName(emlElement.individualName.surName.text()?.trim())
        } else if (emlElement.organizationName) {
            contact = Contact.findByOrganizationName(emlElement.organizationName.text()?.trim())
        } else if (emlElement.positionName) {
            contact = Contact.findByPositionName(emlElement.positionName.text()?.trim())
        }

        boolean hasSurName = emlElement?.individualName?.surName?.text()?.trim()?.isEmpty() == false
        boolean hasOrg = emlElement?.organizationName?.text()?.trim()?.isEmpty() == false
        boolean hasPosition = emlElement?.positionName?.text()?.trim()?.isEmpty() == false
        String userId = emlElement.userId?.text()?.trim()
        String userIdDirectory = emlElement.userId?.@directory?.text()?.trim()
        String userIdUrl = userIdDirectory && userId ? "${userIdDirectory}${userId}" : null

        if (!contact && (hasSurName || hasOrg || hasPosition)) {
            contact = new Contact()
            contact.firstName = emlElement.individualName?.givenName?.text()?.trim()
            contact.lastName = emlElement.individualName?.surName?.text()?.trim()
            contact.organizationName = emlElement.organizationName?.text()?.trim()
            contact.positionName = emlElement.positionName?.text()?.trim()
            contact.userId = userIdUrl
            contact.email = emlElement.electronicMailAddress?.text()?.trim()
            contact.phone = emlElement.phone?.text()?.trim()
            contact.setUserLastModified(collectoryAuthService.username())

            Contact.withTransaction {
                if (contact.validate()) {
                    contact.save(flush: true, failOnError: true)
                } else {
                    log.error("Validation errors creating contact: ${contact.errors}")
                    return null
                }
            }
        } else if (contact) {
            // Update existing contact fields if they differ
            boolean updated = false
            if (emlElement.phone?.text()?.trim() && emlElement.phone.text().trim() != contact.phone) {
                contact.phone = emlElement.phone.text().trim()
                updated = true
            }
            if (emlElement.electronicMailAddress?.text()?.trim() && emlElement.electronicMailAddress.text().trim() != contact.email) {
                contact.email = emlElement.electronicMailAddress.text().trim()
                updated = true
            }
            if (emlElement.individualName?.givenName?.text()?.trim() && emlElement.individualName.givenName.text().trim() != contact.firstName) {
                contact.firstName = emlElement.individualName.givenName.text().trim()
                updated = true
            }
            if (emlElement.individualName?.surName?.text()?.trim() && emlElement.individualName.surName.text().trim() != contact.lastName) {
                contact.lastName = emlElement.individualName.surName.text().trim()
                updated = true
            }
            if (emlElement.organizationName?.text()?.trim() && emlElement.organizationName.text().trim() != contact.organizationName) {
                contact.organizationName = emlElement.organizationName.text().trim()
                updated = true
            }
            if (emlElement.positionName?.text()?.trim() && emlElement.positionName.text().trim() != contact.positionName) {
                contact.positionName = emlElement.positionName.text().trim()
                updated = true
            }
            if (userIdUrl != contact.userId) {
                contact.userId = userIdUrl
                updated = true
            }
            if (updated) {
                contact.setUserLastModified(collectoryAuthService.username())
                Contact.withTransaction {
                    if (contact.validate()) {
                        contact.save(flush: true, failOnError: true)
                    } else {
                        log.error("Validation errors updating contact: ${contact.errors}")
                        return null
                    }
                }
            }
        }
        return contact
    }

}
