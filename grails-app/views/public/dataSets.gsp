<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <title><g:message code="public.datasets.title" /></title>
      <script type="text/javascript">
          var COLLECTORY_CONF = {
              contextPath: "${request.contextPath}",
              locale: "${(org.springframework.web.servlet.support.RequestContextUtils.getLocale(request).toString())?:request.locale}",
              cartodbPattern: "${grailsApplication.config.cartodb.pattern}"
          };
          var CHARTS_CONFIG = {
              biocacheServicesUrl: "https://biocache-ws.ala.org.au/ws",
              biocacheWebappUrl: "https://biocache.ala.org.au",
              bieWebappUrl: "",
              collectionsUrl: "https://collections.ala.org.au"
          };
      </script>
      <asset:stylesheet src="application.css"/>
      <asset:javascript src="application.js"/>
  </head>

  <body>
        <div id="header">
        <div class="full-width">
          <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
          </g:if>
          <div>
            <h1><g:message code="public.datasets.header.title.param" args="${[grailsApplication.config.projectName]}" /></h1>
            <p><g:message code="public.datasets.header.message.param" args="${[grailsApplication.config.projectName]}" /></p>
          </div><!--close hrgroup-->
        </div><!--close section-->
      </div><!--close header-->

      <noscript>
          <div class="noscriptmsg">
            <g:message code="public.datasets.noscript.message01" />.
          </div>
      </noscript>

      <div class="collectory-content row">
          <div id="sidebarBox" class="col-md-3 facets well well-small">
            <div class="sidebar-header">
              <h3><g:message code="public.datasets.sidebar.header" /></h3>
            </div>
            <div id="currentFilterHolder">
            </div>
            <div id="dsFacets">
            </div>
          </div>

          <div id="data-set-list" class="col-md-9">
            <div class="well">
                <div class="row">
                    <form class="form-inline">
                        <div class="col-md-12">
                            <div>
                                <span id="resultsReturned"><g:message code="public.datasets.resultsreturned.message01" /> <strong></strong>&nbsp;<g:message code="public.datasets.resultsreturned.message02" />.</span>
                            </div>
                            <div class="input-group col-lg-8">
                                <input type="text" name="dr-search" id="dr-search" class="form-control" />
                                <span class="input-group-btn">
                                    <button href="javascript:void(0);" title="${message(code:"public.datasets.search.btn.title")}"
                                            id="dr-search-link" class="btn btn-default">
                                        <g:message code="public.datasets.drsearch.search" />
                                    </button>
                                </span>
                            </div><!-- /input-group -->

                            <div class="pull-right">
                                <button href="javascript:reset()" title="${message(code:"datasets.remove.all.filters")}" class=" form-control btn btn-default">
                                    <g:message code="public.datasets.drsearch.resetlist" />
                                </button>
                                <button href="#" id="downloadLink" class="btn btn-default"
                                        title="Download metadata for datasets as a CSV file">
                                    <span class="glyphicon glyphicon-download"></span>
                                    <g:message code="public.datasets.downloadlink.label" />
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
            <div>
                <div id="searchControls">
                  <div id="sortWidgets" class="row">
                      <div class="col-md-4 form-inline">
                          <label for="per-page"><g:message code="public.datasets.sortwidgets.rpp" /></label>
                          <g:select class="form-control input-sm" id="per-page" name="per-page" from="${[10,20,50,100,500]}" value="${pageSize ?: 20}"/>
                      </div>
                      <div class="col-md-4 form-inline">
                          <label for="sort"><g:message code="public.datasets.sortwidgets.sb" /></label>
                          <g:select class="form-control  input-sm" id="sort" name="sort" valueMessagePrefix="datasets.sort" from="${['name','type','license']}"/>
                      </div>
                      <div class="col-md-4 form-inline">
                          <label for="dir"><g:message code="public.datasets.sortwidgets.so" /></label>
                          <g:select class="form-control  input-sm" id="dir" name="dir" valueMessagePrefix="datasets.sort" from="${['ascending','descending']}"/>
                      </div>
                  </div>
                </div><!--drop downs-->
            </div>

            <div id="results">
              <div id="loading"><g:message code="public.datasets.loading" /> ..</div>
            </div>

            <div id="searchNavBar" class="clearfix">
              <div id="navLinks" class="nav"></div>
            </div>
          </div>

    </div><!-- close collectory-content-->

  <asset:script>
      var altMap = true;
      $(document).ready(function() {
          loadResources("${grailsApplication.config.grails.serverURL}","${grailsApplication.config.biocacheUiURL}");
          $('select#per-page').change(onPageSizeChange);
          $('select#sort').change(onSortChange);
          $('select#dir').change(onDirChange);
      });
  </asset:script>
  </body>

</html>