<%@ page contentType="text/html;charset=UTF-8" import="au.org.ala.collectory.DataProvider" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <meta name="breadcrumbParent"
          content="${createLink(action: 'datasets', controller: 'public')},${message(code: 'breadcrumb.dataproviders')}"
    />
    <title>${fieldValue(bean: instance, field: "name")}</title>
    <asset:stylesheet src="application.css"/>
    <script type="text/javascript">
        var COLLECTORY_CONF = {
            contextPath: "${request.contextPath}",
            locale: "${(org.springframework.web.servlet.support.RequestContextUtils.getLocale(request).toString())?:request.locale}",
            cartodbPattern: "${grailsApplication.config.cartodb.pattern}"
        };
        // define biocache server
        biocacheServicesUrl = "${grailsApplication.config.biocacheServicesUrl}";
        biocacheWebappUrl = "${grailsApplication.config.biocacheUiURL}";
        loadLoggerStats = ${!grailsApplication.config.disableLoggerLinks.toBoolean()};
    </script>
    <asset:javascript src="application-pages.js"/>
    <style type="text/css">
        .institutionImage { margin-bottom: 15px; }
    </style>
</head>
<body class="two-column-right">
<div id="content">
    <cl:pageOptionsPopup instance="${instance}"/>
    <div class="row">
        <div class="col-md-9">

            <cl:h1 value="${instance.name}"/>
            <g:render template="editButton"/>
            <cl:valueOrOtherwise value="${instance.acronym}"><span
                    class="acronym">Acronym: ${fieldValue(bean: instance, field: "acronym")}</span></cl:valueOrOtherwise>
            <g:if test="${instance.guid?.startsWith('urn:lsid:')}">
                <span class="lsid"><a href="#lsidText" id="lsid" class="local"
                                      title="Life Science Identifier (pop-up)"><g:message code="public.lsid" /></a></span>
            </g:if>

            <div class="tabbable">
                <ul class="nav nav-tabs" id="home-tabs">
                    <li class="active"><a href="#basic-metadata" data-toggle="tab"><g:message code="show.tab.metadata" /></a></li>
                    <li><a href="#data-resources" data-toggle="tab"><g:message code="show.tab.data.resources" /></a></li>
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
                        <h2><g:message code="public.sdp.content.label02.param"  args="${[grailsApplication.config.skin.orgNameShort]}" /></h2>
                        <cl:formattedText>${fieldValue(bean: instance, field: "focus")}</cl:formattedText>
                    </g:if>
                    <cl:lastUpdated date="${instance.lastUpdated}"/>
                </div>

                <div id="data-resources" class="tab-pane">
                    <h2><g:message code="public.sdp.content.label03" /></h2>
                    <g:set var="hasRecords" value="false"/>
                    <g:if test="${instance.getResources()}">
                    <ol>
                        <g:each var="c" in="${instance.getResources().sort { it.name }}">
                            <li><g:link controller="public" action="show" id="${c.uid}">${c?.name}</g:link>
                                <br/>
                                <span style="color:#555;">${c?.makeAbstract(400)}</span></li>
                            <g:if test="${c.resourceType == 'records'}">
                                <g:set var="hasRecords" value="true"/>
                            </g:if>
                        </g:each>
                    </ol>
                    </g:if>
                    <g:else>
                        <p><g:message code="public.sdp.content.noresources"/></p>
                    </g:else>
                </div>

                <g:if test="${hasRecords == 'true'}">
                    <div id="usage-stats" class="tab-pane">
                        <h2><g:message code="public.sdp.usagestats.label" /></h2>
                        <div id='usage'>
                            <p><g:message code="public.usage.des" />...</p>
                        </div>
                    </div>
                </g:if>

                <div id="metrics" class="tab-pane">
                    <div id="charts"> </div>
                </div>
            </div>

        </div><!--close column-one-->
        <div class="col-md-3">

            <!-- logo -->
            <g:if test="${fieldValue(bean: instance, field: 'logoRef') && fieldValue(bean: instance, field: 'logoRef.file')}">
                <section class="public-metadata">
                    <img class="institutionImage" src='${resource(absolute:"true", dir:"data/dataProvider/",file:instance.logoRef.file)}' />
                </section>
            </g:if>

            <div id="dataAccessWrapper" style="display:none;">
                <g:render template="dataAccess" model="[instance:instance]"/>
            </div>


        <g:if test="${fieldValue(bean: instance, field: 'imageRef') && fieldValue(bean: instance, field: 'imageRef.file')}">
                <section class="public-metadata">
                    <img class="entityLogo" alt="${fieldValue(bean: instance, field: "imageRef.file")}"
                         src="${resource(absolute: "true", dir: "data/" + instance.urlForm() + "/", file: instance.imageRef.file)}"/>
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
                        <a class='external_icon' target="_blank"
                           href="${instance.websiteUrl}"><g:message code="public.sdp.content.link01" /></a>
                    </div>
                </section>
            </g:if>

        <!-- network membership -->
            <g:if test="${instance.networkMembership}">
                <section class="public-metadata">
                    <h4><g:message code="public.network.membership.label" /></h4>
                    <g:if test="${instance.isMemberOf('CHAEC')}">
                        <p><g:message code="public.network.membership.des01" /></p>
                        <img src="${resource(absolute: "true", dir: "data/network/", file: "butflyyl.gif")}"/>
                    </g:if>
                    <g:if test="${instance.isMemberOf('CHAH')}">
                        <p><g:message code="public.network.membership.des02" /></p>
                        <a target="_blank" href="http://www.chah.gov.au"><img
                                src="${resource(absolute: "true", dir: "data/network/", file: "CHAH_logo_col_70px_white.gif")}"/>
                        </a>
                    </g:if>
                    <g:if test="${instance.isMemberOf('CHAFC')}">
                        <p><g:message code="public.network.membership.des03" /></p>
                    </g:if>
                    <g:if test="${instance.isMemberOf('CHACM')}">
                        <p><g:message code="public.network.membership.des04" /></p>
                        <img src="${resource(absolute: "true", dir: "data/network/", file: "chacm.png")}"/>
                    </g:if>
                </section>
            </g:if>

            <!-- external identifiers -->
            <g:render template="externalIdentifiers" model="[instance:instance]"/>
    </div>
    </div><!--close column-two-->
</div><!--close content-->
<asset:script>
  if (loadLoggerStats){
      loadDownloadStats("${grailsApplication.config.loggerURL}", "${instance.uid}","${instance.name}", "1002");
  }
</asset:script>
<g:render template="charts" model="[facet:'data_provider_uid', instance: instance]" />
</body>
</html>