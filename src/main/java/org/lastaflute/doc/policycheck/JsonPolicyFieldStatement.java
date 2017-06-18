package org.lastaflute.doc.policycheck;

import org.lastaflute.doc.meta.ActionDocMeta;
import org.lastaflute.doc.meta.TypeDocMeta;

import java.util.List;
import java.util.Map;

/**
 * @author yuto.eguma
 * @since 1.? (2017/05/28 wakoshi)
 */
public class JsonPolicyFieldStatement {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected JsonPolicyMiscSecretary _secretary = new JsonPolicyMiscSecretary();

    // ===================================================================================
    //                                                                     Field Statement
    //                                                                     ===============
    public void checkFieldStatement(ActionDocMeta actionDocMeta, Map<String, Object> fieldMap, JsonPolicyResult result) {
        List<TypeDocMeta> fieldTypeDocMetaList = actionDocMeta.getFieldTypeDocMetaList();
        fieldTypeDocMetaList.forEach(fieldTypeDocMeta -> {
            processFieldStatement(fieldTypeDocMeta, fieldMap, result);
        });
    }

    protected void processFieldStatement(TypeDocMeta fieldTypeDocMeta, Map<String, Object> fieldMap, JsonPolicyResult result) {
        @SuppressWarnings("unchecked")
        final List<String> statementList = (List<String>) fieldMap.get("statementList");
        statementList.forEach(statement -> {
            evaluateFieldIfClause(fieldTypeDocMeta, result, _secretary.extractIfClause(statement));
        });
    }

    // ===================================================================================
    //                                                                            Evaluate
    //                                                                            ========
    // -----------------------------------------------------
    //                                             If Clause
    //                                             ---------
    // e.g.
    //  if fieldName is suffix:Str then bad
    //  if fieldName is suffix:Id then type is Integer
    protected void evaluateFieldIfClause(TypeDocMeta fieldTypeDocMeta, JsonPolicyResult result, JsonPolicyIfClause ifClause) {
        final String ifItem = ifClause.getIfItem();  // fieldName
        final String ifValue = ifClause.getIfValue();  // suffix:Str
        final boolean notIfValue = ifClause.isNotIfValue();
        if (ifItem.equalsIgnoreCase("fieldName")) { // if fieldName is ...
            if (isHitField(fieldTypeDocMeta.getName(), ifValue) != notIfValue) {
                evaluateFieldThenClause(fieldTypeDocMeta, result, ifClause);
            }
        }
    }

    protected boolean isHitField(String fieldName, String value) {
        if (value.contains("suffix:")) {
            String suffix = value.replace("suffix:", "");
            return fieldName.endsWith(suffix);
        }
        // TODO yuto consider this method should throw exception if IllegalValue (2017/05/28)
        return false;
    }

    // -----------------------------------------------------
    //                                           Then Clause
    //                                           -----------
    protected void evaluateFieldThenClause(TypeDocMeta fieldTypeDocMeta, JsonPolicyResult result, JsonPolicyIfClause ifClause) {
        final String thenClause = ifClause.getThenClause();
        if (ifClause.getThenItem() != null) { // e.g. then type is Integer
            evaluateFieldThenItemValue(fieldTypeDocMeta, result, ifClause);
        } else {
            final boolean notThenClause = ifClause.isNotThenClause(); // e.g. then bad
            if (thenClause.equalsIgnoreCase("bad") == !notThenClause) { // "not bad" is non-sense
                result.addViolation( "The field is no good: " + toFieldDisp(fieldTypeDocMeta));
            }
        }
    }

    protected void evaluateFieldThenItemValue(TypeDocMeta fieldTypeDocMeta, JsonPolicyResult result, JsonPolicyIfClause ifClause) {
        final String thenItem = ifClause.getThenItem();
        final String thenValue = ifClause.getThenValue();
        if (thenItem.equalsIgnoreCase("type")) {
            String typeName = fieldTypeDocMeta.getTypeName();
            if (typeName.equalsIgnoreCase(thenValue)) {
                result.addViolation("The field is no good: " + toFieldDisp(fieldTypeDocMeta));
            }
        }
    }

    protected String toFieldDisp(TypeDocMeta fieldTypeDocMeta) {
        return fieldTypeDocMeta.getName();
    }
}
