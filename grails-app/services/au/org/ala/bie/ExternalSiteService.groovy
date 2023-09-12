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

import au.org.ala.citation.BHLAdaptor
import grails.config.Config
import grails.converters.JSON
import grails.core.support.GrailsConfigurationAware
import grails.plugin.cache.Cacheable
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.grails.web.json.JSONObject
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector
import org.apache.tika.language.detect.LanguageDetector
import org.apache.tika.language.detect.LanguageResult
import org.jsoup.Jsoup
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory

import java.util.regex.Pattern

/**
 * Get information from external sites
 */
class ExternalSiteService implements GrailsConfigurationAware {
    /** Base URL of BHL web services */
    String bhlApiKey
    /** API Key to use when accessing BHL */
    String bhlApi
    /** The fixed BHL search page size */
    int bhlPageSize
    /** Extend BHL information with DOIs and citations */
    boolean bhlExtend
    /** The file containing elements to update */
    String updateFile
    /** Blacklist for external sites */
    Blacklist blacklist
    String ausTraitsBase
    String wikipediaUrl
    String wikipediaLang

    def webClientService

    @Override
    void setConfiguration(Config config) {
        bhlApi = config.getProperty("literature.bhl.api")
        bhlApiKey = config.getProperty("literature.bhl.apikey")
        bhlPageSize = config.getProperty("literature.bhl.pageSize", Integer)
        bhlExtend = config.getProperty("literature.bhl.extend", Boolean)
        updateFile = config.getProperty("update.file.location")
        def blacklistURL = config.getProperty("external.blacklist", URL)
        blacklist = blacklistURL ? Blacklist.read(blacklistURL) : null
        ausTraitsBase = config.getProperty("ausTraits.baseURL")
        wikipediaUrl = config.getProperty("wikipedia.url")
        wikipediaLang = config.getProperty("wikipedia.lang")
    }

    /**
     * Search the BHL for terms (PublicationSearch)
     *
     * @param search The terms to search for
     * @param start The start position
     * @param rows The number of rows
     * @param fulltext Do a full text search if true (very slow)
     *
     * @return A map containing
     */
    @Cacheable("bhlCache")
    def searchBhl(List<String> search, int start = 0, int rows = 10, boolean fulltext = false) {
        //https://www.biodiversitylibrary.org/docs/api3.html
        // searchtype - 'C' for a catalog-only search; 'F' for a catalog+full-text search
        def searchtype = fulltext ? 'F' : 'C'
        def page = (start / bhlPageSize) + 1 as Integer
        def from = start % bhlPageSize
        def max = 0
        def more = false
        def adaptor = new BHLAdaptor()
        def results = []
        if (bhlApiKey && bhlApi) {
            def searchTerms = URLEncoder.encode('"' + search.join('" OR "') + '"', 'UTF-8')
            def encodedKey = URLEncoder.encode(bhlApiKey, 'UTF-8')
            def url = "${bhlApi}?op=PublicationSearch&searchterm=${searchTerms}&searchtype=${searchtype}&page=${page}&apikey=${encodedKey}&format=json"
            def js = new JsonSlurper()
            try {
                def json = js.parse(new URL(url))
                if (!json.Status || json.Status != 'ok') {
                    log.warn "Unable to retrieve data for ${url}, status: ${json.Status}, error: ${json.ErrorMessage}"
                } else {
                    more = json.Result.size() == bhlPageSize
                    max = (page - 1) * bhlPageSize + json.Result.size()
                    def res = json.Result?.drop(from)?.take(rows)
                    res.each { result ->
                        def cite = adaptor.convert(result)
                        if (bhlExtend) {
                            def action = null
                            def id = null
                            switch (result.BHLType) {
                                case 'Item':
                                    action = 'GetItemMetadata'
                                    id = result.ItemID
                                    break
                                case 'Part':
                                    action = 'GetPartMetadata'
                                    id = result.PartID
                                    break
                                default:
                                    break
                            }
                            if (action && id) {
                                def murl = "${bhlApi}?op=${action}&id=${id}&&pages=f&names=f&apikey=${bhlApiKey}&format=json"
                                try {
                                    def mjson = js.parse(new URL(murl))
                                    if (mjson && mjson.Status == 'ok' && mjson.Result) {
                                        cite.DOI = mjson.Result[0].Doi
                                        cite.thumbnailUrl = mjson.Result[0].ItemThumbUrl
                                    }
                                } catch (Exception ex) {
                                    log.info "Error retrieving ${murl}: ${ex.message}"
                                }
                            }
                        }
                        results << cite
                    }
                }
            } catch (Exception ex) {
                log.warn "Error retrieving ${url}: ${ex.message}"
            }
        }
        return [start: start, rows: rows, search: search, max: max, more: more, results: results]
    }

    def getAusTraitsSummary(def params) {
        def url = ausTraitsBase + "/trait-summary?taxon=" + URLEncoder.encode(params.s, "UTF-8")
        url = handleAusTraitsAPNI(url, params)
        return fetchAusTraits(url)
    }

    def getAusTraitsCount(def params){
        String url = ausTraitsBase + "/trait-count?taxon=" + URLEncoder.encode(params.s, "UTF-8")
        url = handleAusTraitsAPNI(url, params)
        return fetchAusTraits(url)

    }

    def generateAusTraitsDownloadUrl(def params){
        String  url = ausTraitsBase + "/download-taxon-data?taxon=" + URLEncoder.encode(params.s, "UTF-8")
        url = handleAusTraitsAPNI(url, params)
        return url
    }

    def handleAusTraitsAPNI(String url, def params){
        if (params.guid.indexOf("apni") > 0) {
            url += "&APNI_ID=" + params.guid.split('/').last()
        }
        return url
    }

    @Cacheable("austraitsCache")
    def fetchAusTraits(String url) {
        def json = webClientService.getJson(url)
        // return a JSON with a simple error key if there is an error with fetching it.
        if (json instanceof JSONObject && json.has("error")) {
            log.warn "failed to get json, request error: " + json.error
            return JSON.parse("{'error': 'Error fetching content from source'}")
        }
        return json
    }

    @Cacheable("wikiCache")
    def searchWikipedia(String name) {
        if (blacklist && blacklist.isBlacklisted(name, null, null)) {
            return ''
        }

        String url = wikipediaUrl +  URLEncoder.encode(name.replace(' ', '_'), 'UTF-8')

        var header = ["Accept-Language": wikipediaLang]

        webClientService.get(url, false, header)
    }

}
