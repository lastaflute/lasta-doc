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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.config.ModuleConfig;
import org.lastaflute.web.util.LaModuleConfigUtil;

import com.google.gson.FieldNamingPolicy;

// package of this class should be under lastaflute but no fix for compatible
/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 (2015/09/18 Friday)
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

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** source directory. (NotNull) */
    protected final List<String> srcDirList;

    /** depth. */
    protected int depth;

    /** sourceParserReflector. */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    public ActionDocumentGenerator(List<String> srcDirList, int depth, OptionalThing<SourceParserReflector> sourceParserReflector) {
        this.srcDirList = srcDirList;
        this.depth = depth;
        this.sourceParserReflector = sourceParserReflector;
    }

    // -----------------------------------------------------
    //                                    Generate Meta List
    //                                    ------------------
    public List<ActionDocMeta> generateActionDocMetaList() {
        final List<String> actionComponentNameList = findActionComponentNameList();
        final List<ActionDocMeta> metaList = DfCollectionUtil.newArrayList();
        final ModuleConfig moduleConfig = LaModuleConfigUtil.getModuleConfig();
        actionComponentNameList.forEach(componentName -> {
            moduleConfig.findActionMapping(componentName).alwaysPresent(actionMapping -> {
                final Class<?> actionClass = actionMapping.getActionDef().getComponentClass();
                final List<Method> methodList = DfCollectionUtil.newArrayList();
                sourceParserReflector.ifPresent(sourceParserReflector -> {
                    methodList.addAll(sourceParserReflector.getMethodListOrderByDefinition(actionClass));
                });

                if (methodList.isEmpty()) {
                    methodList.addAll(Arrays.stream(actionClass.getMethods()).sorted(Comparator.comparing(method -> {
                        return method.getName();
                    })).collect(Collectors.toList()));
                }

                methodList.forEach(method -> {
                    if (method.getAnnotation(Execute.class) != null) {
                        ActionExecute actionExecute = actionMapping.getActionExecute(method);
                        if (actionExecute != null && !suppressActionExecute(actionExecute)) {
                            metaList.add(createActionDocMeta(actionMapping.getActionExecute(method)));
                        }
                    }
                });
            });
        });
        return metaList;
    }

    protected boolean suppressActionExecute(ActionExecute actionExecute) {
        return false;
    }

    // -----------------------------------------------------
    //                                 Find Action Component
    //                                 ---------------------
    protected List<String> findActionComponentNameList() {
        final List<String> componentNameList = DfCollectionUtil.newArrayList();
        final LaContainer container = SingletonLaContainerFactory.getContainer().getRoot();

        srcDirList.forEach(srcDir -> {
            if (Paths.get(srcDir).toFile().exists()) {
                try (Stream<Path> stream = Files.find(Paths.get(srcDir), Integer.MAX_VALUE, (path, attr) -> {
                    return path.toString().endsWith("Action.java");
                })) {
                    stream.forEach(path -> {
                        String className =
                                DfStringUtil.substringFirstRear(path.toFile().getAbsolutePath(), new File(srcDir).getAbsolutePath());
                        if (className.startsWith(File.separator)) {
                            className = className.substring(1);
                        }
                        className = DfStringUtil.substringLastFront(className, ".java").replace(File.separatorChar, '.');
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

    // -----------------------------------------------------
    //                                      Create ActionDoc
    //                                      ----------------
    protected ActionDocMeta createActionDocMeta(ActionExecute execute) {
        final Class<?> componentClass = execute.getActionMapping().getActionDef().getComponentClass();
        final ActionDocMeta actionDocMeta = new ActionDocMeta();
        final UrlChain urlChain = new UrlChain(componentClass);
        final String urlPattern = execute.getPreparedUrlPattern().getResolvedUrlPattern();
        if (!"index".equals(urlPattern)) {
            urlChain.moreUrl(urlPattern);
        }

        actionDocMeta.setUrl(getActionPathResolver().toActionUrl(componentClass, urlChain));
        final Method method = execute.getExecuteMethod();
        actionDocMeta.setType(method.getDeclaringClass());
        actionDocMeta.setTypeName(adjustTypeName(method.getDeclaringClass()));
        actionDocMeta.setSimpleTypeName(adjustSimpleTypeName(method.getDeclaringClass()));
        actionDocMeta.setFieldTypeDocMetaList(Arrays.stream(method.getDeclaringClass().getDeclaredFields()).map(field -> {
            TypeDocMeta typeDocMeta = new TypeDocMeta();
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
        actionDocMeta.setMethodName(method.getName());

        final List<Annotation> annotationList = DfCollectionUtil.newArrayList();
        annotationList.addAll(Arrays.asList(method.getDeclaringClass().getAnnotations()));
        annotationList.addAll(Arrays.asList(method.getAnnotations()));
        actionDocMeta.setAnnotationTypeList(annotationList);
        actionDocMeta.setAnnotationList(analyzeAnnotationList(annotationList));

        List<TypeDocMeta> parameterTypeDocMetaList = Arrays.stream(method.getParameters()).filter(parameter -> {
            return !(execute.getFormMeta().isPresent() && execute.getFormMeta().get().getFormType().equals(parameter.getType()));
        }).map(parameter -> {
            final StringBuilder builder = new StringBuilder();
            builder.append("{").append(parameter.getName()).append("}");
            actionDocMeta.setUrl(actionDocMeta.getUrl().replaceFirst("\\{\\}", builder.toString()));

            TypeDocMeta typeDocMeta = new TypeDocMeta();
            typeDocMeta.setName(parameter.getName());
            typeDocMeta.setType(parameter.getType());
            if (OptionalThing.class.isAssignableFrom(parameter.getType())) {
                typeDocMeta.setGenericType(DfReflectionUtil.getGenericFirstClass(parameter.getParameterizedType()));
            }
            typeDocMeta.setTypeName(adjustTypeName(parameter.getParameterizedType()));
            typeDocMeta.setSimpleTypeName(adjustSimpleTypeName(parameter.getParameterizedType()));
            typeDocMeta.setNestTypeDocMetaList(Collections.emptyList());
            typeDocMeta.setAnnotationTypeList(Arrays.asList(parameter.getAnnotatedType().getAnnotations()));
            typeDocMeta.setAnnotationList(analyzeAnnotationList(typeDocMeta.getAnnotationTypeList()));

            sourceParserReflector.ifPresent(sourceParserReflector -> {
                sourceParserReflector.reflect(typeDocMeta, parameter.getType());
            });

            return typeDocMeta;
        }).collect(Collectors.toList());
        actionDocMeta.setParameterTypeDocMetaList(parameterTypeDocMetaList);

        execute.getFormMeta().ifPresent(actionFormMeta -> {
            actionDocMeta.setFormTypeDocMeta(analyzeFormClass(actionFormMeta));
        });

        actionDocMeta.setReturnTypeDocMeta(analyzeReturnClass(method));

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
    //                                          Analyze Form
    //                                          ------------
    protected TypeDocMeta analyzeFormClass(ActionFormMeta actionFormMeta) {
        final TypeDocMeta typeDocMeta = new TypeDocMeta();
        actionFormMeta.getListFormParameterParameterizedType().ifPresent(type -> {
            typeDocMeta.setType(actionFormMeta.getFormType());
            typeDocMeta.setTypeName(adjustTypeName(type));
            typeDocMeta.setSimpleTypeName(adjustSimpleTypeName(type));
        }).orElse(() -> {
            typeDocMeta.setType(actionFormMeta.getFormType());
            typeDocMeta.setTypeName(adjustTypeName(actionFormMeta.getFormType()));
            typeDocMeta.setSimpleTypeName(adjustSimpleTypeName(actionFormMeta.getFormType()));
        });
        final Class<?> formType = actionFormMeta.getListFormParameterGenericType().orElse(actionFormMeta.getFormType());
        typeDocMeta.setNestTypeDocMetaList(prepareTypeDocMetaList(formType, DfCollectionUtil.newLinkedHashMap(), depth));
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(typeDocMeta, formType);
        });
        return typeDocMeta;
    }

    // -----------------------------------------------------
    //                                        Analyze Return
    //                                        --------------
    protected TypeDocMeta analyzeReturnClass(Method method) {
        final TypeDocMeta returnTypeDocMeta = new TypeDocMeta();
        returnTypeDocMeta.setType(method.getReturnType());
        returnTypeDocMeta.setTypeName(adjustTypeName(method.getGenericReturnType()));
        returnTypeDocMeta.setSimpleTypeName(adjustSimpleTypeName(method.getGenericReturnType()));
        returnTypeDocMeta.setGenericType(DfReflectionUtil.getGenericFirstClass(method.getGenericReturnType()));
        returnTypeDocMeta.setAnnotationTypeList(Arrays.asList(method.getAnnotatedReturnType().getAnnotations()));
        returnTypeDocMeta.setAnnotationList(analyzeAnnotationList(returnTypeDocMeta.getAnnotationTypeList()));

        Class<?> returnClass = returnTypeDocMeta.getGenericType();
        if (returnClass != null) {
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

            if (Iterable.class.isAssignableFrom(returnClass)) {
                final String returnClassName = returnTypeDocMeta.getTypeName().replaceAll(JsonResponse.class.getSimpleName() + "<(.*)>", "$1");
                final Matcher matcher = Pattern.compile(".+<([^,]+)>").matcher(returnClassName);
                if (matcher.matches()) {
                    final String genericClassName = matcher.group(1);
                    try {
                        returnClass = DfReflectionUtil.forName(genericClassName);
                    } catch (RuntimeException e) { // for matcher debug
                        String msg = "Not found the generic class: " + genericClassName + ", return=" + returnClassName;
                        throw new IllegalStateException(msg, e);
                    }
                }
            }
            final List<Class<? extends Object>> nativeClassList = getNativeClassList();
            if (returnClass != null && !nativeClassList.contains(returnClass)) {
                final List<TypeDocMeta> typeDocMeta = prepareTypeDocMetaList(returnClass, genericParameterTypesMap, depth);
                returnTypeDocMeta.setNestTypeDocMetaList(typeDocMeta);
            }

            if (sourceParserReflector.isPresent()) {
                sourceParserReflector.get().reflect(returnTypeDocMeta, returnClass);
            }
        }

        return returnTypeDocMeta;
    }

    protected List<Class<?>> getNativeClassList() {
        return Arrays.asList(Void.class, Integer.class, Long.class, Byte.class, String.class, Map.class);
    }

    // -----------------------------------------------------
    //                                       Prepare TypeDoc
    //                                       ---------------
    protected List<TypeDocMeta> prepareTypeDocMetaList(Class<?> clazz, Map<String, Type> genericParameterTypesMap, int depth) {
        if (depth < 0) {
            return DfCollectionUtil.newArrayList();
        }

        final Set<Field> fieldSet = DfCollectionUtil.newLinkedHashSet();
        for (Class<?> targetClazz = clazz; targetClazz != Object.class; targetClazz = targetClazz.getSuperclass()) {
            if (targetClazz == null) { // e.g. interface: MultipartFormFile
                break;
            }
            fieldSet.addAll(Arrays.asList(targetClazz.getDeclaredFields()));
        }
        return fieldSet.stream().filter(field -> {
            return !suppressField(field);
        }).map(field -> {
            return createTypeDocMeta(clazz, genericParameterTypesMap, depth, field);
        }).collect(Collectors.toList());
    }

    protected boolean suppressField(Field field) {
        return SUPPRESSED_FIELD_SET.contains(field.getName()) || Modifier.isStatic(field.getModifiers());
    }

    protected TypeDocMeta createTypeDocMeta(Class<?> clazz, Map<String, Type> genericParameterTypesMap, int depth, Field field) {
        Type genericClass = genericParameterTypesMap.get(field.getGenericType().getTypeName());
        final Type type = genericClass != null ? genericClass : field.getType();
        final TypeDocMeta meta = new TypeDocMeta();
        meta.setName(field.getName());
        meta.setType(field.getType());
        meta.setTypeName(adjustTypeName(type));
        meta.setSimpleTypeName(adjustSimpleTypeName(type));
        meta.setAnnotationTypeList(Arrays.asList(field.getAnnotations()));
        meta.setAnnotationList(analyzeAnnotationList(meta.getAnnotationTypeList()));
        final Class<?> typeClass = type instanceof Class ? (Class<?>) type : (Class<?>) DfReflectionUtil.getGenericParameterTypes(type)[0];
        if (typeClass.isEnum()) {
            meta.setValue(buildEnumValuesExp(typeClass));
        }

        final List<String> targetTypeSuffixNameList = getTargetTypeSuffixNameList();
        if (targetTypeSuffixNameList.stream().anyMatch(suffix -> typeClass.getName().contains(suffix))) {
            meta.setNestTypeDocMetaList(prepareTypeDocMetaList(typeClass, genericParameterTypesMap, depth - 1));
        } else if (targetTypeSuffixNameList.stream().anyMatch(suffix -> field.getGenericType().getTypeName().contains(suffix))) {
            Class<?> typeArgumentClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            meta.setNestTypeDocMetaList(prepareTypeDocMetaList(typeArgumentClass, genericParameterTypesMap, depth - 1));
            String typeName = meta.getTypeName();
            meta.setTypeName(adjustTypeName(typeName) + "<" + adjustTypeName(typeArgumentClass) + ">");
            meta.setSimpleTypeName(adjustSimpleTypeName(typeName) + "<" + adjustSimpleTypeName(typeArgumentClass) + ">");
        } else {
            // TODO p1us2er0 optimisation (2017/09/26)
            if (field.getGenericType().getTypeName().matches(".*<(.*)>")) {
                String genericTypeName = field.getGenericType().getTypeName().replaceAll(".*<(.*)>", "$1");
                try {
                    meta.setGenericType(DfReflectionUtil.forName(genericTypeName));
                } catch (ReflectionFailureException e) {
                    meta.setGenericType(Object.class);
                }
                genericClass = genericParameterTypesMap.get(genericTypeName);
                if (genericClass != null) {
                    meta.setNestTypeDocMetaList(prepareTypeDocMetaList((Class<?>) genericClass, genericParameterTypesMap, depth - 1));
                    String typeName = meta.getTypeName();
                    meta.setTypeName(adjustTypeName(typeName) + "<" + adjustTypeName(genericClass) + ">");
                    meta.setSimpleTypeName(adjustSimpleTypeName(typeName) + "<" + adjustSimpleTypeName(genericClass) + ">");
                } else {
                    String typeName = meta.getTypeName();
                    meta.setTypeName(adjustTypeName(typeName) + "<" + adjustSimpleTypeName(genericTypeName) + ">");
                    meta.setSimpleTypeName(adjustSimpleTypeName(typeName) + "<" + adjustSimpleTypeName(genericTypeName) + ">");
                }
            }
        }

        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(meta, clazz);
        });
        // necessary to set it after parsing javadoc
        meta.setName(adjustFieldName(clazz, field));
        return meta;
    }

    protected String adjustFieldName(Class<?> clazz, Field field) {
        // TODO p1us2er0 judge accurately (2017/04/20)
        if (clazz.getSimpleName().endsWith("Form")) {
            return field.getName();
        }
        JsonManager jsonManager = ContainerUtil.getComponent(JsonManager.class);
        if (!(jsonManager instanceof SimpleJsonManager)) {
            return field.getName();
        }
        String fieldName = LaDocReflectionUtil.getNoException(() -> {
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

    protected List<String> getTargetTypeSuffixNameList() {
        return DfCollectionUtil.newArrayList("Form", "Body", "Bean", "Result");
    }

    // ===================================================================================
    //                                                                     Action Property
    //                                                                     ===============
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
}
