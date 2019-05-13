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
package org.lastaflute.doc.util;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.dbflute.utflute.core.PlainTestCase;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.web.response.JsonResponse;

/**
 * @author jflute
 */
public class LaDocReflectionUtilTest extends PlainTestCase {

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    public void test_extractElementType_basic() {
        // ## Arrange ##
        printRecursiveType(getMethod("mapSringAny", null).getGenericReturnType());
        printRecursiveType(getMethod("mapSringTWild", null).getGenericReturnType());
        printRecursiveType(getMethod("mapSringTLand", null).getGenericReturnType());

        // ## Act ##
        // ## Assert ##
        assertEquals(String.class, LaDocReflectionUtil.extractElementType(getMethod("listString", null).getGenericReturnType(), 1));
        assertEquals(SeaLandPiari.class,
                LaDocReflectionUtil.extractElementType(getMethod("listSeaLandPiari", null).getGenericReturnType(), 1));
        assertEquals(Land.class,
                LaDocReflectionUtil.extractElementType(getMethod("listSeaLand", null).getGenericReturnType(), 1));
        assertEquals(Map.class, LaDocReflectionUtil.extractElementType(getMethod("mapSringObject", null).getGenericReturnType(), 1));
        assertEquals(String.class, LaDocReflectionUtil.extractElementType(getMethod("mapSringObject", null).getGenericReturnType(), 2));
        assertEquals(Map.class, LaDocReflectionUtil.extractElementType(getMethod("mapSringAny", null).getGenericReturnType(), 1));
        assertEquals(Object.class, LaDocReflectionUtil.extractElementType(getMethod("mapSringAny", null).getGenericReturnType(), 2));
        assertEquals(Object.class, LaDocReflectionUtil.extractElementType(getMethod("mapSringTWild", null).getGenericReturnType(), 2));
        assertEquals(Land.class, LaDocReflectionUtil.extractElementType(getMethod("mapSringTLand", null).getGenericReturnType(), 2));
    }


    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected Method getMethod(String methodName, Class<?>[] argTypes) {
        return DfReflectionUtil.getAccessibleMethod(LaDocReflectionUtilTest.class, methodName, argTypes);
    }

    protected static void printRecursiveType(Type type) {
        if (type instanceof ParameterizedType) {
            for (Type type2 : ((ParameterizedType) type).getActualTypeArguments()) {
                if (type2 instanceof ParameterizedType) {
                    System.out.println("rawType=" + ((ParameterizedType) type2).getRawType());
                }
                System.out.println(type2 + "=" + type2.getClass().getName());
                printRecursiveType((type2));
            }
        }
    }

    // ===================================================================================
    //                                                                     Test Definition
    //                                                                     ===============
    public JsonResponse<List<String>> listString() {
        return null;
    }

    public class SeaLandPiari {

    }

    public JsonResponse<List<SeaLandPiari>> listSeaLandPiari() {
        return null;
    }

    public class Sea<T> {

    }

    public class Land {

    }

    public JsonResponse<Sea<Land>> listSeaLand() {
        return null;
    }

    public JsonResponse<List<Map<String, Object>>> mapSringObject() {
        return null;
    }

    public JsonResponse<List<Map<?, ?>>> mapSringAny() {
        return null;
    }

    public <T> JsonResponse<List<Map<T, T>>> mapSringTWild() {
        return null;
    }

    public <T extends Land> JsonResponse<List<Map<T, T>>> mapSringTLand() {
        return null;
    }
}
