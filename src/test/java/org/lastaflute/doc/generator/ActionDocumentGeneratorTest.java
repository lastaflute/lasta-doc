/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.lastaflute.doc.generator;

import java.util.Collections;

import org.dbflute.optional.OptionalThing;
import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.web.response.JsonResponse;

/**
 * @author jflute
 */
public class ActionDocumentGeneratorTest extends PlainTestCase {

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    public void test_extractJsonResponseIterableElementTypeName_basic() {
        // ## Arrange ##
        ActionDocumentGenerator generator = createEmptyGenerator();
        String prefix = JsonResponse.class.getSimpleName();

        // ## Act ##
        // ## Assert ##
        assertEquals("String", generator.extractJsonResponseIterableElementTypeName(prefix + "<List<String>>"));
        assertEquals("SeaLandPiari", generator.extractJsonResponseIterableElementTypeName(prefix + "<List<SeaLandPiari>>"));
        assertEquals("Sea", generator.extractJsonResponseIterableElementTypeName(prefix + "<List<Sea<Land>>>"));
        assertEquals("Map", generator.extractJsonResponseIterableElementTypeName(prefix + "<List<Map<String, Object>>>"));
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected ActionDocumentGenerator createEmptyGenerator() {
        return new ActionDocumentGenerator(Collections.emptyList(), 0, OptionalThing.empty());
    }
}
