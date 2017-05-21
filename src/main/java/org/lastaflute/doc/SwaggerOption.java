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
package org.lastaflute.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.dbflute.util.DfCollectionUtil;

/**
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<Map<String, Object>> headerParameterList = new ArrayList<>();
    protected final List<Map<String, Object>> securityDefinitionList = new ArrayList<>();

    // ===================================================================================
    //                                                                    Header Parameter
    //                                                                    ================
    public void addHeaderParameter(String name, String value) {
        headerParameterList.add(createHeaderParameterMap(name, value));
    }

    public void addHeaderParameter(String name, String value, Consumer<SwaggerHeaderParameterResource> resourceLambda) {
        final Map<String, Object> parameterMap = createHeaderParameterMap(name, value);
        resourceLambda.accept(new SwaggerHeaderParameterResource(parameterMap));
        headerParameterList.add(parameterMap);
    }

    protected Map<String, Object> createHeaderParameterMap(String name, String value) {
        final Map<String, Object> parameterMap = DfCollectionUtil.newLinkedHashMap();
        parameterMap.put("in", "header");
        parameterMap.put("type", "string");
        parameterMap.put("required", true);
        parameterMap.put("name", name);
        parameterMap.put("default", value);
        return parameterMap;
    }

    public static class SwaggerHeaderParameterResource {

        protected final Map<String, Object> headerParameterMap;

        public SwaggerHeaderParameterResource(Map<String, Object> headerParameterMap) {
            this.headerParameterMap = headerParameterMap;
        }

        public void registerAttribute(String key, Object value) {
            if (key == null) {
                throw new IllegalArgumentException("The argument 'key' should not be null.");
            }
            if (value == null) {
                throw new IllegalArgumentException("The argument 'value' should not be null.");
            }
            if (key.equalsIgnoreCase("name") || key.equalsIgnoreCase("default")) {
                throw new IllegalArgumentException("Cannot add '" + key + "' key here: " + key + ", " + value);
            }
            headerParameterMap.put(key, value);
        }
    }

    // ===================================================================================
    //                                                                 Security Definition
    //                                                                 ===================
    public void addSecurityDefinition(String name) {
        securityDefinitionList.add(createSecurityDefinitionMap(name));
    }

    public void addSecurityDefinition(String name, Consumer<SwaggerSecurityDefinitionResource> resourceLambda) {
        final Map<String, Object> definitionMap = createSecurityDefinitionMap(name);
        resourceLambda.accept(new SwaggerSecurityDefinitionResource(definitionMap));
        securityDefinitionList.add(definitionMap);
    }

    protected Map<String, Object> createSecurityDefinitionMap(String name) {
        final Map<String, Object> definitionMap = DfCollectionUtil.newLinkedHashMap();
        definitionMap.put("in", "header");
        definitionMap.put("type", "apiKey");
        definitionMap.put("name", name);
        return definitionMap;
    }

    public static class SwaggerSecurityDefinitionResource {

        protected final Map<String, Object> securityDefinitionMap;

        public SwaggerSecurityDefinitionResource(Map<String, Object> securityDefinitionMap) {
            this.securityDefinitionMap = securityDefinitionMap;
        }

        public void registerAttribute(String key, Object value) {
            if (key == null) {
                throw new IllegalArgumentException("The argument 'key' should not be null.");
            }
            if (value == null) {
                throw new IllegalArgumentException("The argument 'value' should not be null.");
            }
            if (key.equalsIgnoreCase("name")) {
                throw new IllegalArgumentException("Cannot add '" + key + "' key here: " + key + ", " + value);
            }
            securityDefinitionMap.put(key, value);
        }
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public List<Map<String, Object>> getHeaderParameterList() {
        return headerParameterList;
    }

    public List<Map<String, Object>> getSecurityDefinitionList() {
        return securityDefinitionList;
    }
}
