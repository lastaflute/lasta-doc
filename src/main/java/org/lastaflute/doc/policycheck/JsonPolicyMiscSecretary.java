package org.lastaflute.doc.policycheck;

import org.dbflute.util.Srl;
import org.dbflute.util.Srl.ScopeInfo;

/**
 * @author yuto.eguma
 * @since 1.? (2017/05/28 wakoshi)
 */
public class JsonPolicyMiscSecretary {

    // ===================================================================================
    //                                                                           Statement
    //                                                                           =========
    public JsonPolicyIfClause extractIfClause(String statement) {
        if (!statement.startsWith("if ")) {
            String msg = "The element of statementList should start with 'if' for SchemaPolicyCheck: " + statement;
            throw new IllegalStateException(msg);
        }
        final ScopeInfo ifClauseScope = Srl.extractScopeFirst(statement, "if ", " then ");
        if (ifClauseScope == null) {
            final String additional = "The statement should start with 'if' and contain 'then'.";
            throwJsonPolicyCheckIllegalIfThenStatementException(statement, additional);
        }
        final String ifClause = ifClauseScope.getContent().trim();
        final String equalsDelimiter = " is ";
        final String notPrefix = "not ";
        if (!ifClause.contains(equalsDelimiter)) {
            final String additional = "The if-clause should contain 'is': " + ifClause;
            throwJsonPolicyCheckIllegalIfThenStatementException(statement, additional);
        }
        final String ifItem = Srl.substringFirstFront(ifClause, equalsDelimiter).trim();
        final String ifValueCandidate = Srl.substringFirstRear(ifClause, equalsDelimiter).trim();
        final boolean notIfValue = ifValueCandidate.startsWith(notPrefix);
        final String ifValue = notIfValue ? Srl.substringFirstRear(ifValueCandidate, notPrefix).trim() : ifValueCandidate;
        final String thenRear = ifClauseScope.substringInterspaceToNext();
        String thenClause; // may be filtered later
        final String supplement;
        final String supplementDelimiter = " => "; // e.g. if fieldName is suffix:Str then bad => "using 'str' is non-sense" (comment)
        if (thenRear.contains(supplementDelimiter)) {
            thenClause = Srl.substringLastFront(thenRear, supplementDelimiter).trim();
            supplement = Srl.substringLastRear(thenRear, supplementDelimiter).trim();
        } else {
            thenClause = thenRear;
            supplement = null;
        }
        final boolean notThenClause;
        final String thenItem;
        final boolean notThenValue;
        final String thenValue;
        if (thenClause.contains(equalsDelimiter)) {
            notThenClause = false;
            thenItem = Srl.substringFirstFront(thenClause, equalsDelimiter).trim();
            final String valueCandidate = Srl.substringFirstRear(thenClause, equalsDelimiter).trim();
            notThenValue = valueCandidate.startsWith(notPrefix);
            thenValue = notThenValue ? Srl.substringFirstRear(valueCandidate, notPrefix).trim() : valueCandidate;
        } else {
            notThenClause = thenClause.startsWith(notPrefix);
            if (notThenClause) {
                thenClause = Srl.substringFirstRear(thenClause, notPrefix);
            }
            thenItem = null;
            notThenValue = false;
            thenValue = null;
        }
        return new JsonPolicyIfClause(statement, ifItem, ifValue, notIfValue, thenClause, notThenClause, thenItem, thenValue,
                notThenValue, supplement);
    }

    private void throwJsonPolicyCheckIllegalIfThenStatementException(String statement, String additional) {
        // TODO yuto throw exception (2017/05/28)
    }
}
