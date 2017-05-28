/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.lastaflute.doc.policycheck;

import java.util.List;
import java.util.Map;

import org.dbflute.util.Srl;
import org.lastaflute.doc.meta.ActionDocMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuto.eguma (2017/05/02 itoshima)
 */
public class JsonPolicyChecker {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger _log = LoggerFactory.getLogger(JsonPolicyChecker.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<ActionDocMeta> _actionDocMetaList;
    protected final Map<String, Object> _policyMap;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JsonPolicyChecker(List<ActionDocMeta> actionDocMetaList, Map<String, Object> policyMap) {
        _actionDocMetaList = actionDocMetaList;
        _policyMap = policyMap;
    }

    // ===================================================================================
    //                                                                        Check Policy
    //                                                                        ============
    public void checkPolicyIfNeeds() {
        if (_policyMap.isEmpty()) {
            return;
        }
        _log.info("");
        _log.info("* * * * * * * * * * * * * * * * *");
        _log.info("*                               *");
        _log.info("*       Check Json Policy       *");
        _log.info("*                               *");
        _log.info("* * * * * * * * * * * * * * * * *");

        // map:{
        //    ; actionExceptList = list:{ [action-hints for except] }
        //    ; actionTargetList = list:{ [action-hints for target] }
        //    ; methodExceptMap = map:{
        //        ; [action-hint] = list:{ [method-hints for except] }
        //    }
        //    ; fieldExceptMap = map:{
        //        ; [action-hint] = list:{ [method.field-hints for except] }
        //    }
        //    ; methodMap = map:{
        //        ; themeList = list:{ [themes] }
        //        ; statementList = list:{ [statements] }
        //    }
        //    ; fieldMap = map:{
        //        ; themeList = list:{ [themes] }
        //        ; statementList = list:{ [statements] }
        //    }
        // }

        JsonPolicyResult result = new JsonPolicyResult();
        _actionDocMetaList.forEach(actionDocMeta -> {
            doCheck(actionDocMeta, result);
        });
    }

    protected void doCheck(ActionDocMeta actionDocMeta, JsonPolicyResult result) {
        _policyMap.forEach((key, value) -> {
            if (key.equals("methodMap")) {
                // @SuppressWarnings("unchecked")
                // final Map<String, Object> methodMap = (Map<String, Object>) value;
                // TODO yuto check method policy (2017/05/14)
            } else if (key.equals("fieldMap")) {
                // @SuppressWarnings("unchecked")
                // final Map<String, Object> fieldMap = (Map<String, Object>) value;
                // TODO yuto check field policy (2017/05/14)
            } else {
                if (!Srl.equalsPlain(key, "actionExceptList", "actionTargetList", "methodExceptMap", "fieldExceptMap")) {
                    // TODO yuto throw unknown Exception (2017/05/14)
                }
            }
        });
    }
}
