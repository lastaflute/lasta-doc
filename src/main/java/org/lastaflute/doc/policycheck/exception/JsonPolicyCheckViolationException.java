package org.lastaflute.doc.policycheck.exception;

/**
 * @author yuto.eguma
 */
public class JsonPolicyCheckViolationException extends RuntimeException {

    /** The serial version UID for object serialization. (Default) */
    private static final long serialVersionUID = 1L;

    public JsonPolicyCheckViolationException(String msg) {
        super(msg);
    }
}
