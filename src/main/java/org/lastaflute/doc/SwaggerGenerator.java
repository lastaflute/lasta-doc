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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.constraints.Size;

import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfResourceUtil;
import org.dbflute.util.DfStringUtil;
import org.dbflute.util.DfTypeUtil;
import org.hibernate.validator.constraints.Length;
import org.lastaflute.core.direction.AccessibleConfig;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.json.SimpleJsonManager;
import org.lastaflute.core.json.engine.GsonJsonEngine;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.helper.misc.ParameterizedRef;
import org.lastaflute.di.util.tiger.Tuple3;
import org.lastaflute.doc.generator.ActionDocumentGenerator;
import org.lastaflute.doc.meta.TypeDocMeta;
import org.lastaflute.doc.util.LaDocReflectionUtil;
import org.lastaflute.doc.web.LaActionSwaggerable;
import org.lastaflute.web.util.LaRequestUtil;
import org.lastaflute.web.util.LaServletContextUtil;
import org.lastaflute.web.validation.Required;

/**
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerGenerator {

    // ===================================================================================
    //                                                                            Generate
    //                                                                            ========
    public void saveSwaggerMeta(LaActionSwaggerable swaggerable) {
        final String json = createJsonParser().toJson(swaggerable.json().getJsonResult());

        final Path path = Paths.get(getLastaDocDir(), "swagger.json");
        final Path parentPath = path.getParent();
        if (!Files.exists(parentPath)) {
            try {
                Files.createDirectories(parentPath);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directory: " + parentPath, e);
            }
        }

        try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.forName("UTF-8"))) {
            bw.write(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write the json to the file: " + path, e);
        }
    }

    public Map<String, Object> generateSwaggerMap() {
        OptionalThing<Map<String, Object>> swaggerJson = readSwaggerJson();
        if (swaggerJson.isPresent()) {
            return swaggerJson.get();
        }
        Map<String, Object> swaggerMap = createSwaggerMap();
        return swaggerMap;
    }

    public Map<String, Object> generateSwaggerMap(Consumer<SwaggerOption> op) {
        OptionalThing<Map<String, Object>> swaggerJson = readSwaggerJson();
        if (swaggerJson.isPresent()) {
            return swaggerJson.get();
        }
        SwaggerOption swaggerOption = new SwaggerOption();
        op.accept(swaggerOption);
        Map<String, Object> swaggerMap = createSwaggerMap();
        List<Map<String, Object>> headerParameterList = swaggerOption.getHeaderParameterList();
        adaptHeaderParameters(swaggerMap, headerParameterList);
        List<Map<String, Object>> securityDefinitionList = swaggerOption.getSecurityDefinitionList();
        adaptSecurityDefinitions(swaggerMap, securityDefinitionList);
        return swaggerMap;
    }

    // ===================================================================================
    //                                                                         Swagger Map
    //                                                                         ===========
    protected Map<String, Object> createSwaggerMap() {
        Map<String, Object> swaggerMap = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("swagger", "2.0");
        Map<String, String> swaggerInfoMap = createSwaggerInfoMap();
        swaggerMap.put("info", swaggerInfoMap);
        swaggerMap.put("schemes", Arrays.asList(LaRequestUtil.getRequest().getScheme()));

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

    protected OptionalThing<Map<String, Object>> readSwaggerJson() {
        String swaggerJsonFilePath = "./swagger.json";
        if (!DfResourceUtil.isExist(swaggerJsonFilePath)) {
            return OptionalThing.empty();
        }

        try (InputStream inputStream = DfResourceUtil.getResourceStream(swaggerJsonFilePath);
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String json = DfResourceUtil.readText(bufferedReader);
            return OptionalThing
                    .of(getJsonManager().fromJsonParameteried(json, new ParameterizedRef<Map<String, Object>>() {
                    }.getType()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the json to the file: " + swaggerJsonFilePath, e);
        }
    }

    protected Map<String, String> createSwaggerInfoMap() {
        Map<String, String> swaggerInfoMap = DfCollectionUtil.newLinkedHashMap();
        String domainName = getAccessibleConfig().get("domain.name");
        swaggerInfoMap.put("title", domainName);
        swaggerInfoMap.put("description", domainName);
        swaggerInfoMap.put("version", "1.0.0");
        return swaggerInfoMap;
    }

    // ===================================================================================
    //                                                                    Swagger Path Map
    //                                                                    ================
    protected void createSwaggerPathMap(List<Map<String, Object>> swaggerTagList, Map<String, Map<String, Object>> swaggerPathMap,
            Map<String, Map<String, Object>> swaggerDefinitionsMap) {
        createActionDocumentGenerator().generateActionDocMetaList().stream().forEach(actiondocMeta -> {
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
                    }).collect(Collectors.toMap(key -> key.get("name"), value -> value, (u, v) -> v, LinkedHashMap::new)));
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
            String tag = DfStringUtil.substringFirstFront(actiondocMeta.getUrl().replaceAll("^/", ""), "/");
            if (swaggerTagList.stream().noneMatch(swaggerTag -> swaggerTag.containsValue(tag))) {
                swaggerTagList.add(DfCollectionUtil.newLinkedHashMap("name", tag));
            }

            Map<String, Object> responseMap = DfCollectionUtil.newLinkedHashMap();
            swaggerHttpMethodMap.put("responses", responseMap);
            swaggerHttpMethodMap.put("produces", Arrays.asList("application/json"));

            Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
            schema.put("type", "object");
            schema.put("properties", actiondocMeta.getReturnTypeDocMeta().getNestTypeDocMetaList().stream().map(typeDocMeta -> {
                return createSwaggerParameterMap(typeDocMeta, swaggerDefinitionsMap);
            }).collect(Collectors.toMap(key -> key.get("name"), value -> value, (u, v) -> v, LinkedHashMap::new)));
            swaggerDefinitionsMap.put(actiondocMeta.getReturnTypeDocMeta().getTypeName(), schema);

            responseMap.put("200", DfCollectionUtil.newLinkedHashMap("description", "success", "schema",
                    DfCollectionUtil.newLinkedHashMap("$ref", "#/definitions/" + actiondocMeta.getReturnTypeDocMeta().getTypeName())));
            responseMap.put("400", DfCollectionUtil.newLinkedHashMap("description", "client error"));
        });
    }

    // ===================================================================================
    //                                                               Swagger Parameter Map
    //                                                               =====================
    protected Map<String, Object> createSwaggerParameterMap(TypeDocMeta typeDocMeta, Map<String, Map<String, Object>> definitionsMap) {
        Map<Class<?>, Tuple3<String, String, Function<Object, Object>>> typeMap = createTypeMap();

        Map<String, Object> swaggerParameterMap = DfCollectionUtil.newLinkedHashMap();
        swaggerParameterMap.put("name", typeDocMeta.getName());
        if (DfStringUtil.is_NotNull_and_NotEmpty(typeDocMeta.getDescription())) {
            swaggerParameterMap.put("description", typeDocMeta.getDescription());
        }
        // TODO p1us2er0 pri.B need to support @NotNull、@NotEmpty (2017/01/10)
        swaggerParameterMap.put("required",
                typeDocMeta.getAnnotationTypeList().stream().anyMatch(annotationType -> annotationType instanceof Required));
        if (typeMap.containsKey(typeDocMeta.getType())) {
            Tuple3<String, String, Function<Object, Object>> swaggerType = typeMap.get(typeDocMeta.getType());
            swaggerParameterMap.put("type", swaggerType.getValue1());
            String format = swaggerType.getValue2();
            if (DfStringUtil.is_NotNull_and_NotEmpty(format)) {
                swaggerParameterMap.put("format", format);
            }
        } else if (Iterable.class.isAssignableFrom(typeDocMeta.getType())) {
            swaggerParameterMap.put("type", "array");
            if (!typeDocMeta.getNestTypeDocMetaList().isEmpty()) {
                String definition = putDefinition(definitionsMap, typeDocMeta);
                swaggerParameterMap.put("items", DfCollectionUtil.newLinkedHashMap("$ref", definition));
            } else {
                Map<String, String> items = DfCollectionUtil.newLinkedHashMap();
                Class<?> genericType = typeDocMeta.getGenericType();
                if (genericType != null) {
                    Tuple3<String, String, Function<Object, Object>> swaggerType = typeMap.get(genericType);
                    if (swaggerType != null) {
                        items.put("type", swaggerType.getValue1());
                        String format = swaggerType.getValue2();
                        if (DfStringUtil.is_NotNull_and_NotEmpty(format)) {
                            items.put("format", format);
                        }
                    }
                }
                if (!items.containsKey("type")) {
                    items.put("type", "string");
                }
                swaggerParameterMap.put("items", items);
            }
        } else if (Map.class.isAssignableFrom(typeDocMeta.getType())) {
            swaggerParameterMap.put("type", "object");
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

        deriveDefaultValue(typeDocMeta).ifPresent(defaultValue -> {
            swaggerParameterMap.put("default", defaultValue);
        });
        return swaggerParameterMap;
    }

    protected String putDefinition(Map<String, Map<String, Object>> definitionsMap, TypeDocMeta typeDocMeta) {
        Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
        schema.put("type", "object");
        schema.put("properties", typeDocMeta.getNestTypeDocMetaList().stream().map(nestTypeDocMeta -> {
            return createSwaggerParameterMap(nestTypeDocMeta, definitionsMap);
        }).collect(Collectors.toMap(key -> key.get("name"), value -> value, (u, v) -> v, LinkedHashMap::new)));
        definitionsMap.put(typeDocMeta.getTypeName(), schema);
        return "#/definitions/" + typeDocMeta.getTypeName();
    }

    protected Map<Class<?>, Tuple3<String, String, Function<Object, Object>>> createTypeMap() {
        Map<Class<?>, Tuple3<String, String, Function<Object, Object>>> typeMap = DfCollectionUtil.newLinkedHashMap();
        typeMap.put(Integer.class, Tuple3.tuple3("integer", "int32", value -> DfTypeUtil.toInteger(value)));
        typeMap.put(Long.class, Tuple3.tuple3("integer", "int64", value -> DfTypeUtil.toLong(value)));
        typeMap.put(Float.class, Tuple3.tuple3("integer", "float", value -> DfTypeUtil.toFloat(value)));
        typeMap.put(Double.class, Tuple3.tuple3("integer", "double", value -> DfTypeUtil.toDouble(value)));
        typeMap.put(String.class, Tuple3.tuple3("string", null, value -> value));
        typeMap.put(byte[].class, Tuple3.tuple3("string", "byte", value -> value));
        typeMap.put(Boolean.class, Tuple3.tuple3("boolean", null, value -> DfTypeUtil.toBoolean(value)));
        TimeManager timeManager = getTimeManager();
        LocalDate currentDate = timeManager.currentDate();
        typeMap.put(LocalDate.class, Tuple3.tuple3("string", "date",
                value -> value == null ? getLocalDateFormatter().format(currentDate) : value));
        typeMap.put(LocalDateTime.class, Tuple3.tuple3("string", "date-time",
                value -> value == null ? getLocalDateTimeFormatter().format(currentDate.atStartOfDay()) : value));
        typeMap.put(int.class, Tuple3.tuple3("integer", "int32", value -> DfTypeUtil.toInteger(value)));
        typeMap.put(long.class, Tuple3.tuple3("integer", "int64", value -> DfTypeUtil.toLong(value)));
        typeMap.put(float.class, Tuple3.tuple3("integer", "float", value -> DfTypeUtil.toFloat(value)));
        typeMap.put(double.class, Tuple3.tuple3("integer", "double", value -> DfTypeUtil.toDouble(value)));
        return typeMap;
    }

    protected List<String> buildEnumValueList(Class<?> typeClass) {
        // cannot resolve type by maven compiler, explicitly cast it
        final List<String> valueList;
        if (Classification.class.isAssignableFrom(typeClass)) {
            @SuppressWarnings("unchecked")
            final Class<Classification> clsType = ((Class<Classification>) typeClass);
            valueList = Arrays.stream(clsType.getEnumConstants()).map(constant -> {
                return ((Classification) constant).code();
            }).collect(Collectors.toList());
        } else {
            Enum<?>[] constants = (Enum<?>[]) typeClass.getEnumConstants();
            valueList = Arrays.stream(constants).map(constant -> constants.toString()).collect(Collectors.toList());
        }
        return valueList;
    }

    protected void adaptHeaderParameters(Map<String, Object> swaggerMap, List<Map<String, Object>> headerParameterList) {
        if (headerParameterList.isEmpty()) {
            return;
        }
        final Object paths = swaggerMap.get("paths");
        if (!(paths instanceof Map<?, ?>)) {
            return;
        }
        @SuppressWarnings("unchecked")
        final Map<Object, Object> pathMap = (Map<Object, Object>) paths;
        pathMap.forEach((path, pathData) -> {
            if (!(pathData instanceof Map<?, ?>)) {
                return;
            }
            @SuppressWarnings("unchecked")
            final Map<Object, Object> pathDataMap = (Map<Object, Object>) pathData;

            headerParameterList.forEach(headerParameter -> {
                if (!pathDataMap.containsKey("parameters")) {
                    pathDataMap.put("parameters", DfCollectionUtil.newArrayList());
                }
                final Object parameters = pathDataMap.get("parameters");
                if (parameters instanceof List<?>) {
                    @SuppressWarnings("all")
                    final List<Object> parameterList = (List<Object>) parameters;
                    parameterList.add(headerParameter);
                }
            });
        });
    }

    protected void adaptSecurityDefinitions(Map<String, Object> swaggerMap, List<Map<String, Object>> securityDefinitionList) {
        final Map<Object, Object> securityDefinitions = DfCollectionUtil.newLinkedHashMap();
        final Map<Object, Object> security = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("securityDefinitions", securityDefinitions);
        swaggerMap.put("security", security);
        securityDefinitionList.forEach(securityDefinition -> {
            securityDefinitions.put(securityDefinition.get("name"), securityDefinition);
            security.put(securityDefinition.get("name"), Arrays.asList());
        });
    }

    // ===================================================================================
    //                                                                  Document Generator
    //                                                                  ==================
    protected DocumentGenerator createDocumentGenerator() {
        return new DocumentGenerator();
    }

    protected ActionDocumentGenerator createActionDocumentGenerator() {
        return createDocumentGenerator().createActionDocumentGenerator();
    }

    protected OptionalThing<String> prepareApplicationVersion() {
        return OptionalThing.empty();
    }

    protected DateTimeFormatter getLocalDateFormatter() {
        OptionalThing<DateTimeFormatter> localDateFormatter;
        JsonManager jsonManager = getJsonManager();
        if (jsonManager instanceof SimpleJsonManager) {
            localDateFormatter = LaDocReflectionUtil.getNoException(() -> {
                return ((SimpleJsonManager) jsonManager).getJsonMappingOption()
                        .flatMap(jsonMappingOption -> jsonMappingOption.getLocalDateFormatter());
            });
        } else {
            localDateFormatter = OptionalThing.empty();
        }
        return localDateFormatter.orElseGet(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    protected DateTimeFormatter getLocalDateTimeFormatter() {
        OptionalThing<DateTimeFormatter> localDateTimeFormatter;
        JsonManager jsonManager = getJsonManager();
        if (jsonManager instanceof SimpleJsonManager) {
            localDateTimeFormatter = LaDocReflectionUtil.getNoException(() -> {
                return ((SimpleJsonManager) jsonManager).getJsonMappingOption()
                        .flatMap(jsonMappingOption -> jsonMappingOption.getLocalDateTimeFormatter());
            });
        } else {
            localDateTimeFormatter = OptionalThing.empty();
        }
        return localDateTimeFormatter.orElseGet(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
    }

    protected OptionalThing<Object> deriveDefaultValue(TypeDocMeta typeDocMeta) {
        Map<Class<?>, Tuple3<String, String, Function<Object, Object>>> typeMap = createTypeMap();
        if (typeMap.containsKey(typeDocMeta.getType())) {
            Tuple3<String, String, Function<Object, Object>> swaggerType = typeMap.get(typeDocMeta.getType());
            Object defaultValue = swaggerType.getValue3().apply(deriveDefaultValueByComment(typeDocMeta.getComment()));
            if (defaultValue != null) {
                return OptionalThing.of(defaultValue);
            }
        } else if (Iterable.class.isAssignableFrom(typeDocMeta.getType()) && typeDocMeta.getNestTypeDocMetaList().isEmpty()) {
            Object defaultValue = deriveDefaultValueByComment(typeDocMeta.getComment());
            if (!(defaultValue instanceof List)) {
                return OptionalThing.empty();
            }
            @SuppressWarnings("unchecked")
            List<Object> defaultValueList = (List<Object>) defaultValue;
            Class<?> genericType = typeDocMeta.getGenericType();
            if (genericType == null) {
                genericType = String.class;
            }
            Tuple3<String, String, Function<Object, Object>> swaggerType = typeMap.get(genericType);
            if (swaggerType != null) {
                return OptionalThing.of(defaultValueList.stream().map(value -> {
                    return swaggerType.getValue3().apply(value);
                }).collect(Collectors.toList()));
            }
        } else if (Enum.class.isAssignableFrom(typeDocMeta.getType())) {
            Object defaultValue = deriveDefaultValueByComment(typeDocMeta.getComment());
            if (defaultValue != null) {
                return OptionalThing.of(defaultValue);
            } else {
                List<String> enumValueList = buildEnumValueList(typeDocMeta.getType());
                if (!enumValueList.isEmpty()) {
                    return OptionalThing.of(enumValueList.get(0));
                }
            }
        }
        return OptionalThing.empty();
    }

    protected Object deriveDefaultValueByComment(String comment) {
        if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
            if (comment.contains("e.g. \"")) {
                return DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(comment, "e.g. \""), "\"");
            }
            if (comment.contains("e.g. [")) {
                String defaultValue = DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(comment, "e.g. ["), "]");
                return Arrays.stream(defaultValue.split(", *")).map(value -> {
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        return value.substring(1, value.length() - 1);
                    }
                    return "null".equals(value) ? null : value;
                }).collect(Collectors.toList());
            }
            Pattern pattern = Pattern.compile(" e\\.g\\. ([^ ]+)");
            Matcher matcher = pattern.matcher(comment);
            if (matcher.find()) {
                String value = matcher.group(1);
                return "null".equals(value) ? null : value;
            }
        }
        return null;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected GsonJsonEngine createJsonParser() {
        return new DocumentGenerator().createJsonParser();
    }

    protected String getLastaDocDir() {
        return new DocumentGenerator().getLastaDocDir();
    }

    // ===================================================================================
    //                                                                           Component
    //                                                                           =========
    protected AccessibleConfig getAccessibleConfig() {
        return ContainerUtil.getComponent(AccessibleConfig.class);
    }

    protected TimeManager getTimeManager() {
        return ContainerUtil.getComponent(TimeManager.class);
    }

    protected JsonManager getJsonManager() {
        return ContainerUtil.getComponent(JsonManager.class);
    }
}
