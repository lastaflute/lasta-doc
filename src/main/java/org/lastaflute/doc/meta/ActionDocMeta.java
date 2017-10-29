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

import org.lastaflute.core.util.Lato;

/**
 * The document meta of action, per execute method.
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class ActionDocMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                           Action Item
    //                                           -----------
    /** The url of the execute method. e.g. /sea/land/ (NotNull: after setup) */
    private String url;

    // -----------------------------------------------------
    //                                            Class Item
    //                                            ----------
    /** The type declaring the execute method. e.g. org.docksidestage.app.web.sea.SeaAction.class (NotNull: after setup) */
    private transient Class<?> type; // #question why transient? by jflute

    /** The full name of type declaring the execute method. e.g. "org.docksidestage.app.web.sea.SeaAction" (NotNull: after setup) */
    private String typeName;

    /** The simple name of type declaring the execute method. e.g. "SeaAction" (NotNull: after setup) */
    private String simpleTypeName;

    /** description. */
    private String description; // basically extracted by java parser

    /** type comment. */
    private String typeComment; // basically extracted by java parser

    // -----------------------------------------------------
    //                                            Field Item
    //                                            ----------
    /** The list of field meta, in method declaring class. (NotNull: after setup) */
    private List<TypeDocMeta> fieldTypeDocMetaList;

    // -----------------------------------------------------
    //                                           Method Item
    //                                           -----------
    /** The method name of action execute. e.g. org.docksidestage.app.web.sea.SeaAction@land() (NotNull: after setup) */
    private String methodName;

    /** The method comment of action execute. e.g. "Let's go to land" */
    private String methodComment; // basically extracted by java parser

    // -----------------------------------------------------
    //                                       Annotation Item
    //                                       ---------------
    /** The list of annotation type defined at both action and execute method. (Required) */
    public transient List<Annotation> annotationTypeList; // #question why transient, public? by jflute

    /** annotation list. */
    private List<String> annotationList; // #question what is this? by jflute

    // -----------------------------------------------------
    //                                           IN/OUT Item
    //                                           -----------
    /** parameter type doc meta list. */
    private List<TypeDocMeta> parameterTypeDocMetaList;

    /** form type doc meta. */
    private TypeDocMeta formTypeDocMeta;

    /** return type doc meta. */
    private TypeDocMeta returnTypeDocMeta;

    // -----------------------------------------------------
    //                                           Source Item
    //                                           -----------
    /** file line count. */
    private Integer fileLineCount; // basically extracted by java parser

    /** method line count. */
    private Integer methodLineCount; // basically extracted by java parser

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
    // -----------------------------------------------------
    //                                           Action Item
    //                                           -----------
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // -----------------------------------------------------
    //                                            Class Item
    //                                            ----------
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

    // -----------------------------------------------------
    //                                            Field Item
    //                                            ----------
    public List<TypeDocMeta> getFieldTypeDocMetaList() {
        return fieldTypeDocMetaList;
    }

    public void setFieldTypeDocMetaList(List<TypeDocMeta> fieldTypeDocMetaList) {
        this.fieldTypeDocMetaList = fieldTypeDocMetaList;
    }

    // -----------------------------------------------------
    //                                           Method Item
    //                                           -----------
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

    // -----------------------------------------------------
    //                                       Annotation Item
    //                                       ---------------
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

    // -----------------------------------------------------
    //                                           IN/OUT Item
    //                                           -----------
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

    // -----------------------------------------------------
    //                                           Source Item
    //                                           -----------
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
