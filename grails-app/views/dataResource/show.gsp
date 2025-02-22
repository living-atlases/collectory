<%@ page import="grails.converters.JSON; au.org.ala.collectory.ProviderGroup; au.org.ala.collectory.DataProvider" %>
<%@ page import="au.org.ala.collectory.GbifRegistryService" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="breadcrumbParent"
              content="${createLink(action: 'list', controller: 'manage')},${message(code: 'manage.list.title01')}"
        />
        <meta name="breadcrumbs"
              content="${createLink(action: 'list', controller: 'dataResource')},Datasets"
        />
        <meta name="layout" content="${grailsApplication.config.skin.layout}" />
        <g:set var="entityName" value="${instance.ENTITY_TYPE}" />
        <g:set var="entityNameLower" value="${cl.controller(type: instance.ENTITY_TYPE)}"/>
        <title>${instance.name}</title>
        <script async defer
                src="https://maps.googleapis.com/maps/api/js?key=${grailsApplication.config.google?.apikey}"
                type="text/javascript"></script>
        <asset:stylesheet src="application.css"/>
    </head>
    <body onload="initializeLocationMap('${instance.canBeMapped()}',${instance.latitude},${instance.longitude});">
    <style>
    #mapCanvas {
      width: 200px;
      height: 170px;
      float: right;
    }
    </style>
        <div class="btn-toolbar">
            <ul class="btn-group">
                <li class="btn btn-default"><cl:homeLink/></li>
                <li class="btn btn-default"><span class="glyphicon glyphicon-list"></span><g:link class="list" action="list"> <g:message code="default.list.label" args="[entityName]"/></g:link></li>
%{--                <li class="btn btn-default"><span class="glyphicon glyphicon-list"></span><g:link class="list" action="myList"> <g:message code="default.myList.label" args="[entityName]"/></g:link></li>--}%
                <li class="btn btn-default"><span class="glyphicon glyphicon-plus"></span><g:link class="create" action="create"> <g:message code="default.new.label" args="[entityName]"/></g:link></li>
            </ul>
            <ul class="btn-group pull-right">
                <li class="btn btn-default"><cl:viewPublicLink uid="${instance?.uid}"/></li>
                <li class="btn btn-default"><cl:jsonSummaryLink uid="${instance.uid}"/></li>
                <li class="btn btn-default"><cl:jsonDataLink uid="${instance.uid}"/></li>
                <li class="btn btn-default"><cl:emlDataLink uid="${instance.uid}"/></li>
                <g:if test="${instance.getPrimaryContact()?.contact?.email}"><li class="btn btn-default"><a href="mailto:${instance.getPrimaryContact()?.contact?.email}?subject=Request to review web pages presenting information about the ${instance.name}.&body=${contactEmailBody}"><span class="glyphicon glyphicon-envelope"></span><g:message code="default.query.label"/></a></li></g:if>
            </ul>
        </div>
        <div class="body">
            <g:if test="${flash.message}">
                <div class="alert alert-warning">${flash.message}</div>
            </g:if>

            <div class="dialog emulate-public">
              <!-- base attributes -->
              <div class="show-section well titleBlock">
                <!-- Name --><!-- Acronym -->
                <h1>${fieldValue(bean: instance, field: "name")}<cl:valueOrOtherwise value="${instance.acronym}"> (${fieldValue(bean: instance, field: "acronym")})</cl:valueOrOtherwise></h1>

                <!-- Data Provider --><!-- ALA Partner -->
                <g:if test="${instance.dataProvider}">
                    <h2 style="display:inline"><g:link controller="dataProvider" action="show" id="${instance.dataProvider?.id}">${instance.dataProvider?.name}</g:link></h2>
                </g:if>

                <cl:partner test="${instance.dataProvider?.isALAPartner}"/><br/>

                <!-- Institution -->
                <g:if test="${instance.institution}">
                  <p><span class="category"><g:message code="dataresource.show.sor" />: </span> <g:link controller="institution" action="show" id="${instance.institution?.uid}">${instance.institution?.name}</g:link></p>
                </g:if>

                <!-- GUID    -->
                <p><span class="category"><g:message code="dataresource.show.guid" />: </span> <cl:guid target="_blank" guid='${fieldValue(bean: instance, field: "guid")}'/></p>

                <!-- DOI    -->
                <p><span class="category"><g:message code="dataresource.show.doi" default="doi" />: </span> <cl:guid target="_blank" guid='${fieldValue(bean: instance, field: "gbifDoi")}'/></p>

                <!-- UID    -->
                <p><span class="category"><g:message code="providerGroup.uid.label" />: </span> ${fieldValue(bean: instance, field: "uid")}</p>

                <!-- type -->
                <p><span class="category"><g:message code="dataresource.show.resourcetype" />: </span> ${fieldValue(bean: instance, field: "resourceType")}</p>

                <!-- Web site -->
                <p><span class="category"><g:message code="dataresource.show.website" />: </span> <cl:externalLink href="${fieldValue(bean:instance, field:'websiteUrl')}"/></p>

                <!-- Networks -->
                <g:if test="${instance.networkMembership}">
                  <p><cl:membershipWithGraphics coll="${instance}"/></p>
                </g:if>

                <!-- Notes -->
                <g:if test="${instance.notes}">
                  <p><cl:formattedText>${fieldValue(bean: instance, field: "notes")}</cl:formattedText></p>
                </g:if>

                <!-- last edit -->
                <p><span class="category"><g:message code="dataresource.show.lastchange" />: </span> ${fieldValue(bean: instance, field: "userLastModified")} on ${fieldValue(bean: instance, field: "lastUpdated")}</p>

                <g:if test="${instance.gbifDataset}">
                    <p>This dataset was downloaded from GBIF. <a href="https://www.gbif.org/dataset/${instance.guid}">View details on GBIF.org</a></p>
                </g:if>

                <p>
                    <span class="category"><g:message code="dataresource.show.verificationStatus" default="Verification status"/>: </span>
                    <g:if test="${instance.isVerified()}">
                        Currently this data resource is marked as verified <i class="fa fa-check-circle tooltips" style="color:green;"></i>
                        <g:link class="btn btn-default btn-sm" controller="dataResource" action="markAsUnverified" params="[uid: instance.uid]">Mark as <strong>unverified</strong></g:link>
                    </g:if>
                    <g:else>
                        Currently this data resource is marked as unverified
                            <g:link class="btn btn-default btn-sm" controller="dataResource" action="markAsVerified" params="[uid: instance.uid]">Mark as <strong>verified</strong></g:link>
                    </g:else>
                </p>

                <cl:editButton uid="${instance.uid}" page="/shared/base" notAuthorisedMessage="You are not authorised to edit this resource."/>
              </div>

              <!-- description -->
              <div class="show-section well">
                <h2><g:message code="collection.show.title.description" /></h2>

                <!-- Pub Short Desc -->
                <span class="category"><g:message code="collection.show.pubShort"  default="Public short description"/></span><br/>
                <cl:formattedText body="${instance.pubShortDescription?:'Not provided'}"/>

                <!-- Pub Desc -->
                <span class="category"><g:message code="collection.show.span04" /></span><br/>
                <cl:formattedText body="${instance.pubDescription?:'Not provided'}"/>

                <!-- Tech Desc -->
                <span class="category"><g:message code="collection.show.span05" /></span><br/>
                <cl:formattedText body="${instance.techDescription?:'Not provided'}"/>

                <!-- Bounding box -->
                <span class="category"><g:message code="collection.show.boundingbox" default="Bounding box (decimal degrees WGS84)"/></span><br/>
                <p>
                    <ul>
                        <li><g:message code="collection.show.northBoundingCoordinate" default="North" />: ${instance.northBoundingCoordinate?:'Not provided'}</li>
                        <li><g:message code="collection.show.southBoundingCoordinate" default="South" />: ${instance.southBoundingCoordinate?:'Not provided'}</li>
                        <li><g:message code="collection.show.eastBoundingCoordinate" default="East" />: ${instance.eastBoundingCoordinate?:'Not provided'}</li>
                        <li><g:message code="collection.show.westBoundingCoordinate" default="West" />: ${instance.westBoundingCoordinate?:'Not provided'}</li>
                    </ul>
                </p>

                <!-- Temporal range -->
                <span class="category"><g:message code="collection.show.temporalrange" default="Temporal range"/></span><br/>
                <p>
                <ul>
                    <li><g:message code="collection.show.beginDate" default="Start date" />: ${instance.beginDate?:'Not provided'}</li>
                    <li><g:message code="collection.show.endDate" default="End date" />: ${instance.endDate?:'Not provided'}</li>
                </ul>
                </p>

                <!-- Data Quality -->
                <span class="category"><g:message code="collection.show.qualityControlDescription" /></span><br/>
                <cl:formattedText body="${instance.qualityControlDescription?:'Not provided'}"/>

                <!-- Methods -->
                <span class="category"><g:message code="collection.show.methodStepDescription" /></span><br/>
                <cl:formattedText body="${instance.methodStepDescription?:'Not provided'}"/>

                <!-- Focus / purpose -->
                <span class="category"><g:message code="providerGroup.purpose.label" default="Purpose"/></span><br/>
                <cl:formattedText>${instance.purpose?:'Not provided'}</cl:formattedText>

                <!-- generalisations -->
                <p><span class="category"><g:message code="dataresource.show.dg" />: </span> ${fieldValue(bean: instance, field: "dataGeneralizations")}</p>

                <!-- info withheld -->
                <p><span class="category"><g:message code="dataresource.show.iw" />: </span> ${fieldValue(bean: instance, field: "informationWithheld")}</p>

                <!-- content types -->
                <p><span class="category"><g:message code="dataResource.contentTypes.label" />: </span> <cl:formatJsonList value="${instance.contentTypes}"/></p>

                <!-- data collection protocol name -->
                <p><span class="category"><g:message code="dataResource.datacollectionprotocolname.label" />: </span>${instance.dataCollectionProtocolName}</p>

                <!-- data collection protocol documentation -->
                <p><span class="category"><g:message code="dataResource.datacollectionprotocoldoc.label" />: </span>${instance.dataCollectionProtocolDoc}</p>

                <!-- suitable for -->
                <g:set var="suitable" value="${instance.suitableFor != 'other' ? suitableFor.getOrDefault(instance.suitableFor, "") : (instance.suitableForOtherDetail ?: suitableFor.getOrDefault('other', message(code: "dataresource.suitablefor.other", default: "Other")))}"/>
                <p><span class="category"><g:message code="dataResource.suitablefor.label" />: </span> ${suitable}</p>

                <cl:editButton uid="${instance.uid}" page="description"/>
              </div>

              <!-- image metadata -->
              <div class="show-section well">
                <h2>Image metadata</h2>
                <p>These values the default values displayed for any images loaded for this data resource.</p>
                <cl:showImageMetadata imageMetadata="${instance.imageMetadata}"/>
                <cl:editButton uid="${instance.uid}" page="/dataResource/imageMetadata"/>
              </div>

              <!-- taxonomic range -->
              <div class="show-section well">
                <h2>Taxonomic range</h2>

                <!-- range -->
                <cl:taxonomicRangeDescription obj="${instance.taxonomyHints}" key="range"/>

                <cl:editButton uid="${instance.uid}" page="/shared/taxonomicRange"/>
              </div>

              <!-- mobilisation -->
              <div class="show-section well">
                <g:if test="${instance.gbifDataset}">
                    <cl:ifGranted role="${grailsApplication.config.ROLE_ADMIN}">
                        <div class="pull-right"><span class="buttons">
                            <g:link class="edit btn btn-default" controller="manage" action="gbifDatasetDownload" id="${instance.uid}">
                            <i class="glyphicon-refresh"> </i>
                            ${message(code: 'datasource.button.update', default: 'Reload from GBIF')}</g:link></span></div>
                    </cl:ifGranted>
                </g:if>

                <h2><g:message code="dataresource.show.title01" /></h2>

                <!-- contributor -->
                <p><span class="category"><g:message code="dataresource.show.ac" />: </span><cl:tickOrCross test="${instance.status == 'dataAvailable' || instance.status == 'linksAvailable'}">yes|no</cl:tickOrCross></p>

                <!-- status -->
                <p><span class="category"><g:message code="dataresource.show.status" />: </span> ${fieldValue(bean: instance, field: "status")}</p>

                <!-- provenance -->
                <p><span class="category"><g:message code="dataresource.show.provenance" />: </span> ${fieldValue(bean: instance, field: "provenance")}</p>

                <!-- last checked -->
                <p><span class="category"><g:message code="dataresource.show.lc" />: </span> ${fieldValue(bean: instance, field: "lastChecked")}</p>

                <!-- data currency -->
                <p><span class="category"><g:message code="dataresource.show.dc" />: </span> ${fieldValue(bean: instance, field: "dataCurrency")}</p>

                <!-- harvest frequency -->
                <p><span class="category"><g:message code="dataresource.show.hf" />: </span>
                    <g:if test="${instance.harvestFrequency}">
                        <g:message code="dataresource.show.ed" args="[instance.harvestFrequency]" />.</p>
                    </g:if>
                    <g:else><g:message code="dataresource.show.manual" /></g:else>

                <!-- mobilisation notes -->
                <p><span class="category"><g:message code="dataresource.show.mn" />: </span> ${fieldValue(bean: instance, field: "mobilisationNotes")}</p>

                <!-- harvesting notes -->
                <p><span class="category"><g:message code="dataresource.show.hn" />: </span> ${fieldValue(bean: instance, field: "harvestingNotes")}</p>

                <!-- public archive available -->
                <p><span class="category"><g:message code="dataresource.show.paa" />: </span><cl:tickOrCross test="${instance.publicArchiveAvailable}">yes|no</cl:tickOrCross></p>

                <!-- connection parameters -->
                <h3><g:message code="dataresource.show.title02" /></h3>

                <cl:showConnectionParameters connectionParameters="${instance.connectionParameters}"/></p>

                <g:if test="${instance.resourceType == 'records'}">
                    <!-- darwin core defaults -->
                    <g:set var="dwc" value="${instance.defaultDarwinCoreValues ? JSON.parse(instance.defaultDarwinCoreValues) : [:]}"/>
                    <h3>Default values for DwC fields</h3>
                        <g:if test="${!dwc}">none</g:if>
                        <table class="table">
                            <g:each in="${dwc.entrySet()}" var="dwct">
                                <tr><td>${dwct.key}:</td><td>${dwct.value?:'Not supplied'}</td></tr>
                            </g:each>
                        </table>

                </g:if>
                <cl:editButton uid="${instance.uid}" page="/dataResource/contribution"
                               notAuthorisedMessage="You are not authorised to edit this resource."/>
              </div>

              <div class="well">
                  <h3><g:message code="dataresource.show.title03" /></h3>
                  <g:link controller="dataResource" action="upload" class="btn btn-default" id="${instance.uid}"><i class="glyphicon glyphicon-upload"></i> <g:message code="dataresource.show.link.upload" /></g:link>
              </div>

              <!-- rights -->
              <div class="show-section well">
                <h2><g:message code="dataresource.show.title04" /></h2>

                <!-- citation -->
                <p><span class="category"><g:message code="dataResource.citation.label" />: </span> ${fieldValue(bean: instance, field: "citation")}</p>

                <!-- rights -->
                <p><span class="category"><g:message code="dataResource.rights.label" />: </span> ${fieldValue(bean: instance, field: "rights")}</p>

                <!-- license type-->
                <p><span class="category"><g:message code="dataResource.licenseType.label" />: </span> <cl:displayLicenseType type="${instance.licenseType}" version="${instance.licenseVersion}"/></p>

                <!-- license version -->
                <p><span class="category"><g:message code="dataResource.licenseVersion.label" />: </span> ${fieldValue(bean: instance, field: "licenseVersion")}</p>

                <!-- permissions document -->
                <p><span class="category"><g:message code="dataResource.permissionsDocument.label" />: </span>
                    <g:if test="${instance.permissionsDocument?.startsWith('http://') || instance.permissionsDocument?.startsWith('https://')}">
                        <g:link class="external_icon" target="_blank" url="${instance.permissionsDocument}">${fieldValue(bean: instance, field: "permissionsDocument")}</g:link>
                    </g:if>
                    <g:else>
                        ${fieldValue(bean: instance, field: "permissionsDocument")}
                    </g:else>
                </p>

                <!-- permissions document type -->
                <p><span class="category"><g:message code="dataResource.permissionsDocumentType.label" />: </span> ${fieldValue(bean: instance, field: "permissionsDocumentType")}</p>

                <!-- DPA flags -->
                <g:if test="${instance.permissionsDocumentType == 'Data Provider Agreement'}">
                    <p><span class="category"><g:message code="dataResource.riskAssessment.label" />: </span><cl:tickOrCross test="${instance.riskAssessment}">yes|no</cl:tickOrCross></p>
                    <p><span class="category"><g:message code="dataresource.show.documentfield.label" />: </span><cl:tickOrCross test="${instance.filed}">yes|no</cl:tickOrCross></p>
                </g:if>

                <!-- download limit -->
                <p><span class="category"><g:message code="dataResource.downloadLimit.label" />: </span> ${instance.downloadLimit ? fieldValue(bean:instance,field:'downloadLimit') : 'no limit'}</p>

                <cl:editButton uid="${instance.uid}" page="rights"/>
              </div>

              <!-- images -->
              <g:render template="/shared/images" model="[target: 'logoRef', image: instance.logoRef, title:'Logo', instance: instance]"/>
              <g:render template="/shared/images" model="[target: 'imageRef', image: instance.imageRef, title:'Representative image', instance: instance]"/>

              <!-- location -->
              <g:render template="/shared/location" model="[instance: instance]"/>

              <!-- Record consumers -->
              <g:if test="${instance.resourceType == 'records'}">
                  <g:render template="/shared/consumers" model="[instance: instance]"/>
              </g:if>

              <!-- Contacts -->
              <g:render template="/shared/contacts" model="[contacts: contacts, instance: instance]"/>

              <!-- Attributions -->
              <g:render template="/shared/attributions" model="[instance: instance]"/>

              <!-- taxonomy hints -->
              <g:render template="/shared/taxonomyHints" model="[instance: instance]"/>

              <!-- external identifiers -->
              <g:render template="/shared/externalIdentifiers" model="[instance: instance]"/>

              <g:if test="${grailsApplication.config.loggerURL}">
              <div class="well">
                <!-- Resources -->
                <h2>User download reports</h2>
                <p>
                        <a class="btn btn-default" href="${grailsApplication.config.loggerURL}/admin/userReport/download?fileName=user-report-${instance.uid}.csv&entityUids=${instance.uid}&eventId=1002">
                            <i class="glyphicon glyphicon-cloud-download"></i>  Download user report for this data resource
                        </a>
                 </p>
                 <p>
                        <a class="btn  btn-default" href="${grailsApplication.config.loggerURL}/admin/userReport/downloadDetailed?fileName=user-report-detailed-${instance.uid}.csv&entityUids=${instance.uid}&eventId=1002">
                            <i class="glyphicon glyphicon-cloud-download"></i> Download detailed user report for this data resource
                        </a>
                 </p>
              </div>
              </g:if>

              <!-- GBIF integration -->
              <div class="well">
                <h2><g:message code="dataresource.show.gbif.sync" default="GBIF synchronisation" /></h2>

                <g:set var="gbif" bean="gbifRegistryService"/>
                <cl:ifGranted role="${grailsApplication.config.gbifRegistrationRole}">
                    <g:if test="${instance.isShareableWithGBIF && gbif.getGBIFCompatibleLicence(instance.licenseType)}">
                        <div class="pull-right">
                            <g:if test="${!instance.gbifRegistryKey}">
                                <g:link class="btn btn-default" controller="dataResource" action="registerGBIF" id="${instance.id}">
                                    Register with GBIF
                                </g:link>


                            </g:if>
                            <g:else>
                                <g:link class="btn btn-default" controller="dataResource" action="updateGBIF" id="${instance.id}">
                                    Update GBIF
                                </g:link>
                                <g:link
                                        class="btn btn-default"
                                        controller="dataResource"
                                        action="deleteGBIF"
                                        id="${instance.id}"
                                        onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure you want to do this? This will remove the data and metadata from GBIFs public website.')}')"
                                >
                                    Delete from GBIF
                                </g:link>
                            </g:else>
                        </div>
                    </g:if>
                </cl:ifGranted>

                <p><span class="category">GBIF registry key:</span> ${instance.gbifRegistryKey ?: 'Not registered with GBIF'}</p>
                <g:if test="${instance.repatriationCountry}">
                    <p><span class="category">GBIF repatriation country:</span> ${instance.repatriationCountry}</p>
                </g:if>

                <p><span class="category">GBIF supplied dataset (i.e. downloaded via GBIF services): </span> ${instance.gbifDataset ? 'yes' : 'no'}</p>
                <p><span class="category">Should be shared with GBIF ? :</span>
                    <span class="${instance.isShareableWithGBIF ? '' : 'config-warning'}">
                        ${instance.isShareableWithGBIF ? 'yes' : 'no'}
                    </span>
                </p>

                <g:set var="gbifCompatible" value="${gbif.getGBIFCompatibleLicence(instance.licenseType)}"/>

                <p><span class="category">Has a GBIF compatible licence (current licence is ${instance.licenseType}) ? :</span>
                    <span class="${gbifCompatible ? '' : 'config-warning'}">${gbif.getGBIFCompatibleLicence(instance.licenseType) ? 'yes' : 'no'} </span>
                </p>

                <g:if test="${instance.gbifRegistryKey}">
                    <p>
                        <span class="category">GBIF Link:</span>
                        <a href="${grailsApplication.config.gbifWebsite}/dataset/${instance.gbifRegistryKey}">View details on GBIF.org</a>
                    </p>
                    <p>
                        <span class="category">GBIF webservices Link:</span>
                        <a href="${grailsApplication.config.gbifApiUrl}/dataset/${instance.gbifRegistryKey}">View details on GBIF.org</a>
                    </p>
                </g:if>

                <cl:editButton uid="${instance.uid}" page="gbif"/>

              </div>

              <!-- change history -->
              <g:render template="/shared/changes" model="[changes: changes, instance: instance]"/>

            </div>
            <div class="btn-toolbar">
                <g:form class="btn-group">
                    <g:hiddenField name="id" value="${instance?.id}"/>
                    <cl:ifGranted role="${grailsApplication.config.ROLE_ADMIN}">
                        <g:actionSubmit class="delete btn btn-danger" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');"/>
                    </cl:ifGranted>
                </g:form>
                <ul class="btn-group pull-right">
                    <li class="btn btn-default"><cl:viewPublicLink uid="${instance?.uid}"/></li>
                    <li class="btn btn-default"><cl:jsonSummaryLink uid="${instance.uid}"/></li>
                    <li class="btn btn-default"><cl:jsonDataLink uid="${instance.uid}"/></li>
                    <li class="btn btn-default"><cl:emlDataLink uid="${instance.uid}"/></li>
                </ul>
            </div>
        </div>
    </body>
</html>
