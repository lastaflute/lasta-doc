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

import org.dbflute.util.DfCollectionUtil;

/**
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerOption {

    protected final List<Map<String, Object>> headerParameterList = new ArrayList<>();
    protected final List<Map<String, Object>> securityDefinitionList = new ArrayList<>();

    public void addHeaderParameter(Map<String, Object> headerParameter) {
        headerParameterList.add(headerParameter);
    }

    public List<Map<String, Object>> getHeaderParameterList() {
        return headerParameterList;
    }

    public void addSecurityDefinition(Map<String, Object> securityDefinition) {
        securityDefinitionList.add(securityDefinition);
    }

    public List<Map<String, Object>> getSecurityDefinitionList() {
        return securityDefinitionList;
    }

    // TODO jflute createHeaderParameter、createSecurityDefinitionの定義位置迷っています。 by p1us2er0 (2017/05/20)
    public Map<String, Object> createHeaderParameter(String name, String value) {
        final Map<String, Object> headerParameter = DfCollectionUtil.newLinkedHashMap();
        headerParameter.put("in", "header");
        headerParameter.put("type", "string");
        headerParameter.put("required", true);
        headerParameter.put("name", name);
        headerParameter.put("default", value);
        return headerParameter;
    }

    public Map<String, Object> createSecurityDefinition(String name) {
        final Map<String, Object> securityDefinition = DfCollectionUtil.newLinkedHashMap();
        securityDefinition.put("in", "header");
        securityDefinition.put("type", "apiKey");
        securityDefinition.put("name", name);
        return securityDefinition;
    }
}
