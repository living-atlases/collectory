<%@ page import="java.text.DecimalFormat; au.org.ala.collectory.Collection; au.org.ala.collectory.Institution" %>
<g:set var="orgNameLong" value="${grailsApplication.config.skin.orgNameLong}"/>
<g:set var="isPipelinesCompatible" value="${grailsApplication.config.getProperty("isPipelinesCompatible", Boolean.class)}"/>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="breadcrumbParent"
                  content="${createLink(action: 'map', controller: 'public')},${message(code: 'breadcrumb.collections')}"
        />
        <meta name="layout" content="${grailsApplication.config.skin.layout}" />
        <title>${fieldValue(bean: instance, field: "name")}</title>
        <asset:stylesheet src="application.css"/>
        <script type="text/javascript">
            var COLLECTORY_CONF = {
                contextPath: "${request.contextPath}",
                locale: "${(org.springframework.web.servlet.support.RequestContextUtils.getLocale(request).toString())?:request.locale}",
                cartodbPattern: "${grailsApplication.config.cartodb.pattern}"
            };
        </script>
        <asset:script>
          // Global var SHOW_REC to pass GSP data to external JS file
          var SHOW_REC = {
              orgNameLong: "${orgNameLong}",
              biocacheServicesUrl: "${grailsApplication.config.biocacheServicesUrl}",
              biocacheWebappUrl: "${grailsApplication.config.biocacheUiURL}",
              loggerServiceUrl: "${grailsApplication.config.loggerURL}",
              loadLoggerStats: ${!grailsApplication.config.disableLoggerLinks.toBoolean()},
              isPipelinesCompatible: ${isPipelinesCompatible},
              instanceUuid: "${instance.uid}",
              instanceName:"${instance.name}"
          }

          // Legacy global vars - keep for now
          orgNameLong = "${orgNameLong}";
          biocacheServicesUrl = "${grailsApplication.config.biocacheServicesUrl}";
          biocacheWebappUrl = "${grailsApplication.config.biocacheUiURL}";
          loadLoggerStats = ${!grailsApplication.config.disableLoggerLinks.toBoolean()};
        </asset:script>
        <asset:javascript src="application-pages.js"/>
    </head>
    <body>
        <div id="header">
          <cl:pageOptionsPopup instance="${instance}"/>
        </div>
        <div class="row">
            <div class="col-md-9">
              <cl:h1 value="${instance.name}"/>
              <g:render template="editButton"/>
              <g:set var="inst" value="${instance.getInstitution()}"/>
              <g:if test="${inst}">
                <h3><g:link action="show" id="${inst.uid}">${inst.name}</g:link></h3>
              </g:if>
              <span style="display:none;">
                <cl:valueOrOtherwise value="${instance.acronym}"><span class="acronym"><g:message code="public.show.header.acronym" />: ${fieldValue(bean: instance, field: "acronym")}</span></cl:valueOrOtherwise>
                  <span class="lsid"><a href="#lsidText" id="lsid" class="local" title="Life Science Identifier (pop-up)"><g:message code="public.lsid" /></a></span>
              </span>
              <div style="display:none; text-align: left;">
                  <div id="lsidText" style="text-align: left;">
                      <b><a class="external_icon" href="https://wayback.archive.org/web/20100515104710/http://lsids.sourceforge.net:80/" target="_blank"><g:message code="public.lsidtext.link" />:</a></b>
                      <p style="margin: 10px 0;"><cl:guid target="_blank" guid='${fieldValue(bean: instance, field: "guid")}'/></p>
                      <p style="font-size: 12px;"><g:message code="public.lsidtext.des" />. </p>
                  </div>
              </div>
                <div class="tabbable">
                    <ul class="nav nav-tabs" id="overviewTabs">
                        <li class="active"><a id="tab1" href="#basic-metadata" data-toggle="tab"><g:message code="public.show.overviewtabs.overview" /></a></li>
                        <li><a href="#usage-stats" data-toggle="tab"><g:message code="show.tab.usage.stats" /></a></li>
                        <li><a id="tab2" href="#metrics" data-toggle="tab"><g:message code="public.show.overviewtabs.records" /></a></li>
                        <li id="imagesTabEl" style="display:none;"><a id="tab3" href="#imagesTab" data-toggle="tab"><g:message code="public.show.overviewtabs.images" /></a></li>
                    </ul>
                </div>
                <div class="tab-content">
                    <div id="basic-metadata" class="tab-pane active">
                        <div id="overview-content" >
                            <h2><g:message code="public.des" /></h2>
                            <cl:formattedText body="${instance.pubDescription}"/>
                            <cl:formattedText>${fieldValue(bean: instance, field: "techDescription")}</cl:formattedText>
                            <g:if test="${instance.startDate || instance.endDate}">
                                <p><cl:temporalSpanText start='${fieldValue(bean: instance, field: "startDate")}' end='${fieldValue(bean: instance, field: "endDate")}'/></p>
                            </g:if>

                            <h2><g:message code="public.show.oc.label02" /></h2>
                            <g:if test="${fieldValue(bean: instance, field: 'focus')}">
                                <cl:formattedText>${fieldValue(bean: instance, field: "focus")}</cl:formattedText>
                            </g:if>
                            <g:if test="${fieldValue(bean: instance, field: 'kingdomCoverage')}">
                                <p><g:message code="public.show.oc.des01" />: <cl:concatenateStrings values='${fieldValue(bean: instance, field: "kingdomCoverage")}'/>.</p>
                            </g:if>
                            <g:if test="${fieldValue(bean: instance, field: 'scientificNames')}">
                                <p><cl:collectionName name="${instance.name}" prefix="${g.message(code:'collection.tag.lib.collection.prefix')}"/> <g:message code="public.show.oc.des02" />:<br/>
                                    <cl:JSONListAsStrings json='${instance.scientificNames}'/>.</p>
                            </g:if>

                            <g:if test="${instance?.geographicDescription || instance.states}">
                                <h2><g:message code="public.show.oc.label03" /></h2>
                                <g:if test="${fieldValue(bean: instance, field: 'geographicDescription')}">
                                    <p>${fieldValue(bean: instance, field: "geographicDescription")}</p>
                                </g:if>
                                <g:if test="${fieldValue(bean: instance, field: 'states')}">
                                    <p><cl:stateCoverage states='${fieldValue(bean: instance, field: "states")}'/></p>
                                </g:if>
                                <g:if test="${instance.westCoordinate != -1}">
                                    <p><g:message code="public.show.oc.des03" />: <cl:showDecimal value='${instance.westCoordinate}' degree='true'/></p>
                                </g:if>
                                <g:if test="${instance.eastCoordinate != -1}">
                                    <p><g:message code="public.show.oc.des04" />: <cl:showDecimal value='${instance.eastCoordinate}' degree='true'/></p>
                                </g:if>
                                <g:if test="${instance.northCoordinate != -1}">
                                    <p><g:message code="public.show.oc.des05" />: <cl:showDecimal value='${instance.northCoordinate}' degree='true'/></p>
                                </g:if>
                                <g:if test="${instance.southCoordinate != -1}">
                                    <p><g:message code="public.show.oc.des06" />: <cl:showDecimal value='${instance.southCoordinate}' degree='true'/></p>
                                </g:if>
                            </g:if>

                            <g:set var="nouns" value="${cl.nounForTypes(types:instance.listCollectionTypes())}"/>
                            <h2><g:message code="public.show.oc.label04" /> <cl:nounForTypes types="${instance.listCollectionTypes()}"/> <g:message code="public.show.oc.label05" /></h2>
                            <g:if test="${fieldValue(bean: instance, field: 'numRecords') != '-1'}">
                                <p><g:message code="public.show.oc.des07" /> ${nouns} <g:message code="public.show.oc.des07.in" /> <cl:collectionName prefix="${g.message(code:'collection.tag.lib.collection.prefix.lowercase')}" name="${instance.name}"/> <g:message code="public.show.oc.des08" /> ${fieldValue(bean: instance, field: "numRecords")}.</p>
                            </g:if>
                            <g:if test="${fieldValue(bean: instance, field: 'numRecordsDigitised') != '-1'}">
                                <p><g:message code="public.show.oc.des09" /> ${fieldValue(bean: instance, field: "numRecordsDigitised")} <g:message code="public.show.oc.des10" />.
                                <g:message code="public.show.oc.des11" /> <cl:percentIfKnown dividend='${instance.numRecordsDigitised}' divisor='${instance.numRecords}' /> <g:message code="public.show.oc.des12" />.</p>
                            </g:if>
                            <p><g:message code="public.show.oc.des13" />.</p>

                            <g:if test="${instance.listSubCollections()?.size() > 0}">
                                <h2><g:message code="public.show.oc.label06" /></h2>
                                <p><cl:collectionName prefix="${g.message(code:"collection.tag.lib.collection.prefix")}" name="${instance.name}"/> <g:message code="public.show.oc.des14" />:</p>
                                <cl:subCollectionList list="${instance.subCollections}"/>
                            </g:if>

                            <cl:lastUpdated date="${instance.lastUpdated}"/>
                        </div>
                    </div>
                    <div id="usage-stats" class="tab-pane">
                        <g:if test="${!grailsApplication.config.disableLoggerLinks.toBoolean()}">
                            <div id='usage'></div>
                        </g:if>
                    </div>
                    <div id="metrics" class="tab-pane">
                        <div class="row">
                            <div class="col-md-8">
                                <h2><g:message code="public.show.rt.title" /></h2>
                                <g:if test="${instance.numRecords != -1}">
                                    <p><cl:collectionName prefix="${g.message(code:'collection.tag.lib.collection.prefix')}" name="${instance.name}"/> <g:message code="public.show.collection.has.an.estimated" args="[fieldValue(bean: instance, field: "numRecords"), nouns]" />.
                                        <g:if test="${instance.numRecordsDigitised != -1}">
                                            <br/><g:message code="public.show.rt.des01" /> <cl:percentIfKnown dividend='${instance.numRecordsDigitised}' divisor='${instance.numRecords}'/> <g:message code="public.show.rt.des02" /> (${fieldValue(bean: instance, field: "numRecordsDigitised")} <g:message code="public.show.rt.des03" />).
                                        </g:if>
                                    </p>
                                </g:if>
                                <p>
                                    <span id="numBiocacheRecords"><g:message code="public.show.rt.des04" /></span>
                                    <g:message code="public.show.rt.des05" args="[orgNameLong]"/>
                                    <cl:warnIfInexactMapping collection="${instance}"/><br/>
                                    <cl:recordsLink entity="${instance}"><g:message code="public.show.rt.des06" /> <cl:collectionName name="${instance.name}"/></cl:recordsLink>
                                </p>
                            </div>
                            <div class="col-md-4">
                                <div id="progress" class="well">
                                    <div class="progress">
                                        <div id="progressBar" class="progress-bar progress-bar-success" style="width: 0%;"></div>
                                    </div>
                                    <p class="caption"><span id="speedoCaption"><g:message code="public.show.setprogress.02" args="${[grailsApplication.config.skin.orgNameShort]}" />.</span></p>
                                </div>
                            </div>

                        </div>
                        <div id="charts"></div>
                    </div>
                    <div id="imagesTab" class="tab-pane">
                        <style type="text/css">
                        #imagesList { margin:0; }
                        #imagesList .imgCon { display: inline-block;
                            margin-right: 8px;
                            text-align: center;
                            line-height: 1.3em;
                            background-color: #DDD;
                            color: #DDD;
                            padding: 5px;
                            margin-bottom: 8px;
                        }
                        #imagesList .imgCon img { max-height:150px; }
                        </style>
                        <h2><g:message code="public.show.it.title"/></h2>
                        <div id="imagesSpiel"></div>
                        <div id="imagesList"></div>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
          <!-- institution logo -->
              <g:if test="${inst?.logoRef?.file}">
                  <section class="public-metadata">
                      <g:link action="showInstitution" id="${inst.id}">
                          <img class="institutionImage" src='${resource(absolute:"true", dir:"data/institution/",file:inst.logoRef.file)}' />
                      </g:link>
                  </section>
              </g:if>
              <g:if test="${fieldValue(bean: instance, field: 'imageRef') && fieldValue(bean: instance, field: 'imageRef.file')}">
                  <section class="public-metadata">
                      <img style="max-width:100%;max-height:350px;" alt="${fieldValue(bean: instance, field: "imageRef.file")}"
                           src="${resource(absolute:"true", dir:"data/collection/", file:instance.imageRef.file)}" />
                      <cl:formattedText pClass="caption">${fieldValue(bean: instance, field: "imageRef.caption")}</cl:formattedText>
                      <cl:valueOrOtherwise value="${instance.imageRef?.attribution}"><p class="caption">${fieldValue(bean: instance, field: "imageRef.attribution")}</p></cl:valueOrOtherwise>
                      <cl:valueOrOtherwise value="${instance.imageRef?.copyright}"><p class="caption">${fieldValue(bean: instance, field: "imageRef.copyright")}</p></cl:valueOrOtherwise>
                  </section>
              </g:if>

              <div id="dataAccessWrapper" style="display:none;">
                  <g:render template="dataAccess" model="[instance:instance]"/>
              </div>

              <section class="public-metadata">
                  <h4><g:message code="public.location" /></h4>
                  <g:set var="address" value="${instance.address}"/>
                  <g:if test="${address == null || address.isEmpty()}">
                      <g:if test="${instance.getInstitution()}">
                          <g:set var="address" value="${instance.getInstitution().address}"/>
                      </g:if>
                  </g:if>

                  <g:if test="${!address?.isEmpty()}">
                      <p>
                          <cl:valueOrOtherwise value="${address?.street}">${address?.street}<br/></cl:valueOrOtherwise>
                          <cl:valueOrOtherwise value="${address?.city}">${address?.city}<br/></cl:valueOrOtherwise>
                          <cl:valueOrOtherwise value="${address?.state}">${address?.state}</cl:valueOrOtherwise>
                          <cl:valueOrOtherwise value="${address?.postcode}">${address?.postcode}<br/></cl:valueOrOtherwise>
                          <cl:valueOrOtherwise value="${address?.country}">${address?.country}<br/></cl:valueOrOtherwise>
                      </p>
                  </g:if>

                  <g:if test="${instance.email}"><cl:emailLink>${fieldValue(bean: instance, field: "email")}</cl:emailLink><br/></g:if>
                  <cl:ifNotBlank value='${fieldValue(bean: instance, field: "phone")}'/>
              </section>

          <!-- contacts -->
              <g:render template="contacts" bean="${instance.getPublicContactsPrimaryFirst()}"/>

          <!-- web site -->
              <g:if test="${instance.websiteUrl || instance.institution?.websiteUrl}">
                  <section class="public-metadata">
                      <h4><g:message code="public.website" /></h4>
                      <g:if test="${instance.websiteUrl}">
                          <div class="webSite">
                              <a class='external' rel='nofollow' target="_blank" href="${instance.websiteUrl}"><g:message code="public.show.osb.link01" /></a>
                          </div>
                      </g:if>
                      <g:if test="${instance.institution?.websiteUrl}">
                          <div class="webSite">
                              <a class='external' rel='nofollow' target="_blank" href="${instance.institution?.websiteUrl}">
                                  <g:message code="public.si.website.link.param" args="${[g.message(code:'public.si.website.link.param.'
                                                     + cl.institutionType(inst:instance.institution))]}"/></a>
                          </div>
                      </g:if>
                  </section>
              </g:if>

          <!-- network membership -->
              <g:if test="${instance.networkMembership}">
                  <section class="public-metadata">
                      <h4><g:message code="public.network.membership.label" /></h4>
                      <g:if test="${instance.isMemberOf('CHAEC')}">
                          <p><g:message code="public.network.membership.des01" /></p>
                          <img src="${resource(absolute:"true", dir:"data/network/",file:"chaec-logo.png")}"/>
                      </g:if>
                      <g:if test="${instance.isMemberOf('CHAH')}">
                          <p><g:message code="public.network.membership.des02" /></p>
                          <a target="_blank" href="http://www.chah.gov.au"><img src="${resource(absolute:"true", dir:"data/network/",file:"CHAH_logo_col_70px_white.gif")}"/></a>
                      </g:if>
                      <g:if test="${instance.isMemberOf('CHAFC')}">
                          <p><g:message code="public.network.membership.des03" /></p>
                          <img src="${resource(absolute:"true", dir:"data/network/",file:"chafc.png")}"/>
                      </g:if>
                      <g:if test="${instance.isMemberOf('CHACM')}">
                          <p><g:message code="public.network.membership.des04" /></p>
                          <img src="${resource(absolute:"true", dir:"data/network/",file:"chacm.png")}"/>
                      </g:if>
                  </section>
              </g:if>

          <!-- attribution -->
              <g:set var='attribs' value='${instance.getCollectionAttributionList()}'/>
              <g:if test="${attribs.size() > 0}">
                  <section class="public-metadata" id="infoSourceList">
                      <h4><g:message code="public.show.osb.label04" /></h4>
                      <ul>
                          <g:each var="a" in="${attribs}">
                              <g:if test="${a.url}">
                                  <li><cl:wrappedLink href="${a.url}">${a.name}</cl:wrappedLink></li>
                              </g:if>
                              <g:else>
                                  <li>${a.name}</li>
                              </g:else>
                          </g:each>
                      </ul>
                  </section>
              </g:if>

          <!-- external identifiers -->
              <g:render template="externalIdentifiers" model="[instance:instance]"/>
          </div>
        </div>
        <asset:script>
            // stats
            if (loadLoggerStats){
              loadDownloadStats("${grailsApplication.config.loggerURL}", "${instance.uid}","${instance.name}", "1002");
            }
            loadImagesTab();
        </asset:script>
        <g:render template="charts" model="[facet: isPipelinesCompatible ? 'collectionUid': 'collection_uid', instance: instance]" />
        <g:render template="progress" model="[facet: isPipelinesCompatible ? 'collectionUid':'collection_uid', instance: instance]" />
    </body>
</html>
