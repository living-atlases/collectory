package collectory

class BootStrap {
     def messageSource
     def application

     def init = { servletContext ->
        messageSource.setBasenames(
                "file:///var/opt/atlas/i18n/collectory-plugin/messages",
                "file:///opt/atlas/i18n/collectory-plugin/messages",
                "WEB-INF/grails-app/i18n/messages",
                "classpath:messages",
                // In case we want to reuse biocache-service i18n messages:
                // "${application.config.biocache.baseUrl}/facets/i18n"
        )

    }
    def destroy = {
    }
}
