<%@ page contentType="text/html;charset=UTF-8" import="au.org.ala.collectory.DataResource; au.org.ala.collectory.Institution" %>
<g:set var="orgNameLong" value="${grailsApplication.config.skin.orgNameLong}"/>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="breadcrumbParent"
          content="${createLink(action: 'map', controller: 'public')},${message(code: 'breadcrumb.collections')}"
    />
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <title><g:fieldValue bean="${instance}" field="name"/></title>
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
          instanceUuid: "${instance.uid}",
          instanceName:"${instance.name}"
        }
        orgNameLong = "${orgNameLong}";
        biocacheServicesUrl = "${grailsApplication.config.biocacheServicesUrl}";
        biocacheWebappUrl = "${grailsApplication.config.biocacheUiURL}";
        loadLoggerStats = ${!grailsApplication.config.disableLoggerLinks.toBoolean()};
    </asset:script>
    <asset:javascript src="application-pages.js"/>
</head>

<body>
<div id="content">
    <div class="row">
            <div class="col-md-9">
                <cl:h1 value="${instance.name}"/>
                <g:render template="editButton"/>
                <g:set var="parents" value="${instance.listParents()}"/>
                <g:each var="p" in="${parents}">
                    <h2><g:link action="show" id="${p.uid}">${p.name}</g:link></h2>
                </g:each>

                <div class="tabbable">
                    <ul class="nav nav-tabs" id="home-tabs">
                        <li class="active"><a href="#basic-metadata" data-toggle="tab"><g:message code="show.tab.metadata" /></a></li>
                        <li><a href="#collections" data-toggle="tab"><g:message code="show.tab.collections" /></a></li>
                        <li><a href="#usage-stats" data-toggle="tab"><g:message code="show.tab.usage.stats" /></a></li>
                        <li><a href="#metrics" data-toggle="tab"><g:message code="show.tab.metrics" /></a></li>
                    </ul>
                </div>

                <div class="tab-content">

                    <div id="basic-metadata" class="active tab-pane">
                        <g:if test="${instance.pubDescription}">
                            <h2><g:message code="public.des" /></h2>
                            <cl:formattedText>${fieldValue(bean: instance, field: "pubDescription")}</cl:formattedText>
                            <cl:formattedText>${fieldValue(bean: instance, field: "techDescription")}</cl:formattedText>
                        </g:if>
                        <g:if test="${instance.focus}">
                            <h2><g:message code="public.si.content.label02.param" args="${[grailsApplication.config.skin.orgNameShort]}" /></h2>
                            <cl:formattedText>${fieldValue(bean: instance, field: "focus")}</cl:formattedText>
                        </g:if>

                        <div>
                            <p style="padding-bottom:8px;"><span id="numBiocacheRecords"><g:message code="public.numbrs.des01" /></span> <g:message code="public.numbrs.des02" args="[orgNameLong]"/>.</p>
                            <p><cl:recordsLink entity="${instance}"><g:message code="public.numbrs.link" /> ${instance.name}.</cl:recordsLink></p>
                        </div>

                        <cl:lastUpdated date="${instance.lastUpdated}"/>
                    </div>
                    <div id="collections" class="tab-pane">
                        <h2><g:message code="public.si.content.label03" /></h2>
                        <ol>
                            <g:each var="c" in="${instance.listCollections().sort { it.name }}">
                                <li><g:link controller="public" action="show"
                                            id="${c.uid}">${c?.name}</g:link> ${c?.makeAbstract(400)}</li>
                            </g:each>
                        </ol>
                    </div>

                    <div id="usage-stats" class="tab-pane">
                        <h2><g:message code="public.usagestats.label" /></h2>

                        <div id='usage'>
                            <p><g:message code="public.usage.des" />...</p>
                        </div>
                    </div>

                    <div id="metrics" class="tab-pane">
                        <h2><g:message code="public.si.content.label04" /></h2>


                        <div id="recordsBreakdown" class="section vertical-charts">
                            <div id="charts"></div>
                        </div>
                    </div>
                </div>

            </div><!--close section-->
            <section class="col-md-3">

                <g:if test="${fieldValue(bean: instance, field: 'logoRef') && fieldValue(bean: instance, field: 'logoRef.file')}">
                    <section class="public-metadata">
                        <img class="institutionImage" src='${resource(absolute:"true", dir:"data/institution/",file:instance.logoRef.file)}' />
                    </section>
                </g:if>

                <g:if test="${fieldValue(bean: instance, field: 'imageRef') && fieldValue(bean: instance, field: 'imageRef.file')}">
                    <section class="public-metadata">
                        <img alt="${fieldValue(bean: instance, field: "imageRef.file")}"
                             src="${resource(absolute: "true", dir: "data/institution/", file: instance.imageRef.file)}"/>
                        <cl:formattedText
                                pClass="caption">${fieldValue(bean: instance, field: "imageRef.caption")}</cl:formattedText>
                        <cl:valueOrOtherwise value="${instance.imageRef?.attribution}"><p
                                class="caption">${fieldValue(bean: instance, field: "imageRef.attribution")}</p></cl:valueOrOtherwise>
                        <cl:valueOrOtherwise value="${instance.imageRef?.copyright}"><p
                                class="caption">${fieldValue(bean: instance, field: "imageRef.copyright")}</p></cl:valueOrOtherwise>
                    </section>
                </g:if>

                <div id="dataAccessWrapper" style="display:none;">
                    <g:render template="dataAccess" model="[instance:instance]"/>
                </div>

                <section class="public-metadata">
                    <h4><g:message code="public.location" /></h4>
                    <g:if test="${instance.address != null && !instance.address.isEmpty()}">
                        <p>
                            <cl:valueOrOtherwise
                                    value="${instance.address?.street}">${instance.address?.street}<br/></cl:valueOrOtherwise>
                            <cl:valueOrOtherwise
                                    value="${instance.address?.city}">${instance.address?.city}<br/></cl:valueOrOtherwise>
                            <cl:valueOrOtherwise
                                    value="${instance.address?.state}">${instance.address?.state}</cl:valueOrOtherwise>
                            <cl:valueOrOtherwise
                                    value="${instance.address?.postcode}">${instance.address?.postcode}<br/></cl:valueOrOtherwise>
                            <cl:valueOrOtherwise
                                    value="${instance.address?.country}">${instance.address?.country}<br/></cl:valueOrOtherwise>
                        </p>
                    </g:if>
                    <g:if test="${instance.email}"><cl:emailLink>${fieldValue(bean: instance, field: "email")}</cl:emailLink><br/></g:if>
                    <cl:ifNotBlank value='${fieldValue(bean: instance, field: "phone")}'/>
                </section>

                <!-- contacts -->
                <g:render template="contacts" bean="${instance.getPublicContactsPrimaryFirst()}"/>

                <!-- web site -->
                <g:if test="${instance.websiteUrl}">
                    <section class="public-metadata">
                        <h4><g:message code="public.website" /></h4>

                        <div class="webSite">
                            <a class='external' target="_blank"
                               href="${instance.websiteUrl}"><g:message code="public.si.website.link.param"
                                                                        args="${[g.message(code:'public.si.website.link.param.'
                                                                                + cl.institutionType(inst:instance))]}"/></a>
                        </div>
                    </section>
                </g:if>

                <!-- network membership -->
                <g:if test="${instance.networkMembership}">
                    <section class="public-metadata">
                        <h4><g:message code="public.network.membership.label" /></h4>
                        <g:if test="${instance.isMemberOf('CHAEC')}">
                            <p><g:message code="public.network.membership.des01" /></p>
                            <img src="${resource(absolute: "true", dir: "data/network/", file: "chaec-logo.png")}"/>
                        </g:if>
                        <g:if test="${instance.isMemberOf('CHAH')}">
                            <p><g:message code="public.network.membership.des02" /></p>
                            <a target="_blank" href="http://www.chah.gov.au"><img style="padding-left:25px;"
                                                                                  src="${resource(absolute: "true", dir: "data/network/", file: "CHAH_logo_col_70px_white.gif")}"/>
                            </a>
                        </g:if>
                        <g:if test="${instance.isMemberOf('CHAFC')}">
                            <p><g:message code="public.network.membership.des03" /></p>
                            <img src="${resource(absolute: "true", dir: "data/network/", file: "CHAFC_sm.jpg")}"/>
                        </g:if>
                        <g:if test="${instance.isMemberOf('CHACM')}">
                            <p><g:message code="public.network.membership.des04" /></p>
                            <img src="${resource(absolute: "true", dir: "data/network/", file: "chacm.png")}"/>
                        </g:if>
                    </div>
                </g:if>

                <!-- external identifiers -->
                <g:render template="externalIdentifiers" model="[instance:instance]"/>
        </div>
    </div><!--close content-->
</div>
<asset:script>
    // stats
    if (loadLoggerStats){
      loadDownloadStats("${grailsApplication.config.loggerURL}", "${instance.uid}","${instance.name}", "1002");
    }
</asset:script>
<g:render template="charts" model="[facet:'institution_uid', instance: instance]" />
</body>
</html>
