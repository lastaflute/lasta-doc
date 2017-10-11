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
package org.lastaflute.doc.meta;

import java.lang.annotation.Annotation;
import java.util.List;

import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.core.util.Lato;

/**
 * @author p1us2er0
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class TypeDocMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** name. */
    private String name;
    /** type. */
    private transient Class<?> type;
    /** type name. */
    private String typeName;
    /** simple type name. */
    private String simpleTypeName;
    /** generic type. */
    private transient Class<?> genericType;
    /** value. */
    private String value;
    /** description. */
    private String description;
    /** comment. */
    private String comment;
    /** annotation type list. */
    public transient List<Annotation> annotationTypeList;
    /** annotation list. */
    private List<String> annotationList = DfCollectionUtil.newArrayList();
    /** nest meta bean list. */
    private List<TypeDocMeta> nestTypeDocMetaList = DfCollectionUtil.newArrayList();

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return Lato.string(this);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getSimpleTypeName() {
        return simpleTypeName;
    }

    public void setSimpleTypeName(String simpleTypeName) {
        this.simpleTypeName = simpleTypeName;
    }

    public Class<?> getGenericType() {
        return genericType;
    }

    public void setGenericType(Class<?> genericType) {
        this.genericType = genericType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Annotation> getAnnotationTypeList() {
        return annotationTypeList;
    }

    public void setAnnotationTypeList(List<Annotation> annotationTypeList) {
        this.annotationTypeList = annotationTypeList;
    }

    public List<String> getAnnotationList() {
        return annotationList;
    }

    public void setAnnotationList(List<String> annotationList) {
        this.annotationList = annotationList;
    }

    public List<TypeDocMeta> getNestTypeDocMetaList() {
        return nestTypeDocMetaList;
    }

    public void setNestTypeDocMetaList(List<TypeDocMeta> nestTypeDocMetaList) {
        this.nestTypeDocMetaList = nestTypeDocMetaList;
    }
}
