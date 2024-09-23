/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.bie

class UrlMappings {

    static mappings = {
        // Redirects for BIE web services URLs
        "/geo"(controller: "species", action: "geoSearch")
//        "/species"(view:"/species/index")
        "/species/$guid**"(controller: "species", action: "show")
        "/search"(controller: "species", action: "search")
        "/image-search"(controller: "species", action: "imageSearch")
        "/image-search/showSpecies"(controller: "species", action: "imageSearch")
        "/image-search/infoBox"(controller: "species", action: "infoBox")
        "/image-search/$id**"(controller: "species", action: "imageSearch")
        "/bhl-search"(controller: "species", action: "bhlSearch")
        "/sound-search"(controller: "species", action: "soundSearch")
        "/logout"(controller: "species", action: "logout")
        "/i18n/$catalogue"(controller: "i18nMessages", action: "i18n")
        "/search/auto.jso*"(controller:"externalSite", action: "proxyAutocomplete") // legacy URL
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/sitemap($idx)?.xml"(controller: "sitemap", action: "index")

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
