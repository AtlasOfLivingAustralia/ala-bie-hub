/*
 * Copyright (C) 2026 Atlas of Living Australia
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

import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Live Wikipedia integration tests.
 *
 * These tests make real HTTP requests to the public Wikipedia API and are intended to catch
 * regressions in either the service filtering logic or changes in Wikipedia page structure.
 *
 * They do not run during normal builds. Run them explicitly with:
 *
 *     ./gradlew liveWikipediaTest
 */
@Requires({ System.getProperty('wikipedia.live.tests') == 'true' })
class LiveWikipediaSearchSpec extends Specification {

    ExternalSiteService externalSiteService = new ExternalSiteService()

    /**
     * A descriptive User-Agent is required by the Wikimedia Foundation for non-browser API access.
     * See https://foundation.wikimedia.org/wiki/Policy:User-Agent_policy
     */
    static final String USER_AGENT = 'ala-bie-hub live-wikipedia-test (support@ala.org.au)'

    def setup() {
        def config = new Expando()
        config.getProperty = { String name, String defaultValue = null ->
            if (name == 'customUserAgent') return USER_AGENT
            return defaultValue
        }
        def grailsApplication = new Expando()
        grailsApplication.config = config

        externalSiteService.webClientService = new WebClientService(grailsApplication: grailsApplication)
        externalSiteService.wikipediaUrl = 'https://en.wikipedia.org/api/rest_v1/page/html/'
        externalSiteService.wikipediaApi = 'https://en.wikipedia.org/w/api.php'
        externalSiteService.wikipediaLang = 'en'
        externalSiteService.wikipediaSnippetPattern = '(?i)species|genus|family|order|class|phylum|kingdom|australia|endemic'
        externalSiteService.wikipediaSearchLimit = 20
    }

    /**
     * Read the curated list of taxa and expected HTML fragments from the CSV file in
     * src/liveWikipediaTest/resources.
     */
    List<Map<String, String>> getLiveTestCases() {
        def cases = []
        def csv = this.class.getResource('/wikipedia-live-tests.csv')
        if (!csv) {
            throw new FileNotFoundException('wikipedia-live-tests.csv not found on classpath')
        }
        csv.text.eachLine { line, lineNumber ->
            // Skip header line
            if (lineNumber == 1 || line.trim().startsWith('scientificName')) return
            // Skip blank or comment lines
            if (!line || line.trim().startsWith('#')) return

            // CSV may contain trailing commas or a 4th commonName column.
            // Trim trailing empty fields before parsing.
            def parts = line.split(',').toList()
            while (!parts.isEmpty() && parts.last().trim() == '') {
                parts.removeLast()
            }
            if (parts.size() < 3) {
                // No expected content and no common name; skip this row.
                return
            }
            def expectedHtml = parts[2].trim()
            def commonName = parts.size() > 3 ? parts[3].trim() : ''
            cases << [
                    scientificName: parts[0].trim(),
                    kingdom       : parts[1].trim(),
                    expectedHtml  : expectedHtml,
                    commonName    : commonName
            ]
        }
        return cases
    }

    @Unroll
    void "Wikipedia search for #scientificName returns content containing at least one expected fragment"() {
        when:
        def response = externalSiteService.searchWikipedia(scientificName, kingdom)

        then:
        response != null
        response.html != null
        !response.html.isEmpty()
        def lowerHtml = response.html.toLowerCase()
        def lowerName = scientificName.toLowerCase()
        def htmlMatched = expectedHtml && lowerHtml.contains(expectedHtml.toLowerCase())
        def commonNameMatched = commonName && lowerHtml.contains(commonName.toLowerCase())
        htmlMatched || commonNameMatched || lowerHtml.contains(lowerName)

        where:
        testCase << getLiveTestCases()
        scientificName = testCase.scientificName
        kingdom = testCase.kingdom
        expectedHtml = testCase.expectedHtml
        commonName = testCase.commonName
    }
}
