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

import org.apache.commons.httpclient.util.URIUtil
import org.grails.web.json.JSONObject


class BiocacheService {

    def grailsApplication
    def webClientService

    /**
     * Retrieve the available sounds for this taxon.
     *
     * @param taxonName
     * @return
     */
    def getSoundsForTaxon(taxonID){
        JSONObject jsonObj = new JSONObject()
        if (!grailsApplication.config.biocacheService.baseURL)
            return jsonObj
        def queryUrl = grailsApplication.config.biocacheService.baseURL + "/occurrences/search?q=" + URIUtil.encodeWithinQuery("lsid:\"${taxonID}\"", "UTF-8") + "&fq=multimedia:Sound"

        log.debug "calling url = ${queryUrl}"
        def data = webClientService.getJson(queryUrl)

        if (data.size() && data.has("occurrences") && data.get("occurrences").size()) {
            def recordUrl = grailsApplication.config.biocacheService.baseURL + "/occurrences/" + data.get("occurrences").get(0).uuid
            jsonObj = webClientService.getJson(recordUrl)
        }

        jsonObj
    }

    /**
     * Enum for image categories
     */
    public enum ImageCategory {
        TYPE, SPECIMEN, OTHER
    }
}
