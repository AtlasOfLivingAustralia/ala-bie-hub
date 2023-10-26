package au.org.ala.bie

class SitemapController {

    // proxy and cache the sitemap from bie-index
    def index(Integer idx) {
        if (!grailsApplication.config.sitemap.enabled) {
            response.status = 404
            return
        }

        if (idx == null) {
            // return sitemap index
            def text = new URL(grailsApplication.config.bie.index.url + "/sitemap.xml").text

            // replace bie-index's JSON path with the page address
            response.contentType = "application/xml"
            response.outputStream << text.replace(grailsApplication.config.bie.index.url, grailsApplication.config.grails.serverURL)
        } else {
            // return sitemap urls
            def text = new URL(grailsApplication.config.bie.index.url + "/sitemap${idx}.xml").text

            // replace bie-index's JSON path with the page address
            response.contentType = "application/xml"
            response.outputStream << text.replace(grailsApplication.config.bie.index.url, grailsApplication.config.grails.serverURL)
        }
    }
}
