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
package org.lastaflute.doc.reflector;

import java.lang.reflect.Method;
import java.util.List;

import org.lastaflute.doc.meta.ActionDocMeta;
import org.lastaflute.doc.meta.JobDocMeta;
import org.lastaflute.doc.meta.TypeDocMeta;

/**
 * @author p1us2er0
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public interface SourceParserReflector {

    // ===================================================================================
    //                                                                             Reflect
    //                                                                             =======
    List<Method> getMethodListOrderByDefinition(Class<?> clazz);

    void reflect(ActionDocMeta actionDocMeta, Method method);

    void reflect(JobDocMeta jobDocMeta, Class<?> clazz);

    void reflect(TypeDocMeta typeDocMeta, Class<?> clazz);
}
