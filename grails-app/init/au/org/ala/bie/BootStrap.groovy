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

import au.org.ala.dataquality.api.DataProfilesApi
import au.org.ala.dataquality.api.QualityServiceRpcApi
import au.org.ala.dataquality.client.ApiClient
import au.org.ala.dataquality.model.QualityProfile
import retrofit2.Call

class BootStrap {
    def grailsApplication

    def init = { servletContext ->
        Object.metaClass.trimLength = { Integer stringLength ->

            String trimString = delegate?.toString()
            String concatenateString = "..."
            List separators = [".", " "]

            if (stringLength && (trimString?.length() > stringLength)) {
                trimString = trimString.substring(0, stringLength - concatenateString.length())
                String separator = separators.findAll { trimString.contains(it) }?.min { trimString.lastIndexOf(it) }
                if (separator) {
                    trimString = trimString.substring(0, trimString.lastIndexOf(separator))
                }
                trimString += concatenateString
            }
            return trimString
        }

        initConfig()
    }

    def destroy = {
    }

    void initConfig(){
        //Check if qualityProfile is used
        if (grailsApplication.config.getProperty("dataquality.enabled", Boolean, false)) {
            if (grailsApplication.config.dataquality.baseUrl) {
                QualityServiceRpcApi api
                DataProfilesApi dataProfilesApi
                def apiClient = new ApiClient()
                apiClient.adapterBuilder.baseUrl(grailsApplication.config.dataquality.baseUrl)

                api = apiClient.createService(QualityServiceRpcApi)
                def profile = (QualityProfile) responseOrThrow(api.activeProfile(null))
                if (profile) {
                    grailsApplication.config.with {
                        qualityProfile = profile.shortName
                    }
                    log.info("Quality Profile: " + grailsApplication.config.qualityProfile)
                }
            } else {
                log.warn("Data quality is enabled, but the url of data quality service is not defined!")
            }
        }
    }

    private <T> T responseOrThrow(Call<T> call) {
        def response
        try {
            response = call.execute()
        } catch (IOException e) {
            log.error("IOException executing call {}", call.request(), e)
        }
        if (response.successful) {
            return response.body()
        } else {
            log.error("Non-successful call {} returned response {}", call.request(), response)
        }
    }
}
