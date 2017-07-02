package org.lastaflute.doc.policycheck;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dbflute.infra.dfprop.DfPropFile;
import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.doc.meta.ActionDocMeta;
import org.lastaflute.doc.meta.TypeDocMeta;
import org.lastaflute.doc.policycheck.exception.JsonPolicyCheckViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuto.eguma
 */
public class JsonPolicyCheckerTest extends PlainTestCase {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final String BASE_POLICY_MAP_DIR = "src/test/resources/policymap/";
    private static final Logger logger = LoggerFactory.getLogger(JsonPolicyCheckerTest.class);

    // ===================================================================================
    //                                                                                Test
    //                                                                                ====
    public void test_checkPolicyIfNeeds_EmptyActionMetaDoc() throws Exception {
        // ### Arrange ###
        final Map<String, Object> policyMap = loadPolicyMap("statementTestPolicyMap.dfprop");
        final JsonPolicyChecker jsonPolicyChecker = new JsonPolicyChecker(Collections.emptyList(), policyMap);

        // ### Act ###
        // ### Assert ###
        jsonPolicyChecker.checkPolicyIfNeeds();
    }

    public void test_checkPolicyIfNeeds_violationStatementPolicy_namePolicyViolation() throws Exception {
        // ### Arrange ###
        final Map<String, Object> policyMap = loadPolicyMap("statementTestPolicyMap.dfprop");

        // On "statementTestPolicyMap.dfprop", fieldName suffix must not be "Str"
        ActionDocMeta actionDocMeta = new ActionDocMeta();
        TypeDocMeta typeDocMeta = new TypeDocMeta();
        typeDocMeta.setName("sampleStr");
        typeDocMeta.setType(String.class);
        typeDocMeta.setTypeName(String.class.getName());
        actionDocMeta.setFieldTypeDocMetaList(Collections.singletonList(typeDocMeta));

        final JsonPolicyChecker jsonPolicyChecker = new JsonPolicyChecker(Collections.singletonList(actionDocMeta), policyMap);

        // ### Act ###
        // ### Assert ###
        assertException(JsonPolicyCheckViolationException.class, () -> jsonPolicyChecker.checkPolicyIfNeeds());
    }

    public void test_checkPolicyIfNeeds_violationStatementPolicy_fieldTypePolicyViolation() throws Exception {
        // ### Arrange ###
        final Map<String, Object> policyMap = loadPolicyMap("statementTestPolicyMap.dfprop");

        // On "statementTestPolicyMap.dfprop", field name suffix is "Id", type name must be Long or Integer
        ActionDocMeta actionDocMeta = new ActionDocMeta();
        TypeDocMeta typeDocMeta = new TypeDocMeta();
        typeDocMeta.setName("memberId");
        typeDocMeta.setType(String.class);
        typeDocMeta.setTypeName(String.class.getName());
        actionDocMeta.setFieldTypeDocMetaList(Collections.singletonList(typeDocMeta));

        JsonPolicyChecker jsonPolicyChecker = new JsonPolicyChecker(Collections.singletonList(actionDocMeta), policyMap);

        // ### Act ###
        // ### Assert ###
        assertException(JsonPolicyCheckViolationException.class, () -> jsonPolicyChecker.checkPolicyIfNeeds());
    }

    public void test_checkPolicyIfNeeds_violationStatementPolicy_fieldTypePolicyOk() throws Exception {
        // ### Arrange ###
        final Map<String, Object> policyMap = loadPolicyMap("statementTestPolicyMap.dfprop");
        final String checkedMark = "checked mark";

        // On "statementTestPolicyMap.dfprop", field name suffix is "Id", type name must be Long or Integer
        ActionDocMeta actionDocMeta = new ActionDocMeta();
        List<TypeDocMeta> typeDocMetaList = newArrayList();
        {
            TypeDocMeta typeDocMeta = new TypeDocMeta();
            typeDocMeta.setName("memberId");
            typeDocMeta.setType(Long.class);
            typeDocMeta.setTypeName(Long.class.getName());
            typeDocMetaList.add(typeDocMeta);
        }
        {
            TypeDocMeta typeDocMeta = new TypeDocMeta();
            typeDocMeta.setName("productId");
            typeDocMeta.setType(Integer.class);
            typeDocMeta.setTypeName(Integer.class.getName());
            typeDocMetaList.add(typeDocMeta);
        }
        actionDocMeta.setFieldTypeDocMetaList(typeDocMetaList);
        final JsonPolicyChecker jsonPolicyChecker = new JsonPolicyChecker(Collections.singletonList(actionDocMeta), policyMap);

        // ### Act ###
        try {
            jsonPolicyChecker.checkPolicyIfNeeds();
            markHere(checkedMark);
        } catch (JsonPolicyCheckViolationException e) {
            logger.error("JsonPolicyCheckViolationException occurred", e);
        }

        // ### Assert ###
        assertMarked(checkedMark);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    private Map<String, Object> loadPolicyMap(String fileName) {
        DfPropFile propFile = new DfPropFile();
        return propFile.readMap(toFilePath(fileName), null);
    }

    private String toFilePath(String fileName) {
        return BASE_POLICY_MAP_DIR + fileName;
    }
}