package org.lastaflute.doc.policycheck;

import java.util.Collections;
import java.util.Map;

import org.dbflute.infra.dfprop.DfPropFile;
import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.doc.meta.ActionDocMeta;
import org.lastaflute.doc.meta.TypeDocMeta;
import org.lastaflute.doc.policycheck.exception.JsonPolicyCheckViolationException;

/**
 * @author yuto.eguma
 */
public class JsonPolicyCheckerTest extends PlainTestCase {

    private static final String BASE_POLICY_MAP_DIR = "src/test/resources/policymap/";

    public void test_checkPolicyIfNeeds_EmptyActionMetaDoc() throws Exception {
        // ### Arrange ###
        DfPropFile propFile = new DfPropFile();
        Map<String, Object> policyMap = propFile.readMap(toFilePath("statementTestPolicyMap.dfprop"), null);

        JsonPolicyChecker jsonPolicyChecker = new JsonPolicyChecker(Collections.emptyList(), policyMap);

        // ### Act ###
        // ### Assert ###
        jsonPolicyChecker.checkPolicyIfNeeds();
    }

    public void test_checkPolicyIfNeeds_violationStatementPolicy() throws Exception {
        // ### Arrange ###
        DfPropFile propFile = new DfPropFile();
        Map<String, Object> policyMap = propFile.readMap(toFilePath("statementTestPolicyMap.dfprop"), null);

        // field name suffix is "Str", this is defined bad on "statementTestPolicyMap.dfprop"
        ActionDocMeta actionDocMeta = new ActionDocMeta();
        TypeDocMeta typeDocMeta = new TypeDocMeta();
        typeDocMeta.setName("sampleStr");
        actionDocMeta.setFieldTypeDocMetaList(Collections.singletonList(typeDocMeta));

        JsonPolicyChecker jsonPolicyChecker = new JsonPolicyChecker(Collections.singletonList(actionDocMeta), policyMap);

        // ### Act ###
        // ### Assert ###
        assertException(JsonPolicyCheckViolationException.class, () -> jsonPolicyChecker.checkPolicyIfNeeds());
    }

    private String toFilePath(String fileName) {
        return BASE_POLICY_MAP_DIR + fileName;
    }
}