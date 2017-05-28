package org.lastaflute.doc;

import org.dbflute.utflute.core.PlainTestCase;

import java.util.List;
import java.util.Map;

/**
 * @author yuto.eguma
 */
public class DocumentGeneratorTest extends PlainTestCase {

    public void test_loadJsonPolicyMap() throws Exception {
        // ### arrange ###
        DocumentGenerator documentGenerator = new DocumentGenerator();

        // ### act ###
        Map<String, Object> policyMap = documentGenerator.loadJsonPolicyMap();

        // ### assert ###
        assertFalse(policyMap.isEmpty());

        Object actionExceptList = policyMap.get("actionExceptList");
        assertNotNull(actionExceptList);
        assertTrue(actionExceptList instanceof List);

        Object actionTargetList = policyMap.get("actionTargetList");
        assertNotNull(actionTargetList);
        assertTrue(actionTargetList instanceof List);

        Object fieldExceptMap = policyMap.get("fieldExceptMap");
        assertNotNull(fieldExceptMap);
        assertTrue(fieldExceptMap instanceof Map);

        Object fieldMap = policyMap.get("fieldMap");
        assertNotNull(fieldMap);
        assertTrue(fieldMap instanceof Map);
    }
}