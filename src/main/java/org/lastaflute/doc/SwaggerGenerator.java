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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.constraints.Size;

import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfStringUtil;
import org.hibernate.validator.constraints.Length;
import org.lastaflute.core.direction.AccessibleConfig;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.di.util.tiger.Tuple3;
import org.lastaflute.doc.meta.TypeDocMeta;
import org.lastaflute.web.util.LaRequestUtil;
import org.lastaflute.web.util.LaServletContextUtil;
import org.lastaflute.web.validation.Required;

/**
 * @author p1us2er0
 */
public class SwaggerGenerator {

    public Map<String, Object> generateSwaggerMap() {
        Map<String, Object> swaggerMap = createSwaggerMap();
        return swaggerMap;
    }

    protected Map<String, Object> createSwaggerMap() {
        Map<String, Object> swaggerMap = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("swagger", "2.0");
        Map<String, String> swaggerInfoMap = createSwaggerInfoMap();
        swaggerMap.put("info", swaggerInfoMap);
        swaggerMap.put("host", LaRequestUtil.getRequest().getHeader("host"));
        swaggerMap.put("schemes", Arrays.asList(LaRequestUtil.getRequest().getProtocol()));

        StringBuilder basePath = new StringBuilder();
        basePath.append(LaServletContextUtil.getServletContext().getContextPath() + "/");
        prepareApplicationVersion().ifPresent(applicationVersion -> {
            basePath.append(applicationVersion + "/");
        });
        swaggerMap.put("basePath", basePath.toString());

        List<Map<String, Object>> swaggerTagList = DfCollectionUtil.newArrayList();
        swaggerMap.put("tags", swaggerTagList);

        Map<String, Map<String, Object>> swaggerPathMap = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("paths", swaggerPathMap);

        Map<String, Map<String, Object>> swaggerDefinitionsMap = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("definitions", swaggerDefinitionsMap);

        createSwaggerPathMap(swaggerTagList, swaggerPathMap, swaggerDefinitionsMap);
        return swaggerMap;
    }

    protected Map<String, String> createSwaggerInfoMap() {
        Map<String, String> swaggerInfoMap = DfCollectionUtil.newLinkedHashMap();
        String domainName = getAccessibleConfig().get("domain.name");
        swaggerInfoMap.put("title", domainName);
        swaggerInfoMap.put("description", domainName);
        swaggerInfoMap.put("version", "1.0.0");
        return swaggerInfoMap;
    }

    protected void createSwaggerPathMap(List<Map<String, Object>> swaggerTagList, Map<String, Map<String, Object>> swaggerPathMap,
            Map<String, Map<String, Object>> swaggerDefinitionsMap) {
        new DocumentGenerator().createActionDocumentGenerator().generateActionDocMetaList().stream().forEach(actiondocMeta -> {
            if (!swaggerPathMap.containsKey(actiondocMeta.getUrl())) {
                Map<String, Object> swaggerUrlMap = DfCollectionUtil.newLinkedHashMap();
                swaggerPathMap.put(actiondocMeta.getUrl(), swaggerUrlMap);
            }
            Map<String, Object> swaggerUrlMap = swaggerPathMap.get(actiondocMeta.getUrl());
            Map<String, Object> swaggerHttpMethodMap = DfCollectionUtil.newLinkedHashMap();
            Pattern pattern = Pattern.compile("(.+)\\$.+");
            Matcher matcher = pattern.matcher(actiondocMeta.getMethodName());
            String httpMethod = matcher.find() ? matcher.group(1) : "post";
            swaggerUrlMap.put(httpMethod, swaggerHttpMethodMap);

            swaggerHttpMethodMap.put("summary", actiondocMeta.getDescription());
            swaggerHttpMethodMap.put("description", actiondocMeta.getDescription());

            List<Map<String, Object>> parameterMapList = DfCollectionUtil.newArrayList();

            parameterMapList.addAll(actiondocMeta.getParameterTypeDocMetaList().stream().map(typeDocMeta -> {
                Map<String, Object> swaggerParameterMap = createSwaggerParameterMap(typeDocMeta, swaggerDefinitionsMap);
                swaggerParameterMap.put("in", "path");
                swaggerParameterMap.put("required", !OptionalThing.class.isAssignableFrom(typeDocMeta.getType()));
                return swaggerParameterMap;
            }).collect(Collectors.toList()));

            if (actiondocMeta.getFormTypeDocMeta() != null) {
                if (actiondocMeta.getFormTypeDocMeta().getTypeName().endsWith("Form")) {
                    swaggerHttpMethodMap.put("consumes", Arrays.asList("text/plain;charset=utf-8"));
                    parameterMapList.addAll(actiondocMeta.getFormTypeDocMeta().getNestTypeDocMetaList().stream().map(typeDocMeta -> {
                        Map<String, Object> swaggerParameterMap = createSwaggerParameterMap(typeDocMeta, swaggerDefinitionsMap);
                        swaggerParameterMap.put("in", "get".equals(httpMethod) ? "query" : "formData");
                        return swaggerParameterMap;
                    }).collect(Collectors.toList()));
                } else {
                    swaggerHttpMethodMap.put("consumes", Arrays.asList("application/json"));
                    Map<String, Object> swaggerParameterMap = DfCollectionUtil.newLinkedHashMap();
                    swaggerParameterMap.put("name", actiondocMeta.getFormTypeDocMeta().getSimpleTypeName());
                    swaggerParameterMap.put("in", "body");
                    swaggerParameterMap.put("required", true);
                    Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
                    schema.put("type", "object");
                    schema.put("properties", actiondocMeta.getFormTypeDocMeta().getNestTypeDocMetaList().stream().map(typeDocMeta -> {
                        return createSwaggerParameterMap(typeDocMeta, swaggerDefinitionsMap);
                    }).collect(Collectors.toMap(key -> key.get("name"), value -> value)));
                    swaggerDefinitionsMap.put(actiondocMeta.getFormTypeDocMeta().getTypeName(), schema);
                    swaggerParameterMap.put("schema",
                            DfCollectionUtil.newLinkedHashMap("$ref", "#/definitions/" + actiondocMeta.getFormTypeDocMeta().getTypeName()));
                    parameterMapList.add(swaggerParameterMap);
                }
            }
            // Query、Header、Body、Form

            swaggerHttpMethodMap.put("parameters", parameterMapList);
            swaggerHttpMethodMap.put("tags",
                    Arrays.asList(DfStringUtil.substringFirstFront(actiondocMeta.getUrl().replaceAll("^/", ""), "/")));
            swaggerTagList.add(DfCollectionUtil.newLinkedHashMap("name",
                    DfStringUtil.substringFirstFront(actiondocMeta.getUrl().replaceAll("^/", ""), "/")));

            Map<String, Object> responseMap = DfCollectionUtil.newLinkedHashMap();
            swaggerHttpMethodMap.put("responses", responseMap);
            swaggerHttpMethodMap.put("produces", Arrays.asList("application/json"));

            Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
            schema.put("type", "object");
            schema.put("properties", actiondocMeta.getReturnTypeDocMeta().getNestTypeDocMetaList().stream().map(typeDocMeta -> {
                return createSwaggerParameterMap(typeDocMeta, swaggerDefinitionsMap);
            }).collect(Collectors.toMap(key -> key.get("name"), value -> value)));
            swaggerDefinitionsMap.put(actiondocMeta.getReturnTypeDocMeta().getTypeName(), schema);

            responseMap.put("200", DfCollectionUtil.newLinkedHashMap("description", "success", "schema",
                    DfCollectionUtil.newLinkedHashMap("$ref", "#/definitions/" + actiondocMeta.getReturnTypeDocMeta().getTypeName())));
            responseMap.put("400", DfCollectionUtil.newLinkedHashMap("description", "client error"));
        });
    }

    protected Map<String, Object> createSwaggerParameterMap(TypeDocMeta typeDocMeta, Map<String, Map<String, Object>> definitionsMap) {
        Map<Class<?>, Tuple3<String, String, String>> typeMap = createTypeMap();

        Map<String, Object> swaggerParameterMap = DfCollectionUtil.newLinkedHashMap();
        swaggerParameterMap.put("name", typeDocMeta.getName());
        if (DfStringUtil.is_NotNull_and_NotEmpty(typeDocMeta.getDescription())) {
            swaggerParameterMap.put("description", typeDocMeta.getDescription());
        }
        // TODO p1us2er0 need to support @NotNull、@NotEmpty (2017/01/10)
        swaggerParameterMap.put("required",
                typeDocMeta.getAnnotationTypeList().stream().anyMatch(annotationType -> annotationType instanceof Required));
        if (typeMap.containsKey(typeDocMeta.getType())) {
            Tuple3<String, String, String> swaggerType = typeMap.get(typeDocMeta.getType());
            swaggerParameterMap.put("type", swaggerType.getValue1());
            String format = swaggerType.getValue2();
            if (DfStringUtil.is_NotNull_and_NotEmpty(format)) {
                swaggerParameterMap.put("format", format);
            }
            if (DfStringUtil.is_NotNull_and_NotEmpty(swaggerType.getValue3())) {
                swaggerParameterMap.put("default", swaggerType.getValue3());
            }
        } else if (Iterable.class.isAssignableFrom(typeDocMeta.getType())) {
            swaggerParameterMap.put("type", "array");
            if (!typeDocMeta.getNestTypeDocMetaList().isEmpty()) {
                String definition = putDefinition(definitionsMap, typeDocMeta);
                swaggerParameterMap.put("items", DfCollectionUtil.newLinkedHashMap("$ref", definition));
            } else {
                swaggerParameterMap.put("items", DfCollectionUtil.newLinkedHashMap("type", "string"));
            }
        } else if (!typeDocMeta.getNestTypeDocMetaList().isEmpty()) {
            String definition = putDefinition(definitionsMap, typeDocMeta);
            swaggerParameterMap.put("schema", DfCollectionUtil.newLinkedHashMap("$ref", definition));
        } else {
            swaggerParameterMap.put("type", "string");
            try {
                Class<?> clazz = DfReflectionUtil.forName(typeDocMeta.getTypeName());
                if (Enum.class.isAssignableFrom(clazz)) {
                    swaggerParameterMap.put("enum", buildEnumValueList(clazz));
                }
            } catch (RuntimeException e) {
                // ignore
            }
        }

        typeDocMeta.getAnnotationTypeList().forEach(annotation -> {
            if (annotation instanceof Size) {
                Size size = (Size) annotation;
                swaggerParameterMap.put("minimum", size.min());
                swaggerParameterMap.put("maximum", size.max());
            }
            if (annotation instanceof Length) {
                Length length = (Length) annotation;
                swaggerParameterMap.put("minLength", length.min());
                swaggerParameterMap.put("maxLength", length.max());
            }
            // pattern, maxItems, minItems
        });

        return swaggerParameterMap;
    }

    protected String putDefinition(Map<String, Map<String, Object>> definitionsMap, TypeDocMeta typeDocMeta) {
        Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
        schema.put("type", "object");
        schema.put("properties", typeDocMeta.getNestTypeDocMetaList().stream().map(nestTypeDocMeta -> {
            return createSwaggerParameterMap(nestTypeDocMeta, definitionsMap);
        }).collect(Collectors.toMap(key -> key.get("name"), value -> value)));
        definitionsMap.put(typeDocMeta.getTypeName(), schema);
        return "#/definitions/" + typeDocMeta.getTypeName();
    }

    protected Map<Class<?>, Tuple3<String, String, String>> createTypeMap() {
        Map<Class<?>, Tuple3<String, String, String>> typeMap = DfCollectionUtil.newLinkedHashMap();
        typeMap.put(Integer.class, Tuple3.tuple3("integer", "int32", null));
        typeMap.put(Long.class, Tuple3.tuple3("integer", "int64", null));
        typeMap.put(Float.class, Tuple3.tuple3("integer", "float", null));
        typeMap.put(Double.class, Tuple3.tuple3("integer", "double", null));
        typeMap.put(String.class, Tuple3.tuple3("string", null, null));
        typeMap.put(byte[].class, Tuple3.tuple3("string", "byte", null));
        typeMap.put(Boolean.class, Tuple3.tuple3("boolean", null, null));
        typeMap.put(String.class, Tuple3.tuple3("string", null, null));
        // TODO p1us2er0 Guide format from JsonMappingOption (2017/01/10)
        TimeManager timeManager = getTimeManager();
        typeMap.put(LocalDate.class,
                Tuple3.tuple3("string", "date", DateTimeFormatter.ofPattern("yyyy-MM-dd").format(timeManager.currentDate())));
        typeMap.put(LocalDateTime.class, Tuple3.tuple3("string", "date-time",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").format(timeManager.currentDateTime())));
        typeMap.put(int.class, Tuple3.tuple3("integer", "int32", null));
        typeMap.put(long.class, Tuple3.tuple3("integer", "int64", null));
        typeMap.put(float.class, Tuple3.tuple3("integer", "float", null));
        typeMap.put(double.class, Tuple3.tuple3("integer", "double", null));
        return typeMap;
    }

    protected List<String> buildEnumValueList(Class<?> typeClass) {
        // cannot resolve type by maven compiler, explicitly cast it
        final List<String> valueList;
        if (Classification.class.isAssignableFrom(typeClass)) {
            @SuppressWarnings("unchecked")
            final Class<Classification> clsType = ((Class<Classification>) typeClass);
            valueList = Arrays.stream(clsType.getEnumConstants()).map(constant -> {
                return ((Classification) constant).code() + ((Classification) constant).alias();
            }).collect(Collectors.toList());
        } else {
            Enum<?>[] constants = (Enum<?>[]) typeClass.getEnumConstants();
            valueList = Arrays.stream(constants).map(constant -> constants.toString()).collect(Collectors.toList());
        }
        return valueList;
    }

    protected OptionalThing<String> prepareApplicationVersion() {
        return OptionalThing.empty();
    }

    protected AccessibleConfig getAccessibleConfig() {
        return ContainerUtil.getComponent(AccessibleConfig.class);
    }

    protected TimeManager getTimeManager() {
        return SingletonLaContainerFactory.getContainer().getComponent(TimeManager.class);
    }
}
