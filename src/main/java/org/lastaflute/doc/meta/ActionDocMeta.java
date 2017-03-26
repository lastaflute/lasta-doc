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

/**
 * @author p1us2er0
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class ActionDocMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** url. */
    private String url;
    /** type. */
    private Class<?> type;
    /** type name. */
    private String typeName;
    /** simple type name. */
    private String simpleTypeName;
    /** description. */
    private String description;
    /** type comment. */
    private String typeComment;
    /** file type doc meta. */
    private List<TypeDocMeta> fieldTypeDocMetaList;
    /** method mame. */
    private String methodName;
    /** method comment. */
    private String methodComment;
    /** annotation type list. */
    private List<Annotation> annotationTypeList;
    /** annotation list. */
    private List<String> annotationList;
    /** parameter type doc meta list. */
    private List<TypeDocMeta> parameterTypeDocMetaList;
    /** form type doc meta. */
    private TypeDocMeta formTypeDocMeta;
    /** return type doc meta. */
    private TypeDocMeta returnTypeDocMeta;
    /** file line count. */
    private Integer fileLineCount;
    /** method line count. */
    private Integer methodLineCount;

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTypeComment() {
        return typeComment;
    }

    public void setTypeComment(String typeComment) {
        this.typeComment = typeComment;
    }

    public List<TypeDocMeta> getFieldTypeDocMetaList() {
        return fieldTypeDocMetaList;
    }

    public void setFieldTypeDocMetaList(List<TypeDocMeta> fieldTypeDocMetaList) {
        this.fieldTypeDocMetaList = fieldTypeDocMetaList;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodComment() {
        return methodComment;
    }

    public void setMethodComment(String methodComment) {
        this.methodComment = methodComment;
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

    public List<TypeDocMeta> getParameterTypeDocMetaList() {
        return parameterTypeDocMetaList;
    }

    public void setParameterTypeDocMetaList(List<TypeDocMeta> parameterTypeDocMetList) {
        this.parameterTypeDocMetaList = parameterTypeDocMetList;
    }

    public TypeDocMeta getFormTypeDocMeta() {
        return formTypeDocMeta;
    }

    public void setFormTypeDocMeta(TypeDocMeta formTypeDocMeta) {
        this.formTypeDocMeta = formTypeDocMeta;
    }

    public TypeDocMeta getReturnTypeDocMeta() {
        return returnTypeDocMeta;
    }

    public void setReturnTypeDocMeta(TypeDocMeta returnTypeDocMeta) {
        this.returnTypeDocMeta = returnTypeDocMeta;
    }

    public Integer getFileLineCount() {
        return fileLineCount;
    }

    public void setFileLineCount(Integer fileLineCount) {
        this.fileLineCount = fileLineCount;
    }

    public Integer getMethodLineCount() {
        return methodLineCount;
    }

    public void setMethodLineCount(Integer methodLineCount) {
        this.methodLineCount = methodLineCount;
    }
}
