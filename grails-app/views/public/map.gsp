<%@ page import="au.org.ala.collectory.CollectionLocation" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="${grailsApplication.config.skin.layout}" />
        <title><g:message code="public.map3.title" /></title>
        <script type="text/javascript">
            var COLLECTORY_CONF = {
                contextPath: "${request.contextPath}",
                locale: "${(org.springframework.web.servlet.support.RequestContextUtils.getLocale(request).toString())?:request.locale}",
                cartodbPattern: "${grailsApplication.config.cartodb.pattern}"
            };
        </script>
        <asset:stylesheet src="application.css"/>
        <asset:javascript src="application-map.js"/>
    </head>
    <body id="page-collections-map" class="nav-datasets">
    <div id="content">
      <div id="header">
        <div class="section full-width">
          <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
          </g:if>
          <div class="hrgroup">
            <h1><g:message code="public.map3.header.title" args="[raw(grailsApplication.config.regionName)]"/></h1>
            <p><g:message code="public.map3.header.des01" /> ${grailsApplication.config.projectNameShort} <g:message code="public.map3.header.des02" args="[raw(grailsApplication.config.regionName)]"/>.</p>
          </div><!--close hrgroup-->
        </div><!--close section-->
      </div><!--close header-->

      <div class="row"><!-- wrap map and list-->
        <div class="col-md-4">
          <div class="section">
            <p><g:message code="public.map3.des01" />.</p>
          </div>
          <div class="section filter-buttons">
            <div class="all selected" id="all" onclick="toggleButton(this);return false;">
              <h2><a href=""><g:message code="public.map3.link.allcollections" /><span id="allButtonTotal"><g:message code="public.map3.link.showall" /> <collections></collections></span></a></h2>
            </div>
            <div class="fauna" id="fauna" onclick="toggleButton(this);return false;">
              <h2><a href=""><g:message code="public.map3.link.fauna" /><span><g:message code="public.map3.link.mammals" />.</span></a></h2>
            </div>
            <div class="insects" id="entomology" onclick="toggleButton(this);return false;">
              <h2><a href=""><g:message code="public.map3.link.insect" /><span><g:message code="public.map3.link.insects" />.</span></a></h2>
            </div>
            <div class="microbes" id="microbes" onclick="toggleButton(this);return false;">
              <h2><a href=""><g:message code="public.map3.link.mos" /><span><g:message code="public.map3.link.protists" />.</span></a></h2>
            </div>
            <div class="plants" id="plants" onclick="toggleButton(this);return false;">
              <h2><a href=""><g:message code="public.map3.link.plants" /><span><g:message code="public.map3.link.vascular" />.</span></a></h2>
            </div>
            <div class="fungi" id="fungi" onclick="toggleButton(this);return false;">
              <h2><a href=""><g:message code="public.map3.link.fungi" /></a></h2>
            </div>
          </div><!--close section-->
          <div id="collectionTypesFooter">
            <h4 class="collectionsCount"><span id='numFeatures'></span></h4>
            <h4 class="collectionsCount"><span id='numVisible'></span>
                <span id="numUnMappable"></span>
            </h4>
          </div>

          <div id="adminLink" class="dropdown" style="margin-top:110px;">
              <g:link controller="manage" action="list" style="color:#DDDDDD; margin-top:80px;">
                  <g:message code="public.map3.adminlink" />
              </g:link>
          </div>
        </div><!--close column-one-->

        <div class="col-md-8" id="map-list-col">
            <div class="tabbable">
                <ul class="nav nav-tabs" id="home-tabs">
                    <li class="active"><a href="#map" data-toggle="tab"><g:message code="public.map3.maplistcol.map" /></a></li>
                    <li><a href="#list" data-toggle="tab"><g:message code="public.map3.maplistcol.list" /></a></li>
                </ul>
            </div>
            <div class="tab-content">
              <div class="tab-pane active" id="map">
              <div  class="map-column">
                <div class="section">
                  <p style="width:100%;padding-bottom:8px;"><g:message code="public.map3.maplistcol.des01" />.</p>
                  <div id="map-container">
                    <div id="map_canvas"></div>
                  </div>
                </div><!--close section-->
              </div><!--close column-two-->
            </div><!--close map-->

            <div id="list" class="tab-pane">
              <div  class="list-column">
                <div class="nameList section" id="names">
                  <p><span id="numFilteredCollections"><g:message code="public.map3.maplistcol.des03" /></span>. <g:message code="public.map3.maplistcol.des04" /> <img style="vertical-align:middle" src="${resource(dir:'images/map', file: grailsApplication.config.skin.orgNameShort == "ALA" ? 'nomap.gif' : 'noworldmap.png')}"/>.</p>
                  <ul id="filtered-list" style="padding-left:15px;">
                    <g:each var="c" in="${collections}" status="i">
                      <li>
                        <g:link controller="public" action="show" id="${c.uid}">${fieldValue(bean: c, field: "name")}</g:link>
                        <g:if test="${!c.canBeMapped()}">
                          <img style="vertical-align:middle" src="${resource(dir:'images/map', file: grailsApplication.config.skin.orgNameShort == "ALA" ? 'nomap.gif' : 'noworldmap.png')}"/>
                        </g:if>
                      </li>
                    </g:each>
                  </ul>
                </div><!--close nameList-->
              </div><!--close column-one-->
            </div><!--close list-->
        </div><!-- /.tab-content -->
      </div><!--close map/list div-->
    </div><!--close content-->
    </div>
    <asset:script>
        var altMap = true;
        var COLLECTIONS_MAP_OPTIONS = {
          contextPath: "${grailsApplication.config.contextPath}",
          serverUrl:   "${grailsApplication.config.grails.serverURL}",
          centreLat:   ${grailsApplication.config.collectionsMap.centreMapLat?:'-28.2'},
          centreLon:   ${grailsApplication.config.collectionsMap.centreMapLon?:'134'},
          defaultZoom: ${grailsApplication.config.collectionsMap.defaultZoom?:'4'}
        }
        initMap(COLLECTIONS_MAP_OPTIONS);
    </asset:script>
  </body>
</html>