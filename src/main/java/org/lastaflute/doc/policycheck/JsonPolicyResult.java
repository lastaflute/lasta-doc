package org.lastaflute.doc.policycheck;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yuto.eguma
 * @since 1.? (2017/05/28 wakoshi)
 */
public class JsonPolicyResult {

    // TODO yuto consider violation miserable log (2017/05/28)
    private List<String> _violationList = new ArrayList<>();

    public void addViolation(String violation) {
        _violationList.add(violation);
    }

    public boolean isViolate() {
        return !_violationList.isEmpty();
    }
}
