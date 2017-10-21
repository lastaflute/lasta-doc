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
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.helper.misc.ParameterizedRef;
import org.lastaflute.di.util.tiger.Tuple3;
import org.lastaflute.doc.generator.ActionDocumentGenerator;
import org.lastaflute.doc.meta.ActionDocMeta;
import org.lastaflute.doc.meta.TypeDocMeta;
import org.lastaflute.doc.util.LaDocReflectionUtil;
import org.lastaflute.doc.web.LaActionSwaggerable;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.XmlResponse;
import org.lastaflute.web.ruts.multipart.MultipartFormFile;
import org.lastaflute.web.util.LaRequestUtil;
import org.lastaflute.web.util.LaServletContextUtil;
import org.lastaflute.web.validation.Required;

/**
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerGenerator {

    /* e.g. SwaggerAction implementation
    @AllowAnyoneAccess
    public class SwaggerAction extends FortressBaseAction {
    
        // ===================================================================================
        //                                                                           Attribute
        //                                                                           =========
        @Resource
        private RequestManager requestManager;
        @Resource
        private FortressConfig config;
    
        // ===================================================================================
        //                                                                             Execute
        //                                                                             =======
        @Execute
        public HtmlResponse index() {
            verifySwaggerAllowed();
            String swaggerJsonUrl = toActionUrl(SwaggerAction.class, moreUrl("json"));
            return new SwaggerAgent(requestManager).prepareSwaggerUiResponse(swaggerJsonUrl);
        }
    
        @Execute
        public JsonResponse<Map<String, Object>> json() {
            verifySwaggerAllowed();
            return asJson(new SwaggerGenerator().generateSwaggerMap());
        }
    
        private void verifySwaggerAllowed() { // also check in ActionAdjustmentProvider
            verifyOrClientError("Swagger is not enabled.", config.isSwaggerEnabled());
        }
    }
     */

    /* e.g. LastaDocTest implementation
    public class ShowbaseLastaDocTest extends UnitShowbaseTestCase {
    
        @Override
        protected String prepareMockContextPath() {
            return ShowbaseBoot.CONTEXT; // basically for swagger
        }
    
        public void test_document() throws Exception {
            saveLastaDocMeta();
        }
    
        public void test_swaggerJson() throws Exception {
            saveSwaggerMeta(new SwaggerAction());
        }
    }
     */

    // ===================================================================================
    //                                                                            Generate
    //                                                                            ========
    /**
     * Generate swagger map. (no option)
     * @return The map of swagger information. (NotNull)
     */
    public Map<String, Object> generateSwaggerMap() {
        return generateSwaggerMap(op -> {});
    }

    /**
     * Generate swagger map with option.
     * <pre>
     * new SwaggerGenerator().generateSwaggerMap(op -&gt; {
     *     op.deriveBasePath(basePath -&gt; basePath + "api/");
     * });
     * </pre>
     * @param opLambda The callback for settings of option. (NotNull)
     * @return The map of swagger information. (NotNull)
     */
    public Map<String, Object> generateSwaggerMap(Consumer<SwaggerOption> opLambda) {
        OptionalThing<Map<String, Object>> swaggerJson = readSwaggerJson();
        if (swaggerJson.isPresent()) {
            Map<String, Object> swaggerMap = swaggerJson.get();
            swaggerMap.put("schemes", Arrays.asList(LaRequestUtil.getRequest().getScheme()));
            return swaggerMap;
        }
        SwaggerOption swaggerOption = new SwaggerOption();
        opLambda.accept(swaggerOption);
        Map<String, Object> swaggerMap = createSwaggerMap(swaggerOption);
        return swaggerMap;
    }

    // ===================================================================================
    //                                                                               Save
    //                                                                              ======
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

    // ===================================================================================
    //                                                                   Read swagger.json
    //                                                                   =================
    protected OptionalThing<Map<String, Object>> readSwaggerJson() {
        String swaggerJsonFilePath = "./swagger.json";
        if (!DfResourceUtil.isExist(swaggerJsonFilePath)) {
            return OptionalThing.empty();
        }

        try (InputStream inputStream = DfResourceUtil.getResourceStream(swaggerJsonFilePath);
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String json = DfResourceUtil.readText(bufferedReader);
            return OptionalThing.of(getJsonManager().fromJsonParameteried(json, new ParameterizedRef<Map<String, Object>>() {
            }.getType()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the json to the file: " + swaggerJsonFilePath, e);
        }
    }

    protected Map<String, Object> createSwaggerMap(SwaggerOption swaggerOption) {
        Map<String, Object> swaggerMap = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("swagger", "2.0");
        Map<String, String> swaggerInfoMap = createSwaggerInfoMap();
        swaggerMap.put("info", swaggerInfoMap);
        swaggerMap.put("schemes", Arrays.asList(LaRequestUtil.getRequest().getScheme()));
        swaggerMap.put("basePath", derivedBasePath(swaggerOption));

        List<Map<String, Object>> swaggerTagList = DfCollectionUtil.newArrayList();
        swaggerMap.put("tags", swaggerTagList);

        swaggerOption.getHeaderParameterList().ifPresent(headerParameterList -> {
            adaptHeaderParameters(swaggerMap, headerParameterList);
        });
        swaggerOption.getSecurityDefinitionList().ifPresent(securityDefinitionList -> {
            adaptSecurityDefinitions(swaggerMap, securityDefinitionList);
        });
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

    protected String derivedBasePath(SwaggerOption swaggerOption) {
        StringBuilder basePath = new StringBuilder();
        basePath.append(LaServletContextUtil.getServletContext().getContextPath() + "/");
        prepareApplicationVersion().ifPresent(applicationVersion -> {
            basePath.append(applicationVersion + "/");
        });
        return swaggerOption.getDerivedBasePath().map(derivedBasePath -> {
            return derivedBasePath.apply(basePath.toString());
        }).orElse(basePath.toString());
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
                final Map<String, Object> parameterMap = toParameterMap(typeDocMeta, swaggerDefinitionsMap);
                parameterMap.put("in", "path");
                if (!OptionalThing.class.isAssignableFrom(typeDocMeta.getType())) {
                    parameterMap.put("required", true);
                }
                // TODO p1us2er0 Swagger path parameters are always required. (2017/10/12)
                // If path parameter is Option, define Path separately.
                // https://stackoverflow.com/questions/45549663/swagger-schema-error-should-not-have-additional-properties
                parameterMap.put("required", true);
                return parameterMap;
            }).collect(Collectors.toList()));

            if (actiondocMeta.getFormTypeDocMeta() != null) {
                if (actiondocMeta.getFormTypeDocMeta().getTypeName().endsWith("Form")) {
                    swaggerHttpMethodMap.put("consumes", Arrays.asList("application/x-www-form-urlencoded"));
                    parameterMapList.addAll(actiondocMeta.getFormTypeDocMeta().getNestTypeDocMetaList().stream().map(typeDocMeta -> {
                        final Map<String, Object> parameterMap = toParameterMap(typeDocMeta, swaggerDefinitionsMap);
                        parameterMap.put("name", typeDocMeta.getName());
                        parameterMap.put("in", "get".equals(httpMethod) ? "query" : "formData");
                        return parameterMap;
                    }).collect(Collectors.toList()));
                } else {
                    swaggerHttpMethodMap.put("consumes", Arrays.asList("application/json"));
                    Map<String, Object> parameterMap = DfCollectionUtil.newLinkedHashMap();
                    parameterMap.put("name", actiondocMeta.getFormTypeDocMeta().getSimpleTypeName());
                    parameterMap.put("in", "body");
                    parameterMap.put("required", true);
                    Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
                    schema.put("type", "object");
                    List<String> requiredPropertyNameList = derivedRequiredPropertyNameList(actiondocMeta.getFormTypeDocMeta());
                    if (!requiredPropertyNameList.isEmpty()) {
                        schema.put("required", requiredPropertyNameList);
                    }
                    schema.put("properties", actiondocMeta.getFormTypeDocMeta().getNestTypeDocMetaList().stream().map(typeDocMeta -> {
                        return toParameterMap(typeDocMeta, swaggerDefinitionsMap);
                    }).collect(Collectors.toMap(key -> key.get("name"), value -> {
                        LinkedHashMap<String, Object> property = DfCollectionUtil.newLinkedHashMap(value);
                        property.remove("name");
                        return property;
                    }, (u, v) -> v, LinkedHashMap::new)));

                    swaggerDefinitionsMap.put(derivedDefinitionName(actiondocMeta.getFormTypeDocMeta()), schema);
                    parameterMap.put("schema", DfCollectionUtil.newLinkedHashMap("$ref",
                            "#/definitions/" + derivedDefinitionName(actiondocMeta.getFormTypeDocMeta())));
                    parameterMapList.add(parameterMap);
                }
            }
            // Query, Header, Body, Form

            swaggerHttpMethodMap.put("parameters", parameterMapList);
            swaggerHttpMethodMap.put("tags",
                    Arrays.asList(DfStringUtil.substringFirstFront(actiondocMeta.getUrl().replaceAll("^/", ""), "/")));
            String tag = DfStringUtil.substringFirstFront(actiondocMeta.getUrl().replaceAll("^/", ""), "/");
            if (swaggerTagList.stream().noneMatch(swaggerTag -> swaggerTag.containsValue(tag))) {
                swaggerTagList.add(DfCollectionUtil.newLinkedHashMap("name", tag));
            }

            final Map<String, Object> responseMap = DfCollectionUtil.newLinkedHashMap();
            swaggerHttpMethodMap.put("responses", responseMap);
            derivedProduces(actiondocMeta).ifPresent(produces -> {
                swaggerHttpMethodMap.put("produces", produces);
            });
            final Map<String, Object> response = DfCollectionUtil.newLinkedHashMap("description", "success");
            final TypeDocMeta returnTypeDocMeta = actiondocMeta.getReturnTypeDocMeta();
            if (!Arrays.asList(void.class, Void.class).contains(returnTypeDocMeta.getGenericType())) {
                final Map<String, Object> parameterMap = toParameterMap(returnTypeDocMeta, swaggerDefinitionsMap);
                parameterMap.remove("name");
                parameterMap.remove("required");
                if (parameterMap.containsKey("schema")) {
                    response.putAll(parameterMap);
                } else {
                    response.put("schema", parameterMap);
                }
            }
            responseMap.put("200", response);
            responseMap.put("400", DfCollectionUtil.newLinkedHashMap("description", "client error"));
        });
    }

    // ===================================================================================
    //                                                                       Parameter Map
    //                                                                       =============
    protected Map<String, Object> toParameterMap(TypeDocMeta typeDocMeta, Map<String, Map<String, Object>> definitionsMap) {
        Map<Class<?>, Tuple3<String, String, Function<Object, Object>>> typeMap = createTypeMap();
        Class<?> keepType = typeDocMeta.getType();
        if (typeDocMeta.getGenericType() != null && (ActionResponse.class.isAssignableFrom(typeDocMeta.getType())
                || OptionalThing.class.isAssignableFrom(typeDocMeta.getType()))) {
            typeDocMeta.setType(typeDocMeta.getGenericType());
        }

        final Map<String, Object> parameterMap = DfCollectionUtil.newLinkedHashMap();
        parameterMap.put("name", typeDocMeta.getName());
        if (DfStringUtil.is_NotNull_and_NotEmpty(typeDocMeta.getDescription())) {
            parameterMap.put("description", typeDocMeta.getDescription());
        }
        // TODO p1us2er0 required process. (2017/10/12)
        // parameterMap.put("required", xxx);
        if (typeMap.containsKey(typeDocMeta.getType())) {
            final Tuple3<String, String, Function<Object, Object>> swaggerType = typeMap.get(typeDocMeta.getType());
            parameterMap.put("type", swaggerType.getValue1());
            final String format = swaggerType.getValue2();
            if (DfStringUtil.is_NotNull_and_NotEmpty(format)) {
                parameterMap.put("format", format);
            }
        } else if (Iterable.class.isAssignableFrom(typeDocMeta.getType())) {
            setupBeanList(typeDocMeta, definitionsMap, typeMap, parameterMap);
        } else if (typeDocMeta.getType().equals(Object.class) || Map.class.isAssignableFrom(typeDocMeta.getType())) {
            parameterMap.put("type", "object");
        } else if (!typeDocMeta.getNestTypeDocMetaList().isEmpty()) {
            String definition = putDefinition(definitionsMap, typeDocMeta);
            parameterMap.put("schema", DfCollectionUtil.newLinkedHashMap("$ref", definition));
        } else {
            parameterMap.put("type", "object");
            try {
                Class<?> clazz = DfReflectionUtil.forName(typeDocMeta.getTypeName());
                if (Enum.class.isAssignableFrom(clazz)) {
                    parameterMap.put("type", "string");
                    @SuppressWarnings("unchecked")
                    final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) clazz;
                    final List<Map<String, String>> enumMap = buildEnumMapList(enumClass);
                    parameterMap.put("enum", enumMap.stream().map(e -> e.get("code")).collect(Collectors.toList()));
                    String description = typeDocMeta.getDescription();
                    if (DfStringUtil.is_Null_or_Empty(description)) {
                        description = typeDocMeta.getName();
                    }
                    description += ":" + enumMap.stream().map(e -> {
                        return String.format(" * `%s` - %s, %s.", e.get("code"), e.get("name"), e.get("alias"));
                    }).collect(Collectors.joining());
                    parameterMap.put("description", description);
                }
            } catch (RuntimeException e) {
                // ignore
            }
        }

        typeDocMeta.getAnnotationTypeList().forEach(annotation -> {
            if (annotation instanceof Size) {
                final Size size = (Size) annotation;
                parameterMap.put("minimum", size.min());
                parameterMap.put("maximum", size.max());
            }
            if (annotation instanceof Length) {
                final Length length = (Length) annotation;
                parameterMap.put("minLength", length.min());
                parameterMap.put("maxLength", length.max());
            }
            // pattern, maxItems, minItems
        });

        deriveDefaultValue(typeDocMeta).ifPresent(defaultValue -> {
            parameterMap.put("default", defaultValue);
        });

        typeDocMeta.setType(keepType);
        return parameterMap;
    }

    protected void setupBeanList(TypeDocMeta typeDocMeta, Map<String, Map<String, Object>> definitionsMap,
            Map<Class<?>, Tuple3<String, String, Function<Object, Object>>> typeMap, Map<String, Object> schemaMap) {
        schemaMap.put("type", "array");
        if (!typeDocMeta.getNestTypeDocMetaList().isEmpty()) {
            String definition = putDefinition(definitionsMap, typeDocMeta);
            schemaMap.put("items", DfCollectionUtil.newLinkedHashMap("$ref", definition));
        } else {
            final Map<String, String> items = DfCollectionUtil.newLinkedHashMap();
            final Class<?> genericType = typeDocMeta.getGenericType();
            if (genericType != null) {
                final Tuple3<String, String, Function<Object, Object>> swaggerType = typeMap.get(genericType);
                if (swaggerType != null) {
                    items.put("type", swaggerType.getValue1());
                    final String format = swaggerType.getValue2();
                    if (DfStringUtil.is_NotNull_and_NotEmpty(format)) {
                        items.put("format", format);
                    }
                }
            }
            if (!items.containsKey("type")) {
                items.put("type", "object");
            }
            schemaMap.put("items", items);
        }
    }

    protected String putDefinition(Map<String, Map<String, Object>> definitionsMap, TypeDocMeta typeDocMeta) {
        Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
        schema.put("type", "object");
        List<String> requiredPropertyNameList = derivedRequiredPropertyNameList(typeDocMeta);
        if (!requiredPropertyNameList.isEmpty()) {
            schema.put("required", requiredPropertyNameList);
        }
        schema.put("properties", typeDocMeta.getNestTypeDocMetaList().stream().map(nestTypeDocMeta -> {
            return toParameterMap(nestTypeDocMeta, definitionsMap);
        }).collect(Collectors.toMap(key -> key.get("name"), value -> {
            // TODO p1us2er0 remove name. refactor required. (2017/10/12)
            LinkedHashMap<String, Object> property = DfCollectionUtil.newLinkedHashMap(value);
            property.remove("name");
            return property;
        }, (u, v) -> v, LinkedHashMap::new)));
        definitionsMap.put(derivedDefinitionName(typeDocMeta), schema);
        return "#/definitions/" + derivedDefinitionName(typeDocMeta);
    }

    protected List<String> derivedRequiredPropertyNameList(TypeDocMeta typeDocMeta) {
        return typeDocMeta.getNestTypeDocMetaList().stream().filter(nesttypeDocMeta -> {
            return nesttypeDocMeta.getAnnotationTypeList().stream().anyMatch(annotationType -> {
                return getRequiredAnnotationList().stream()
                        .anyMatch(requiredAnnotation -> requiredAnnotation.isAssignableFrom(annotationType.getClass()));
            });
        }).map(nesttypeDocMeta -> nesttypeDocMeta.getName()).collect(Collectors.toList());
    }

    protected String derivedDefinitionName(TypeDocMeta typeDocMeta) {
        if (typeDocMeta.getTypeName().matches("^[^<]+<(.+)>$")) {
            return typeDocMeta.getTypeName().replaceAll("^[^<]+<(.+)>$", "$1");
        }
        return typeDocMeta.getTypeName();
    }

    protected OptionalThing<List<String>> derivedProduces(ActionDocMeta actiondocMeta) {
        if (Arrays.asList(void.class, Void.class).contains(actiondocMeta.getReturnTypeDocMeta().getGenericType())) {
            return OptionalThing.empty();
        }
        if (createTypeMap().containsKey(actiondocMeta.getReturnTypeDocMeta().getGenericType())) {
            return OptionalThing.of(Arrays.asList("text/plain;charset=UTF-8"));
        }
        Map<Class<?>, List<String>> produceMap = DfCollectionUtil.newHashMap();
        produceMap.put(JsonResponse.class, Arrays.asList("application/json"));
        produceMap.put(XmlResponse.class, Arrays.asList("application/xml"));
        produceMap.put(HtmlResponse.class, Arrays.asList("text/html"));
        // TODO you LastaDoc, support StreamResponse's produce by jflute
        //produceMap.put(StreamResponse.class, "");
        Class<?> produceType = actiondocMeta.getReturnTypeDocMeta().getType();
        List<String> produceList = produceMap.get(produceType);
        return OptionalThing.ofNullable(produceList, () -> {
            String msg = "Not found the produce: type=" + produceType + ", keys=" + produceMap.keySet();
            throw new IllegalStateException(msg);
        });
    }

    protected Map<Class<?>, Tuple3<String, String, Function<Object, Object>>> createTypeMap() {
        Map<Class<?>, Tuple3<String, String, Function<Object, Object>>> typeMap = DfCollectionUtil.newLinkedHashMap();
        typeMap.put(boolean.class, Tuple3.tuple3("boolean", null, value -> DfTypeUtil.toBoolean(value)));
        typeMap.put(byte.class, Tuple3.tuple3("byte", null, value -> DfTypeUtil.toByte(value)));
        typeMap.put(int.class, Tuple3.tuple3("integer", "int32", value -> DfTypeUtil.toInteger(value)));
        typeMap.put(long.class, Tuple3.tuple3("integer", "int64", value -> DfTypeUtil.toLong(value)));
        typeMap.put(float.class, Tuple3.tuple3("integer", "float", value -> DfTypeUtil.toFloat(value)));
        typeMap.put(double.class, Tuple3.tuple3("integer", "double", value -> DfTypeUtil.toDouble(value)));
        typeMap.put(Boolean.class, Tuple3.tuple3("boolean", null, value -> DfTypeUtil.toBoolean(value)));
        typeMap.put(Byte.class, Tuple3.tuple3("boolean", null, value -> DfTypeUtil.toByte(value)));
        typeMap.put(Integer.class, Tuple3.tuple3("integer", "int32", value -> DfTypeUtil.toInteger(value)));
        typeMap.put(Long.class, Tuple3.tuple3("integer", "int64", value -> DfTypeUtil.toLong(value)));
        typeMap.put(Float.class, Tuple3.tuple3("integer", "float", value -> DfTypeUtil.toFloat(value)));
        typeMap.put(Double.class, Tuple3.tuple3("integer", "double", value -> DfTypeUtil.toDouble(value)));
        typeMap.put(String.class, Tuple3.tuple3("string", null, value -> value));
        typeMap.put(byte[].class, Tuple3.tuple3("string", "byte", value -> value));
        typeMap.put(Byte[].class, Tuple3.tuple3("string", "byte", value -> value));
        typeMap.put(Date.class,
                Tuple3.tuple3("string", "date", value -> value == null ? getLocalDateFormatter().format(getDefaultLocalDate()) : value));
        typeMap.put(LocalDate.class,
                Tuple3.tuple3("string", "date", value -> value == null ? getLocalDateFormatter().format(getDefaultLocalDate()) : value));
        typeMap.put(LocalDateTime.class, Tuple3.tuple3("string", "date-time",
                value -> value == null ? getLocalDateTimeFormatter().format(getDefaultLocalDateTime()) : value));
        typeMap.put(LocalTime.class,
                Tuple3.tuple3("string", null, value -> value == null ? getLocalTimeFormatter().format(getDefaultLocalTime()) : value));
        typeMap.put(MultipartFormFile.class, Tuple3.tuple3("file", null, value -> value));
        return typeMap;
    }

    protected List<Class<? extends Annotation>> getRequiredAnnotationList() {
        return Arrays.asList(Required.class, NotNull.class, NotEmpty.class);
    }

    protected List<Map<String, String>> buildEnumMapList(Class<? extends Enum<?>> typeClass) {
        // cannot resolve type by maven compiler, explicitly cast it
        final List<Map<String, String>> enumMapList = Arrays.stream(typeClass.getEnumConstants()).map(enumConstant -> {
            Map<String, String> map = DfCollectionUtil.newLinkedHashMap("name", enumConstant.name());
            if (enumConstant instanceof Classification) {
                map.put("code", ((Classification) enumConstant).code());
                map.put("alias", ((Classification) enumConstant).alias());
            } else {
                map.put("code", enumConstant.name());
                map.put("alias", "");
            }
            return map;
        }).collect(Collectors.toList());
        return enumMapList;
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

    protected LocalDate getDefaultLocalDate() {
        return LocalDate.ofYearDay(2000, 1);
    }

    protected LocalDateTime getDefaultLocalDateTime() {
        return getDefaultLocalDate().atStartOfDay();
    }

    protected LocalTime getDefaultLocalTime() {
        return LocalTime.from(getDefaultLocalDateTime());
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

    protected DateTimeFormatter getLocalTimeFormatter() {
        OptionalThing<DateTimeFormatter> localDateTimeFormatter;
        JsonManager jsonManager = getJsonManager();
        if (jsonManager instanceof SimpleJsonManager) {
            localDateTimeFormatter = LaDocReflectionUtil.getNoException(() -> {
                return ((SimpleJsonManager) jsonManager).getJsonMappingOption()
                        .flatMap(jsonMappingOption -> jsonMappingOption.getLocalTimeFormatter());
            });
        } else {
            localDateTimeFormatter = OptionalThing.empty();
        }
        return localDateTimeFormatter.orElseGet(() -> DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
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
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) typeDocMeta.getType();
                List<Map<String, String>> enumMapList = buildEnumMapList(enumClass);
                return OptionalThing.migratedFrom(enumMapList.stream().map(e -> (Object) e.get("code")).findFirst(), () -> {
                    throw new IllegalStateException("not found enum value.");
                });
            }
        }
        return OptionalThing.empty();
    }

    protected Object deriveDefaultValueByComment(String comment) {
        if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
            String commentWithoutLine = comment.replaceAll("\r?\n", " ");
            if (commentWithoutLine.contains(" e.g. \"")) {
                return DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(commentWithoutLine, " e.g. \""), "\"");
            }
            if (commentWithoutLine.contains(" e.g. [")) {
                String defaultValue = DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(commentWithoutLine, " e.g. ["), "]");
                return Arrays.stream(defaultValue.split(", *")).map(value -> {
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        return value.substring(1, value.length() - 1);
                    }
                    return "null".equals(value) ? null : value;
                }).collect(Collectors.toList());
            }
            Pattern pattern = Pattern.compile(" e\\.g\\. ([^ ]+)");
            Matcher matcher = pattern.matcher(commentWithoutLine);
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

    protected JsonManager getJsonManager() {
        return ContainerUtil.getComponent(JsonManager.class);
    }
}
