package org.lastaflute.doc.policycheck;

import org.dbflute.util.Srl;
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
            if (isHitFieldType(fieldTypeDocMeta.getName(), ifValue) != notIfValue) {
                evaluateFieldThenClause(fieldTypeDocMeta, result, ifClause);
            }
        }
    }

    // -----------------------------------------------------
    //                                           Then Clause
    //                                           -----------
    protected void evaluateFieldThenClause(TypeDocMeta fieldTypeDocMeta, JsonPolicyResult result, JsonPolicyIfClause ifClause) {
        final String thenClause = ifClause.getThenClause();
        if (ifClause.getThenItem() != null) { // e.g. then fieldType is Integer
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
        final boolean notThenValue = ifClause.isNotThenValue();
        if (thenItem.equalsIgnoreCase("fieldType")) { // e.g.  if ... then fieldType is Integer or Long
            String fieldType = toTypeNameOnly(fieldTypeDocMeta.getTypeName());
            if (!isHitFieldType(fieldType, thenValue) == !notThenValue) {
                result.addViolation("The fieldType is no good: " + toFieldDisp(fieldTypeDocMeta));
            }
        }
    }

    protected String toTypeNameOnly(String fullTypeName) { // e.g. java.lang.Integer -> Integer
        List<String> packageNameList = Srl.splitList(fullTypeName, ".");
        return packageNameList.get(packageNameList.size() - 1);
    }

    protected String toFieldDisp(TypeDocMeta fieldTypeDocMeta) {
        // TODO yuto detail log (2017/07/02)
        return fieldTypeDocMeta.getName();
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean isHitFieldType(String fieldType, String hint) {
        return _secretary.isHitFieldType(fieldType, hint);
    }
}
