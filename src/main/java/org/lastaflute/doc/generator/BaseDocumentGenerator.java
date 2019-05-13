/*
 * Copyright 2015-2019 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.dbflute.jdbc.Classification;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;

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
            final Class<? extends Annotation> annotationType = annotation.annotationType(); // e.g. @SeaPark
            final String typeName = adjustSimpleTypeName(annotationType); // e.g. SeaPark

            // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
            // e.g.
            //  public @interface SeaPark {
            //      String dockside() default "over";
            //      String hangar() default "mystic";
            //  }
            //
            //  @SeaPark(hangar="shadow")
            //  public String maihama;
            // _/_/_/_/_/_/_/_/_/_/
            // you can get method of concrete annotation by getDeclaredMethods()
            final Map<String, Object> methodMap = Arrays.stream(annotationType.getDeclaredMethods()).filter(method -> {
                final Object value = DfReflectionUtil.invoke(method, annotation, (Object[]) null); // e.g. shadow (of hangar)
                final Object defaultValue = method.getDefaultValue(); // e.g. mystic (of hangar)
                if (Objects.equals(value, defaultValue)) { // means non-specified attribute
                    return false;
                }
                if (method.getReturnType().isArray() && Arrays.equals((Object[]) value, (Object[]) defaultValue)) { // means non-specified attribute
                    return false;
                }
                return true; // specified attributes only here
            }).collect(Collectors.toMap(method -> {
                return method.getName();
            }, method -> {
                Object data = DfReflectionUtil.invoke(method, annotation, (Object[]) null); // e.g. shadow (of hangar)
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
            }, (u, v) -> v, LinkedHashMap::new));

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
            final Class<?> clazz = ((Class<?>) type);
            final String typeName;
            if (Classification.class.isAssignableFrom(clazz)) {
                typeName = Srl.replace(DfTypeUtil.toClassTitle(clazz), "CDef$", "CDef."); // e.g. CDef.MemberStatus
            } else {
                typeName = clazz.getSimpleName();
            }
            return typeName;
        } else {
            return adjustSimpleTypeName(adjustTypeName(type));
        }
    }

    protected String adjustSimpleTypeName(String typeName) {
        return typeName.replaceAll("[a-z0-9]+\\.", "");
    }
}
