package org.lastaflute.doc.policycheck.exception;

/**
 * @author yuto.eguma
 */
public class JsonPolicyUnknownPropertyException extends RuntimeException {

    /** The serial version UID for object serialization. (Default) */
    private static final long serialVersionUID = 1L;

    public JsonPolicyUnknownPropertyException(String msg) {
        super(msg);
    }
}
