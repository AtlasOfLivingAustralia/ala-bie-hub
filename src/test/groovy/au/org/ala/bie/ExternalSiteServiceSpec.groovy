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

import com.stehno.ersatz.ContentType
import com.stehno.ersatz.Encoders
import com.stehno.ersatz.ErsatzServer
import grails.testing.services.ServiceUnitTest
import groovy.json.JsonSlurper
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.regex.Pattern

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class ExternalSiteServiceSpec extends Specification implements ServiceUnitTest<ExternalSiteService> {
    @AutoCleanup
    ErsatzServer server

    PolicyFactory policy

    def setup() {
        server = new ErsatzServer()
        server.reportToConsole()
        grailsApplication.config.wikipedia.url = server.httpUrl + '/page/html/'
        grailsApplication.config.wikipedia.api = server.httpUrl + '/api.php'
        grailsApplication.config.wikipedia.lang = 'en'
        grailsApplication.config.wikipedia.rankPattern = '(?i)species|genus|family|order|class|phylum|kingdom'
        grailsApplication.config.wikipedia.searchLimit = 20
        service.setConfiguration(grailsApplication.config)

        String allowedElements = "h2,div,a,br,i,b,span,ul,li,p,sup"
        String allowedAttributes ="href;a;^(http|https|mailto|#).+,class;span,id;span,src;img;^(http|https).+"

        HtmlPolicyBuilder builder = new HtmlPolicyBuilder()
                .allowStandardUrlProtocols()
                .requireRelNofollowOnLinks()

        if (allowedElements) {
            String[] elements = allowedElements.split(",")
            elements.each {
                builder.allowElements(it)
            }
        }

        if (allowedAttributes){
            String[] attributes = allowedAttributes.split(",")
            attributes.each { attribute ->
                String[] values = attribute.split (";")
                if (values.length == 2){
                    builder.allowAttributes(values[0]).onElements(values[1])
                } else {
                    builder.allowAttributes(values[0]).matching(Pattern.compile(values[2], Pattern.CASE_INSENSITIVE)).onElements(values[1])
                }

            }
        }

        policy = builder.toFactory()
    }

    def cleanup() {
    }

    protected Map getResponse(String resource) {
        JsonSlurper slurper = new JsonSlurper()
        return slurper.parse(this.class.getResource(resource), 'UTF-8')
    }


    void "test search Wikipedia returns taxon article for exact scientific name"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Acipenser brevirostrum"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Acipenser brevirostrum', snippet: 'a <span class="searchmatch">species</span> of sturgeon']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Acipenser_brevirostrum') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Acipenser brevirostrum')

        then:
        response != null
        response.title == 'Acipenser_brevirostrum'
        response.html.contains('infobox biota')
    }

    void "test search Wikipedia resolves ambiguous name to taxon page"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Meretrix"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Meretrix', snippet: 'Meretrix may refer to'],
                                            [ns: 0, title: 'Meretrix (bivalve)', snippet: '<span class="searchmatch">genus</span> of saltwater clams'],
                                            [ns: 0, title: 'Meretrix (poem)', snippet: 'a poem by author']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Meretrix_(bivalve)') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Meretrix')

        then:
        response != null
        response.title == 'Meretrix_(bivalve)'
        response.html.contains('infobox biota')
    }

    void "test search Wikipedia filters candidates without taxonomic rank in snippet"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Foo"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Foo (disambiguation)', snippet: 'Foo may refer to'],
                                            [ns: 0, title: 'Foo (band)', snippet: 'a rock band']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Foo')

        then:
        response != null
        response.title == null
        response.html == ''
    }

    void "test search Wikipedia rejects candidate failing taxon HTML validation"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Badtaxon"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Badtaxon', snippet: 'a <span class="searchmatch">species</span> of nothing']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Badtaxon') {
                called(1)
                responder {
                    code(200)
                    body('<section><p>Not a taxon article.</p></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Badtaxon')

        then:
        response != null
        response.title == null
        response.html == ''
    }

    void "test search Wikipedia returns empty when blacklisted"() {
        given:
        service.blacklist = new Blacklist(list: [[scientificName: 'Naughty nomen']] as Set)

        when:
        def response = service.searchWikipedia('Naughty nomen')

        then:
        response != null
        response.title == null
        response.html == ''
    }

    void "test search Wikipedia uses custom rank pattern from config"() {
        given:
        service.wikipediaRankPattern = '(?i)clam|bivalve'
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Paged"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Paged', snippet: 'a page about things'],
                                            [ns: 0, title: 'Paged (bivalve)', snippet: 'a kind of <span class="searchmatch">clam</span>']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Paged_(bivalve)') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Paged')

        then:
        response != null
        response.title == 'Paged_(bivalve)'
        response.html.contains('infobox biota')
    }

    void "test search Wikipedia kingdom check rejects cross-kingdom homonym"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Vertebrata"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Vertebrata', snippet: 'a <span class="searchmatch">genus</span> of algae'],
                                            [ns: 0, title: 'Vertebrate', snippet: 'a <span class="searchmatch">genus</span> of animals']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Vertebrata') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table><p>Kingdom: Plantae</p></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Vertebrata', 'Animalia')

        then:
        response != null
        response.title == null
        response.html == ''
    }

    void "test search Wikipedia kingdom check accepts same-kingdom match"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Moggridgea rainbowi"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Moggridgea rainbowi', snippet: 'a <span class="searchmatch">species</span> of spider']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Moggridgea_rainbowi') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table><p>Kingdom: Animalia</p></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Moggridgea rainbowi', 'Animalia')

        then:
        response != null
        response.title == 'Moggridgea_rainbowi'
        response.html.contains('infobox biota')
    }

    void "test search Wikipedia returns result when kingdom not supplied"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Something"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Something', snippet: 'a <span class="searchmatch">species</span> of thing']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Something') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Something')

        then:
        response != null
        response.title == 'Something'
        response.html.contains('infobox biota')
    }

    void "test search Wikipedia for Chara finds alga page beyond initial top results"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Chara"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Shugo Chara!', snippet: 'also known as My Guardian Characters, is a Japanese manga series'],
                                            [ns: 0, title: 'Chara Airport', snippet: 'an airport in Russia'],
                                            [ns: 0, title: 'Canes Venatici', snippet: 'a constellation in the northern sky'],
                                            [ns: 0, title: 'Chara (singer)', snippet: 'a Japanese singer'],
                                            [ns: 0, title: 'Chara (alga)', snippet: '<span class="searchmatch">Chara</span> is a <span class="searchmatch">genus</span> of charophyte green algae'],
                                            [ns: 0, title: 'Chara people', snippet: 'a people group of Ethiopia']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Chara_(alga)') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table><p>Kingdom: Plantae</p></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Chara', 'Plantae')

        then:
        response != null
        response.title == 'Chara_(alga)'
        response.html.contains('infobox biota')
    }

    void "test search Wikipedia rank pattern does not match snippet HTML markup"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Chara"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Chara Manga', snippet: 'Japanese <span class="searchmatch">Chara</span> manga'],
                                            [ns: 0, title: 'Chara (alga)', snippet: '<span class="searchmatch">Chara</span> is a <span class="searchmatch">genus</span> of green algae']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Chara_(alga)') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table><p>Kingdom: Plantae</p></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Chara', 'Plantae')

        then:
        response != null
        response.title == 'Chara_(alga)'
    }

    void "test search Wikipedia falls back to disambiguation page for cross-kingdom homonym"() {
        given:
        server.expectations {
            get('/api.php') {
                query('action', 'query')
                query('list', 'search')
                query('srsearch', 'intitle:"Chara"')
                query('srnamespace', '0')
                query('srlimit', '20')
                query('utf8', '1')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body([
                            query: [
                                    search: [
                                            [ns: 0, title: 'Chara (alga)', snippet: '<span class="searchmatch">Chara</span> is a <span class="searchmatch">genus</span> of charophyte green algae']
                                    ]
                            ]
                    ], ContentType.APPLICATION_JSON)
                }
            }
            get('/page/html/Chara_(alga)') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table><p>Kingdom: Plantae</p></section>', ContentType.TEXT_HTML)
                }
            }
            get('/page/html/Chara') {
                called(1)
                responder {
                    code(200)
                    body('''<section>
                        <p>Chara may refer to:</p>
                        <ul>
                            <li><a href="/wiki/Chara_(alga)">Chara (alga)</a>, a genus of algae</li>
                            <li><a href="/wiki/Chara_(moth)">Chara (moth)</a>, a genus of moths</li>
                            <li><a href="/wiki/Chara_(star)">Chara (star)</a></li>
                        </ul>
                    </section>''', ContentType.TEXT_HTML)
                }
            }
            get('/page/html/Chara_(moth)') {
                called(1)
                responder {
                    code(200)
                    body('<section><table class="infobox biota">Scientific classification</table><p>Kingdom: Animalia</p></section>', ContentType.TEXT_HTML)
                }
            }
        }

        when:
        def response = service.searchWikipedia('Chara', 'Animalia')

        then:
        response != null
        response.title == 'Chara_(moth)'
        response.html.contains('Kingdom: Animalia')
    }

    void "test get BHL literature"() {
        given:
        server.expectations {
            get('/api3') {
                query('op', 'PublicationSearch')
                query('searchterm', '\"Acacia dealbata\"')
                query('page', '1')
                query('apikey', '<key value>')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body(getResponse('bhl-search-1.json'), ContentType.APPLICATION_JSON)
                }
            }

        }
        when:
        service.bhlApi = server.httpUrl + '/api3'
        def response = service.searchBhl(['Acacia dealbata'], 0, 10, false)
        then:
        response != null
        response.max == 2
        response.more == false
        response.start == 0
        response.rows == 10
        response.search != null
        response.search in List
        response.search.size() == 1
        response.results != null
        response.results in List
        response.results.size() == 2
        def result = response.results[0]
        result.type == 'article-journal'
        result.title == 'Allelopathic effect of the invasive Acacia dealbata Link (Fabaceae) on two native plant species in south-central Chile'
    }

}
