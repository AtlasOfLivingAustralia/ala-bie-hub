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

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class MetadataSpec extends Specification {
    void "test read from source 1"() {
        given:
        def mapper = new ObjectMapper()
        when:
        def metadata = mapper.readValue(this.class.getResource("metadata-1.json"), Metadata.class)
        then:
        metadata.title == "Some test metadata"
        metadata.description == "A test description"
        metadata.version == "1.1"
        metadata.created != null
        metadata.modified != null
    }

    void "test read from source 2"() {
        given:
        def mapper = new ObjectMapper()
        when:
        def metadata = mapper.readValue(this.class.getResource("metadata-2.json"), Metadata.class)
        then:
        metadata.title == null
        metadata.description == "Another test description"
        metadata.version == null
        metadata.created == null
        metadata.modified == null
    }
}
