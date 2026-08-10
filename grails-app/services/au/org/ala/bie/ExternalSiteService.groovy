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
    String wikipediaApi
    String wikipediaLang
    String wikipediaSnippetPattern
    int wikipediaSearchLimit

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
        wikipediaApi = config.getProperty("wikipedia.api")
        wikipediaLang = config.getProperty("wikipedia.lang")
        wikipediaSnippetPattern = config.getProperty("wikipedia.snippetPattern", "(?i)species|genus|family|order|class|phylum|kingdom|australia|endemic")
        wikipediaSearchLimit = config.getProperty("wikipedia.searchLimit", Integer, 20)
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

    /**
     * Search Wikipedia for a taxon page matching the supplied name.
     *
     * Uses the MediaWiki search API with an intitle: query, filters results to those whose
     * snippet mentions a taxonomic rank, then fetches and validates the first candidate that
     * looks like a taxon article and, if a kingdom is provided, belongs to that kingdom.
     *
     * If a kingdom is supplied and no candidate matches, the search will also look at the
     * base-name disambiguation page to find homonym pages that may belong to the requested
     * kingdom (for example, "Chara (moth)" for kingdom Animalia).
     *
     * @param name The taxon name to search for
     * @param kingdom Optional ALA kingdom to use as a homonym check
     * @return A map with keys {@code title} (the selected Wikipedia page title) and
     *          {@code html} (the page HTML). When nothing suitable is found, returns
     *          {@code [title: null, html: '']}.
     */
    @Cacheable("wikiCache")
    def searchWikipedia(String name, String kingdom = '') {
        if (blacklist && blacklist.isBlacklisted(name, null, null)) {
            return [title: null, html: '']
        }

        if (!wikipediaApi) {
            log.warn "wikipedia.api not configured, cannot search Wikipedia for ${name}"
            return [title: null, html: '']
        }

        Pattern snippetPattern = Pattern.compile(wikipediaSnippetPattern)
        String expectedKingdom = kingdom ? normaliseKingdom(kingdom) : ''

        // Try the supplied name first, then a version with any subgenus parenthetical removed.
        List<String> searchNames = buildSearchNames(name)
        for (String searchName : searchNames) {
            def candidates = searchWikipediaCandidates(searchName, snippetPattern)
            def result = evaluateCandidates(candidates, name, expectedKingdom)
            if (result?.html) {
                return result
            }
            // If search produced no usable candidates, the exact page URL may still resolve
            // via a redirect (e.g. binomial -> common-name article). Try it before moving on.
            result = tryExactPage(searchName, expectedKingdom)
            if (result?.html) {
                return result
            }
        }

        if (expectedKingdom) {
            log.debug "No matching taxon candidate for ${name}; checking disambiguation page for kingdom ${kingdom}"
            for (String searchName : searchNames) {
                def homonymCandidates = findDisambiguationHomonyms(searchName)
                def result = evaluateCandidates(homonymCandidates, name, expectedKingdom)
                if (result?.html) {
                    return result
                }
            }
        }

        log.debug "No Wikipedia candidates for ${name} passed taxon validation"
        return [title: null, html: '']
    }

    /**
     * Build the list of names to search for. Wikipedia pages for species with subgenera
     * are almost always titled using the plain binomial, so if the supplied name contains
     * a subgenus, also search without it. Non-alphanumeric characters and extra whitespace
     * are normalised so the title matches the way Wikipedia indexes pages.
     */
    private List<String> buildSearchNames(String name) {
        def names = []
        // Strip subgenus parentheses and normalise whitespace. Use a Java-style string
        // literal for the regex so Groovy 3.x compiles it as \s*\([^)]*\).
        String withoutSubgenus = name.replaceAll('\\s*\\([^)]*\\)', '').trim().replaceAll('\\s+', ' ')
        if (withoutSubgenus && withoutSubgenus != name) {
            names << withoutSubgenus
        }
        names << name
        return names
    }

    /**
     * Try opening a Wikipedia page for the exact (possibly normalised) name. This is used as a
     * last-resort fallback for names that MediaWiki search does not index in title form, such as
     * a plain binomial that redirects to a common-name article.
     */
    private Map tryExactPage(String name, String expectedKingdom) {
        try {
            String title = name.replace(' ', '_')
            String pageUrl = wikipediaUrl + URLEncoder.encode(title, 'UTF-8')
            String html = webClientService.get(pageUrl, false, ["Accept-Language": wikipediaLang])
            if (html && isTaxonArticle(html)) {
                if (!expectedKingdom || pageMatchesKingdom(html, expectedKingdom)) {
                    return [title: title, html: html]
                }
            }
        } catch (Exception ex) {
            log.debug "Exact page fallback for ${name} failed: ${ex.message}"
        }
        return [title: null, html: '']
    }

    /**
     * Fetch and validate each candidate page, returning the first one that is a taxon article
     * matching the expected kingdom.
     */
    private Map evaluateCandidates(List<String> candidates, String name, String expectedKingdom) {
        if (!candidates) {
            return [title: null, html: '']
        }
        def header = ["Accept-Language": wikipediaLang]
        for (String title : candidates) {
            String pageUrl = wikipediaUrl + URLEncoder.encode(title, 'UTF-8')
            try {
                String html = webClientService.get(pageUrl, false, header)
                if (html && isTaxonArticle(html)) {
                    if (!expectedKingdom || pageMatchesKingdom(html, expectedKingdom)) {
                        return [title: title, html: html]
                    }
                    log.debug "Wikipedia candidate ${title} for ${name} does not match kingdom ${expectedKingdom}"
                } else {
                    log.debug "Wikipedia candidate ${title} for ${name} failed taxon validation"
                }
            } catch (Exception ex) {
                log.warn "Error retrieving Wikipedia page ${pageUrl}: ${ex.message}"
            }
        }
        return [title: null, html: '']
    }

    /**
     * Retrieve the base-name disambiguation page and extract candidate homonym page titles
     * whose link text or title matches the original name. These are typically entries like
     * "Chara (alga)" or "Chara (moth)".
     */
    private List<String> findDisambiguationHomonyms(String name) {
        def candidates = []
        try {
            String pageUrl = wikipediaUrl + URLEncoder.encode(name.replace(' ', '_'), 'UTF-8')
            String html = webClientService.get(pageUrl, false, ["Accept-Language": wikipediaLang])
            if (!html) {
                return candidates
            }
            def doc = Jsoup.parse(html)
            def links = doc.select('a[href^="/wiki/"], a[href^="./"]')
            String targetName = name.toLowerCase()
            links.each { link ->
                String href = link.attr('href')
                String linkText = link.text().toLowerCase()
                if (href.startsWith('/wiki/')) {
                    href = href.substring(6)
                } else if (href.startsWith('./')) {
                    href = href.substring(2)
                }
                String cleanHref = URLDecoder.decode(href, 'UTF-8').replace('_', ' ').toLowerCase()
                if (cleanHref.startsWith(targetName + ' (') || linkText.startsWith(targetName + ' (')) {
                    String candidate = href.replace(' ', '_').replaceAll(/^\/+/, '')
                    if (candidate && !candidates.contains(candidate)) {
                        candidates << candidate
                    }
                }
            }
        } catch (Exception ex) {
            log.warn "Error retrieving disambiguation page for ${name}: ${ex.message}"
        }
        return candidates
    }

    /**
     * Normalise an ALA kingdom value so it can be matched against Wikipedia page text.
     */
    private String normaliseKingdom(String kingdom) {
        return kingdom.trim().toLowerCase().replaceAll(/[^a-z]/, '')
    }

    /**
     * Check whether the supplied taxon article HTML appears to belong to the expected kingdom.
     * The check is intentionally conservative: the kingdom name must appear in the page text
     * close to other taxonomy indicators.
     */
    private boolean pageMatchesKingdom(String html, String expectedKingdom) {
        if (!expectedKingdom) {
            return true
        }
        def doc = Jsoup.parse(html)
        String pageText = doc.text().toLowerCase()
        return pageText.contains("kingdom") && pageText.contains(expectedKingdom)
    }

    /**
     * Query the MediaWiki search API and return a list of candidate page titles.
     * If the search returns a single result we trust the title match and skip the
     * snippet filter, since many taxon articles have non-taxonomic opening sentences
     * (e.g. "The great white shark ... is a large shark"). When there are multiple
     * results, the configured snippet pattern filters out pages that are clearly not
     * taxon articles (disambiguation pages, bands, airports, manga, etc.).
     *
     * @param name The taxon name to search for
     * @param snippetPattern Pattern to apply when there are multiple results
     */
    private List<String> searchWikipediaCandidates(String name, Pattern snippetPattern) {
        def candidates = []
        try {
            String query = "intitle:\"${name}\""
            String url = "${wikipediaApi}?action=query&list=search" +
                    "&srsearch=" + URLEncoder.encode(query, 'UTF-8') +
                    "&srnamespace=0" +
                    "&srlimit=" + wikipediaSearchLimit +
                    "&utf8=1&format=json"
            def json = webClientService.getJson(url)
            if (json instanceof JSONObject && json.has("error")) {
                log.warn "Error searching Wikipedia for ${name}: ${json.error}"
                return candidates
            }
            def results = json?.query?.search
            if (results) {
                boolean singleResult = results.size() == 1
                results.each { result ->
                    if (singleResult) {
                        candidates << result.title.replace(' ', '_')
                    } else {
                        String snippet = stripHtml(result.snippet ?: '')
                        if (snippetPattern.matcher(snippet).find()) {
                            candidates << result.title.replace(' ', '_')
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn "Error searching Wikipedia candidates for ${name}: ${ex.message}"
        }
        return candidates
    }

    /**
     * Strip HTML markup from a string.
     */
    private String stripHtml(String html) {
        return html.replaceAll(/<[^>]+>/, '')
    }

    /**
     * Determine whether the supplied HTML is a Wikipedia taxon article.
     */
    private boolean isTaxonArticle(String html) {
        if (!html) {
            return false
        }
        try {
            def doc = Jsoup.parse(html)
            if (!doc.select(".infobox.biota").isEmpty() ||
                    !doc.select('[aria-labelledby="Taxon_identifiers"]').isEmpty()) {
                return true
            }
            // The MediaWiki REST API returns Parsoid HTML where templates are rendered as
            // transclusion metadata rather than classic infobox markup. Accept pages that
            // contain a Speciesbox/Automatic taxobox/Taxobox template or a taxonomic short
            // description such as "Species of spider".
            def dataMw = doc.select("[data-mw]")
            return dataMw.any { element ->
                String mw = element.attr("data-mw")
                mw.contains('"Speciesbox"') ||
                        mw.contains('"Automatic taxobox"') ||
                        mw.contains('"Taxobox"') ||
                        (mw.contains('"Short description"') &&
                                mw.toLowerCase().contains("species of"))
            }
        } catch (Exception ex) {
            log.warn "Error parsing Wikipedia HTML for taxon validation: ${ex.message}"
            return false
        }
    }

}
