/*
 * Copyright 2015-2019 the original author or authors.
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
import org.lastaflute.doc.unit.mock.MockCDef;
import org.lastaflute.doc.unit.mock.SeaForm;

/**
 * @author jflute
 * @since 0.3.6 (2019/05/11 Saturday)
 */
public class ActionDocumentGeneratorTest extends PlainTestCase {

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    // -----------------------------------------------------
    //                                         Target Suffix
    //                                         -------------
    public void test_isTargetSuffixResolvedClass_basic() {
        // ## Arrange ##
        ActionDocumentGenerator generator = createGenerator();

        // ## Act ##
        // ## Assert ##
        assertTrue(generator.isTargetSuffixResolvedClass(SeaForm.class));
        assertTrue(generator.isTargetSuffixResolvedClass(SeaForm.HangarPart.class));

        assertFalse(generator.isTargetSuffixResolvedClass(String.class));
        assertFalse(generator.isTargetSuffixResolvedClass(MockCDef.MemberStatus.class));
        assertFalse(generator.isTargetSuffixResolvedClass(MockCDef.WhiteConfusingFormatBodying.class));
    }

    private ActionDocumentGenerator createGenerator() {
        return new ActionDocumentGenerator(Collections.emptyList(), 0, OptionalThing.empty());
    }
}
