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
