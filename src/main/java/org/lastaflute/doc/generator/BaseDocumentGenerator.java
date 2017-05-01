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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.dbflute.util.DfReflectionUtil;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.6.9 (2075/03/05 Sunday)
 */
public class BaseDocumentGenerator {

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    // -----------------------------------------------------
    //                                    Analyze Annotation
    //                                    ------------------
    protected List<String> analyzeAnnotationList(List<Annotation> annotationList) {
        return annotationList.stream().map(annotation -> {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            final String typeName = adjustSimpleTypeName(annotationType);

            final Map<String, Object> methodMap = Arrays.stream(annotationType.getDeclaredMethods()).filter(method -> {
                final Object value = DfReflectionUtil.invoke(method, annotation, (Object[]) null);
                final Object defaultValue = method.getDefaultValue();
                if (Objects.equals(value, defaultValue)) {
                    return false;
                }
                if (method.getReturnType().isArray() && Arrays.equals((Object[]) value, (Object[]) defaultValue)) {
                    return false;
                }
                return true;
            }).collect(Collectors.toMap(key -> {
                return key.getName();
            }, value -> {
                Object data = DfReflectionUtil.invoke(value, annotation, (Object[]) null);
                if (data != null && data.getClass().isArray()) {
                    final List<?> dataList = Arrays.asList((Object[]) data);
                    if (dataList.isEmpty()) {
                        return "";
                    }
                    data = dataList.stream().map(o -> {
                        return o instanceof Class<?> ? adjustSimpleTypeName(((Class<?>) o)) : o;
                    }).collect(Collectors.toList());
                }
                return data;
            }, (v1, v2) -> v1, TreeMap::new));

            if (methodMap.isEmpty()) {
                return typeName;
            }
            return typeName + methodMap;
        }).collect(Collectors.toList());
    }

    // ===================================================================================
    //                                                                     Adjust TypeName
    //                                                                     ===============
    protected String adjustTypeName(Type type) {
        return adjustTypeName(type.getTypeName());
    }

    protected String adjustTypeName(String typeName) {
        return typeName;
    }

    protected String adjustSimpleTypeName(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).getSimpleName();
        }
        return adjustSimpleTypeName(adjustTypeName(type));
    }

    protected String adjustSimpleTypeName(String typeName) {
        return typeName.replaceAll("[a-z0-9]+\\.", "");
    }
}
