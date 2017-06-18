package org.lastaflute.doc.policycheck;

/**
 * @author yuto.eguma
 * @since 1.? (2017/05/28 wakoshi)
 */
public class JsonPolicyIfClause {

    protected final String _statement;
    protected final String _ifItem;
    protected final String _ifValue;
    protected final boolean _notIfValue;
    protected final String _thenClause;
    protected final boolean _notThenClause;
    protected final String _thenItem; // null allowed
    protected final String _thenValue; // null allowed
    protected final boolean _notThenValue;
    protected final String _supplement; // null allowed

    public JsonPolicyIfClause(String statement, String ifItem, String ifValue, boolean ifNotValue, String thenClause,
              boolean notThenClause, String thenItem, String thenValue, boolean thenNotValue, String supplement) {
        _statement = statement;
        _ifItem = ifItem;
        _ifValue = ifValue;
        _notIfValue = ifNotValue;
        _thenClause = thenClause;
        _notThenClause = notThenClause;
        _thenItem = thenItem;
        _thenValue = thenValue;
        _notThenValue = thenNotValue;
        _supplement = supplement;
    }

    public String getStatement() {
        return _statement;
    }

    public String getIfItem() {
        return _ifItem;
    }

    public String getIfValue() {
        return _ifValue;
    }

    public boolean isNotIfValue() {
        return _notIfValue;
    }

    public String getThenClause() {
        return _thenClause;
    }

    public boolean isNotThenClause() {
        return _notThenClause;
    }

    public String getThenItem() {
        return _thenItem;
    }

    public String getThenValue() {
        return _thenValue;
    }

    public boolean isNotThenValue() {
        return _notThenValue;
    }

    public String getSupplement() {
        return _supplement;
    }
}
