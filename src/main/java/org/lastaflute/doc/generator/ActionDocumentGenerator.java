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
package org.lastaflute.doc.generator;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfReflectionUtil.ReflectionFailureException;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.json.JsonMappingOption.JsonFieldNaming;
import org.lastaflute.core.json.SimpleJsonManager;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.ComponentDef;
import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.core.factory.SingletonLaContainerFactory;
import org.lastaflute.doc.meta.ActionDocMeta;
import org.lastaflute.doc.meta.TypeDocMeta;
import org.lastaflute.doc.reflector.SourceParserReflector;
import org.lastaflute.doc.util.LaDocReflectionUtil;
import org.lastaflute.web.Execute;
import org.lastaflute.web.UrlChain;
import org.lastaflute.web.path.ActionPathResolver;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.ruts.multipart.MultipartFormFile;
import org.lastaflute.web.util.LaModuleConfigUtil;

import com.google.gson.FieldNamingPolicy;

// package of this class should be under lastaflute but no fix for compatible
/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 of UTFlute (2015/09/18 Friday)
 */
public class ActionDocumentGenerator extends BaseDocumentGenerator {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** list of suppressed fields, e.g. enhanced fields by JaCoCo. */
    protected static final Set<String> SUPPRESSED_FIELD_SET;
    static {
        SUPPRESSED_FIELD_SET = DfCollectionUtil.newHashSet("$jacocoData");
    }

    protected static final List<String> TARGET_SUFFIX_LIST;
    static {
        TARGET_SUFFIX_LIST = Arrays.asList("Form", "Body", "Bean", "Result");
    }

    protected static final List<Class<?>> NATIVE_TYPE_LIST =
            Arrays.asList(void.class, boolean.class, byte.class, int.class, long.class, float.class, double.class, Void.class, Byte.class,
                    Boolean.class, Integer.class, Byte.class, Long.class, Float.class, Double.class, String.class, Map.class, byte[].class,
                    Byte[].class, Date.class, LocalDate.class, LocalDateTime.class, LocalTime.class, MultipartFormFile.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The list of source directory. (NotNull) */
    protected final List<String> srcDirList;

    /** depth of analyzed target, to avoid cyclic analyzing. */
    protected int depth; // #question depth count down? by jflute

    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionDocumentGenerator(List<String> srcDirList, int depth, OptionalThing<SourceParserReflector> sourceParserReflector) {
        this.srcDirList = srcDirList;
        this.depth = depth;
        this.sourceParserReflector = sourceParserReflector;
    }

    // ===================================================================================
    //                                                                            Generate
    //                                                                            ========
    public List<ActionDocMeta> generateActionDocMetaList() { // the list is per execute method
        final List<String> actionComponentNameList = findActionComponentNameList();
        final List<ActionDocMeta> metaList = DfCollectionUtil.newArrayList();
        final ModuleConfig moduleConfig = LaModuleConfigUtil.getModuleConfig();
        actionComponentNameList.forEach(componentName -> { // per action class
            moduleConfig.findActionMapping(componentName).alwaysPresent(actionMapping -> {
                final Class<?> actionClass = actionMapping.getActionDef().getComponentClass();
                final List<Method> methodList = DfCollectionUtil.newArrayList();
                sourceParserReflector.ifPresent(sourceParserReflector -> {
                    methodList.addAll(sourceParserReflector.getMethodListOrderByDefinition(actionClass));
                });
                if (methodList.isEmpty()) { // no java parser, use normal reflection
                    methodList.addAll(Arrays.stream(actionClass.getMethods()).sorted(Comparator.comparing(method -> {
                        return method.getName();
                    })).collect(Collectors.toList()));
                }
                methodList.forEach(method -> { // contains all methods
                    if (method.getAnnotation(Execute.class) != null) { // only execute method here
                        final ActionExecute actionExecute = actionMapping.getActionExecute(method);
                        if (actionExecute != null && !exceptsActionExecute(actionExecute)) {
                            final ActionDocMeta actionDocMeta = createActionDocMeta(actionExecute);
                            metaList.add(actionDocMeta);
                        }
                    }
                });
            });
        });
        return metaList;
    }

    protected boolean exceptsActionExecute(ActionExecute actionExecute) { // may be overridden
        if (suppressActionExecute(actionExecute)) { // for compatible
            return true;
        }
        return false;
    }

    @Deprecated
    protected boolean suppressActionExecute(ActionExecute actionExecute) {
        return false;
    }

    // ===================================================================================
    //                                                                      Action DocMeta
    //                                                                      ==============
    protected ActionDocMeta createActionDocMeta(ActionExecute execute) {
        final ActionDocMeta actionDocMeta = new ActionDocMeta();
        final Class<?> componentClass = execute.getActionMapping().getActionDef().getComponentClass();
        final UrlChain urlChain = new UrlChain(componentClass);
        final String urlPattern = execute.getPreparedUrlPattern().getResolvedUrlPattern();
        if (!"index".equals(urlPattern)) {
            urlChain.moreUrl(urlPattern);
        }

        // action item
        actionDocMeta.setUrl(getActionPathResolver().toActionUrl(componentClass, urlChain));

        // class item
        final Method method = execute.getExecuteMethod();
        final Class<?> methodDeclaringClass = method.getDeclaringClass(); // basically same as componentClass
        actionDocMeta.setType(methodDeclaringClass);
        actionDocMeta.setTypeName(adjustTypeName(methodDeclaringClass));
        actionDocMeta.setSimpleTypeName(adjustSimpleTypeName(methodDeclaringClass));

        // field item
        actionDocMeta.setFieldTypeDocMetaList(Arrays.stream(methodDeclaringClass.getDeclaredFields()).map(field -> {
            final TypeDocMeta typeDocMeta = new TypeDocMeta();
            typeDocMeta.setName(field.getName());
            typeDocMeta.setType(field.getType());
            typeDocMeta.setTypeName(adjustTypeName(field.getGenericType()));
            typeDocMeta.setSimpleTypeName(adjustSimpleTypeName((field.getGenericType())));
            typeDocMeta.setAnnotationTypeList(Arrays.asList(field.getAnnotations()));
            typeDocMeta.setAnnotationList(analyzeAnnotationList(typeDocMeta.getAnnotationTypeList()));

            sourceParserReflector.ifPresent(sourceParserReflector -> {
                sourceParserReflector.reflect(typeDocMeta, field.getType());
            });
            return typeDocMeta;
        }).collect(Collectors.toList()));

        // method item
        actionDocMeta.setMethodName(method.getName());

        // annotation item
        final List<Annotation> annotationList = DfCollectionUtil.newArrayList();
        annotationList.addAll(Arrays.asList(methodDeclaringClass.getAnnotations()));
        annotationList.addAll(Arrays.asList(method.getAnnotations()));
        actionDocMeta.setAnnotationTypeList(annotationList); // contains both action and execute method
        actionDocMeta.setAnnotationList(analyzeAnnotationList(annotationList));

        // in/out item (parameter, form, return)
        final List<TypeDocMeta> parameterTypeDocMetaList = Arrays.stream(method.getParameters()).filter(parameter -> {
            return !(execute.getFormMeta().isPresent() && execute.getFormMeta().get().getFormType().equals(parameter.getType()));
        }).map(parameter -> { // except form parameter here
            final StringBuilder builder = new StringBuilder();
            builder.append("{").append(parameter.getName()).append("}");
            actionDocMeta.setUrl(actionDocMeta.getUrl().replaceFirst("\\{\\}", builder.toString()));
            return analyzeMethodParameter(parameter);
        }).collect(Collectors.toList());
        actionDocMeta.setParameterTypeDocMetaList(parameterTypeDocMetaList);
        execute.getFormMeta().ifPresent(lastafluteFormMeta -> {
            actionDocMeta.setFormTypeDocMeta(analyzeFormClass(lastafluteFormMeta));
        });
        actionDocMeta.setReturnTypeDocMeta(analyzeReturnClass(method));

        // extension item (url, return, comment...)
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(actionDocMeta, method);
        });

        return actionDocMeta;
    }

    protected ActionPathResolver getActionPathResolver() {
        return ContainerUtil.getComponent(ActionPathResolver.class);
    }

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    // -----------------------------------------------------
    //                                     Analyze Parameter
    //                                     -----------------
    protected TypeDocMeta analyzeMethodParameter(Parameter parameter) {
        final TypeDocMeta parameterDocMeta = new TypeDocMeta();
        parameterDocMeta.setName(parameter.getName());
        parameterDocMeta.setType(parameter.getType());
        parameterDocMeta.setTypeName(adjustTypeName(parameter.getParameterizedType()));
        parameterDocMeta.setSimpleTypeName(adjustSimpleTypeName(parameter.getParameterizedType()));
        if (OptionalThing.class.isAssignableFrom(parameter.getType())) {
            parameterDocMeta.setGenericType(DfReflectionUtil.getGenericFirstClass(parameter.getParameterizedType()));
        }
        parameterDocMeta.setAnnotationTypeList(Arrays.asList(parameter.getAnnotatedType().getAnnotations()));
        parameterDocMeta.setAnnotationList(analyzeAnnotationList(parameterDocMeta.getAnnotationTypeList()));
        parameterDocMeta.setNestTypeDocMetaList(Collections.emptyList());
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(parameterDocMeta, parameter.getType());
        });
        return parameterDocMeta;
    }

    // -----------------------------------------------------
    //                                          Analyze Form
    //                                          ------------
    protected TypeDocMeta analyzeFormClass(ActionFormMeta lastafluteFormMeta) {
        final TypeDocMeta formDocMeta = new TypeDocMeta();
        lastafluteFormMeta.getListFormParameterParameterizedType().ifPresent(type -> {
            formDocMeta.setType(lastafluteFormMeta.getFormType());
            formDocMeta.setTypeName(adjustTypeName(type));
            formDocMeta.setSimpleTypeName(adjustSimpleTypeName(type));
        }).orElse(() -> {
            formDocMeta.setType(lastafluteFormMeta.getFormType());
            formDocMeta.setTypeName(adjustTypeName(lastafluteFormMeta.getFormType()));
            formDocMeta.setSimpleTypeName(adjustSimpleTypeName(lastafluteFormMeta.getFormType()));
        });
        final Class<?> formType = lastafluteFormMeta.getListFormParameterGenericType().orElse(lastafluteFormMeta.getFormType());
        final Map<String, Type> genericParameterTypesMap = DfCollectionUtil.newLinkedHashMap(); // #question can be emptyList()? by jflute
        final List<TypeDocMeta> propertyDocMetaList = analyzeProperties(formType, genericParameterTypesMap, depth);
        formDocMeta.setNestTypeDocMetaList(propertyDocMetaList);
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(formDocMeta, formType);
        });
        return formDocMeta;
    }

    // -----------------------------------------------------
    //                                        Analyze Return
    //                                        --------------
    protected TypeDocMeta analyzeReturnClass(Method method) {
        final TypeDocMeta returnDocMeta = new TypeDocMeta();
        returnDocMeta.setType(method.getReturnType());
        returnDocMeta.setTypeName(adjustTypeName(method.getGenericReturnType()));
        returnDocMeta.setSimpleTypeName(adjustSimpleTypeName(method.getGenericReturnType()));
        returnDocMeta.setGenericType(DfReflectionUtil.getGenericFirstClass(method.getGenericReturnType()));
        returnDocMeta.setAnnotationTypeList(Arrays.asList(method.getAnnotatedReturnType().getAnnotations()));
        returnDocMeta.setAnnotationList(analyzeAnnotationList(returnDocMeta.getAnnotationTypeList()));

        Class<?> returnClass = returnDocMeta.getGenericType();
        if (returnClass != null) { // e.g. List<String>, Sea<Land>
            // TODO p1us2er0 optimisation (2015/09/30)
            final Map<String, Type> genericParameterTypesMap = DfCollectionUtil.newLinkedHashMap();
            final Type[] parameterTypes = DfReflectionUtil.getGenericParameterTypes(method.getGenericReturnType());
            final TypeVariable<?>[] typeVariables = returnClass.getTypeParameters();
            IntStream.range(0, parameterTypes.length).forEach(parameterTypesIndex -> {
                final Type[] genericParameterTypes = DfReflectionUtil.getGenericParameterTypes(parameterTypes[parameterTypesIndex]);
                IntStream.range(0, typeVariables.length).forEach(typeVariablesIndex -> {
                    Type type = genericParameterTypes[typeVariablesIndex];
                    genericParameterTypesMap.put(typeVariables[typeVariablesIndex].getTypeName(), type);
                });
            });

            if (Iterable.class.isAssignableFrom(returnClass)) { // e.g. List<String>, List<Sea<Land>>
                returnClass = LaDocReflectionUtil.extractElementType(method.getGenericReturnType(), 1);
            }
            final List<Class<? extends Object>> nativeClassList = getNativeClassList();
            if (returnClass != null && !nativeClassList.contains(returnClass)) {
                final List<TypeDocMeta> propertyDocMetaList = analyzeProperties(returnClass, genericParameterTypesMap, depth);
                returnDocMeta.setNestTypeDocMetaList(propertyDocMetaList);
            }

            if (sourceParserReflector.isPresent()) {
                sourceParserReflector.get().reflect(returnDocMeta, returnClass);
            }
        }

        return returnDocMeta;
    }

    protected List<Class<?>> getNativeClassList() {
        return NATIVE_TYPE_LIST;
    }

    // -----------------------------------------------------
    //                                    Analyze Properties
    //                                    ------------------
    // for e.g. form type, return type, nested property type
    protected List<TypeDocMeta> analyzeProperties(Class<?> propertyOwner, Map<String, Type> genericParameterTypesMap, int depth) {
        if (depth < 0) {
            return DfCollectionUtil.newArrayList();
        }
        final Set<Field> fieldSet = extractWholeFieldSet(propertyOwner);
        return fieldSet.stream().filter(field -> { // also contains private fields and super's fields
            return !exceptsField(field);
        }).map(field -> { // #question contains private fields, right? by jflute
            return analyzePropertyField(propertyOwner, genericParameterTypesMap, depth, field);
        }).collect(Collectors.toList());
    }

    protected Set<Field> extractWholeFieldSet(Class<?> propertyOwner) {
        final Set<Field> fieldSet = DfCollectionUtil.newLinkedHashSet();
        for (Class<?> targetClazz = propertyOwner; targetClazz != Object.class; targetClazz = targetClazz.getSuperclass()) {
            if (targetClazz == null) { // e.g. interface: MultipartFormFile
                break;
            }
            fieldSet.addAll(Arrays.asList(targetClazz.getDeclaredFields()));
        }
        return fieldSet;
    }

    protected boolean exceptsField(Field field) { // e.g. special field and static field
        return SUPPRESSED_FIELD_SET.contains(field.getName()) || Modifier.isStatic(field.getModifiers());
    }

    // -----------------------------------------------------
    //                                Analyze Property Field
    //                                ----------------------
    protected TypeDocMeta analyzePropertyField(Class<?> propertyOwner, Map<String, Type> genericParameterTypesMap, int depth, Field field) {
        final TypeDocMeta meta = new TypeDocMeta();

        final Class<?> resolvedClass;
        {
            final Type resolvedType;
            {
                final Type genericClass = genericParameterTypesMap.get(field.getGenericType().getTypeName());
                resolvedType = genericClass != null ? genericClass : field.getType();
            }

            // basic item
            meta.setName(field.getName()); // also property name #question but overridden later, needed? by jflute
            meta.setType(field.getType()); // e.g. String, Integer, SeaPart #question not use resolvedType, right? by jflute
            meta.setTypeName(adjustTypeName(resolvedType));
            meta.setSimpleTypeName(adjustSimpleTypeName(resolvedType));

            // annotation item
            meta.setAnnotationTypeList(Arrays.asList(field.getAnnotations()));
            meta.setAnnotationList(analyzeAnnotationList(meta.getAnnotationTypeList()));

            // comment item (value expression)
            if (resolvedType instanceof Class) {
                resolvedClass = (Class<?>) resolvedType;
            } else {
                resolvedClass = (Class<?>) DfReflectionUtil.getGenericParameterTypes(resolvedType)[0];
            }
            if (resolvedClass.isEnum()) {
                meta.setValue(buildEnumValuesExp(resolvedClass)); // e.g. {FML = Formalized, PRV = Provisinal, ...}
            }
        }

        if (isTargetSuffixResolvedClass(resolvedClass)) { // nested bean of direct type as top or inner class
            // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
            // e.g.
            //  public SeaResult sea; // current field
            //
            //   or
            //
            //  public class SeaResult {
            //      public HangarPart hangar; // current field
            //      public static class HangarPart {
            //          ...
            //      }
            //  }
            // _/_/_/_/_/_/_/_/_/_/
            meta.setNestTypeDocMetaList(analyzeProperties(resolvedClass, genericParameterTypesMap, depth - 1));
        } else if (isTargetSuffixFieldGeneric(field)) { // nested bean of generic type as top or inner class
            // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
            // e.g.
            //  public List<SeaResult> seaList; // current field
            //
            //   or
            //
            //  public class SeaResult {
            //      public List<HangarPart> hangarList; // current field
            //      public static class HangarPart {
            //          ...
            //      }
            //  }
            // _/_/_/_/_/_/_/_/_/_/
            final Class<?> typeArgumentClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            meta.setNestTypeDocMetaList(analyzeProperties(typeArgumentClass, genericParameterTypesMap, depth - 1));

            // overriding type names that are already set before
            final String currentTypeName = meta.getTypeName();
            meta.setTypeName(adjustTypeName(currentTypeName) + "<" + adjustTypeName(typeArgumentClass) + ">");
            meta.setSimpleTypeName(adjustSimpleTypeName(currentTypeName) + "<" + adjustSimpleTypeName(typeArgumentClass) + ">");
        } else { // e.g. String, Integer, LocalDate, Sea<Mystic>
            // TODO p1us2er0 optimisation (2017/09/26)
            if (field.getGenericType().getTypeName().matches(".*<(.*)>")) { // e.g. Sea<Mystic>
                final String genericTypeName = field.getGenericType().getTypeName().replaceAll(".*<(.*)>", "$1");

                // generic item
                try {
                    meta.setGenericType(DfReflectionUtil.forName(genericTypeName));
                } catch (ReflectionFailureException ignored) {
                    meta.setGenericType(Object.class); // unknown
                }

                final Type genericClass = genericParameterTypesMap.get(genericTypeName);
                if (genericClass != null) {
                    meta.setNestTypeDocMetaList(analyzeProperties((Class<?>) genericClass, genericParameterTypesMap, depth - 1));

                    // overriding type names that are already set before
                    final String typeName = meta.getTypeName();
                    meta.setTypeName(adjustTypeName(typeName) + "<" + adjustTypeName(genericClass) + ">");
                    meta.setSimpleTypeName(adjustSimpleTypeName(typeName) + "<" + adjustSimpleTypeName(genericClass) + ">");
                } else {
                    // overriding type names that are already set before
                    final String typeName = meta.getTypeName();
                    meta.setTypeName(adjustTypeName(typeName) + "<" + adjustSimpleTypeName(genericTypeName) + ">"); // #question why simple? by jflute
                    meta.setSimpleTypeName(adjustSimpleTypeName(typeName) + "<" + adjustSimpleTypeName(genericTypeName) + ">");
                }
            }
        }

        // e.g. comment item (description, comment)
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(meta, propertyOwner);
        });

        // necessary to set it after parsing javadoc
        meta.setName(adjustFieldName(propertyOwner, field));
        return meta;
    }

    protected String buildEnumValuesExp(Class<?> typeClass) {
        // cannot resolve type by maven compiler, explicitly cast it
        final String valuesExp;
        if (Classification.class.isAssignableFrom(typeClass)) {
            @SuppressWarnings("unchecked")
            final Class<Classification> clsType = ((Class<Classification>) typeClass);
            valuesExp = Arrays.stream(clsType.getEnumConstants()).collect(Collectors.toMap(keyMapper -> {
                return ((Classification) keyMapper).code();
            }, valueMapper -> {
                return ((Classification) valueMapper).alias();
            }, (u, v) -> v, LinkedHashMap::new)).toString(); // e.g. {FML = Formalized, PRV = Provisinal, ...}
        } else {
            final Enum<?>[] constants = (Enum<?>[]) typeClass.getEnumConstants();
            valuesExp = Arrays.stream(constants).collect(Collectors.toList()).toString(); // e.g. [SEA, LAND, PIARI]
        }
        return valuesExp;
    }

    protected boolean isTargetSuffixResolvedClass(Class<?> resolvedClass) {
        // #question suffix but contains? by jflute
        // certainly true if AllSuffixResult$ResortParkPart
        // but Integer purchaseResultCount; is hit? 
        return getTargetTypeSuffixList().stream().anyMatch(suffix -> resolvedClass.getName().contains(suffix));
    }

    protected boolean isTargetSuffixFieldGeneric(Field field) {
        // #question suffix but contains? by jflute
        return getTargetTypeSuffixList().stream().anyMatch(suffix -> field.getGenericType().getTypeName().contains(suffix));
    }

    protected List<String> getTargetTypeSuffixList() {
        return TARGET_SUFFIX_LIST;
    }

    protected String adjustFieldName(Class<?> clazz, Field field) {
        // TODO p1us2er0 judge accurately (2017/04/20)
        if (clazz.getSimpleName().endsWith("Form")) {
            return field.getName();
        }
        final JsonManager jsonManager = ContainerUtil.getComponent(JsonManager.class);
        if (!(jsonManager instanceof SimpleJsonManager)) { // basically no way
            return field.getName();
        }
        final String fieldName = LaDocReflectionUtil.getNoException(() -> {
            return ((SimpleJsonManager) jsonManager).getJsonMappingOption().flatMap(jsonMappingOption -> {
                return jsonMappingOption.getFieldNaming().map(naming -> {
                    if (naming == JsonFieldNaming.IDENTITY) {
                        return FieldNamingPolicy.IDENTITY.translateName(field);
                    } else if (naming == JsonFieldNaming.CAMEL_TO_LOWER_SNAKE) {
                        return FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(field);
                    } else {
                        return field.getName();
                    }
                });
            }).orElse(null); // getNoException() cannot handle optional
        });
        return fieldName != null ? fieldName : field.getName();
    }

    // ===================================================================================
    //                                                                     Action Property
    //                                                                     ===============
    // #question who calls? by jflute
    public Map<String, Map<String, String>> generateActionPropertyNameMap(List<ActionDocMeta> actionDocMetaList) {
        final Map<String, Map<String, String>> propertyNameMap = actionDocMetaList.stream().collect(Collectors.toMap(key -> {
            return key.getUrl().replaceAll("\\{.*", "").replaceAll("/$", "").replaceAll("/", "_");
        }, value -> {
            return convertPropertyNameMap("", value.getFormTypeDocMeta());
        }, (u, v) -> v, LinkedHashMap::new));
        return propertyNameMap;
    }

    protected Map<String, String> convertPropertyNameMap(String parentName, TypeDocMeta typeDocMeta) {
        if (typeDocMeta == null) {
            return DfCollectionUtil.newLinkedHashMap();
        }

        final Map<String, String> propertyNameMap = DfCollectionUtil.newLinkedHashMap();

        final String name = calculateName(parentName, typeDocMeta.getName(), typeDocMeta.getTypeName());
        if (DfStringUtil.is_NotNull_and_NotEmpty(name)) {
            propertyNameMap.put(name, "");
        }

        if (typeDocMeta.getNestTypeDocMetaList() != null) {
            typeDocMeta.getNestTypeDocMetaList().forEach(nestDocMeta -> {
                propertyNameMap.putAll(convertPropertyNameMap(name, nestDocMeta));
            });
        }

        return propertyNameMap;
    }

    protected String calculateName(String parentName, String name, String type) {
        if (DfStringUtil.is_Null_or_Empty(name)) {
            return null;
        }

        final StringBuilder builder = new StringBuilder();
        if (DfStringUtil.is_NotNull_and_NotEmpty(parentName)) {
            builder.append(parentName + ".");
        }
        builder.append(name);
        if (name.endsWith("List")) {
            builder.append("[]");
        }

        return builder.toString();
    }

    // ===================================================================================
    //                                                                        DI Container
    //                                                                        ============
    protected List<String> findActionComponentNameList() {
        final List<String> componentNameList = DfCollectionUtil.newArrayList();
        final LaContainer container = getRootContainer();
        srcDirList.forEach(srcDir -> {
            if (!Paths.get(srcDir).toFile().exists()) {
                return;
            }
            try (Stream<Path> stream = Files.find(Paths.get(srcDir), Integer.MAX_VALUE, (path, attr) -> {
                return path.toString().endsWith("Action.java");
            })) {
                stream.forEach(path -> {
                    final String className = extractActionClassName(path, srcDir);
                    final Class<?> clazz = DfReflectionUtil.forName(className);
                    if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                        return;
                    }
                    final String componentName = container.getComponentDef(clazz).getComponentName();
                    if (componentName != null && !componentNameList.contains(componentName)) {
                        componentNameList.add(componentName);
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException("Failed to find the components: " + srcDir, e);
            }
        });
        IntStream.range(0, container.getComponentDefSize()).forEach(index -> {
            final ComponentDef componentDef = container.getComponentDef(index);
            final String componentName = componentDef.getComponentName();
            if (componentName.endsWith("Action") && !componentNameList.contains(componentName)) {
                componentNameList.add(componentDef.getComponentName());
            }
        });
        return componentNameList;
    }

    protected LaContainer getRootContainer() {
        return SingletonLaContainerFactory.getContainer().getRoot();
    }

    protected String extractActionClassName(Path path, String srcDir) { // for forName()
        String className = DfStringUtil.substringFirstRear(path.toFile().getAbsolutePath(), new File(srcDir).getAbsolutePath());
        if (className.startsWith(File.separator)) {
            className = className.substring(1);
        }
        className = DfStringUtil.substringLastFront(className, ".java").replace(File.separatorChar, '.');
        return className;
    }
}
