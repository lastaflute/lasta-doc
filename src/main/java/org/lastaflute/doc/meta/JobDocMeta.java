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

import java.util.List;
import java.util.Map;

import org.lastaflute.core.util.Lato;

/**
 * @author p1us2er0
 * @since 0.6.9 (2075/03/05 Sunday)
 */
public class JobDocMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** jobKey. */
    private String jobKey;
    /** jobUnique. */
    private String jobUnique;
    /** jobTitle. */
    private String jobTitle;
    /** jobDescription. */
    private String jobDescription;
    /** cronExp. */
    private String cronExp;
    /** typeName. */
    private String typeName;
    /** simpleTypeName. */
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
    /** params. */
    private Map<String, Object> params;
    /** noticeLogLevel. */
    private String noticeLogLevel;
    /** concurrentExec. */
    private String concurrentExec;
    /** triggeredJobKeyList. */
    private List<String> triggeredJobKeyList;
    /** file line count. */
    private Integer fileLineCount;
    /** method line count. */
    private Integer methodLineCount;

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }

    public String getJobUnique() {
        return jobUnique;
    }

    public void setJobUnique(String jobUnique) {
        this.jobUnique = jobUnique;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getCronExp() {
        return cronExp;
    }

    public void setCronExp(String cronExp) {
        this.cronExp = cronExp;
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

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getNoticeLogLevel() {
        return noticeLogLevel;
    }

    public void setNoticeLogLevel(String noticeLogLevel) {
        this.noticeLogLevel = noticeLogLevel;
    }

    public String getConcurrentExec() {
        return concurrentExec;
    }

    public void setConcurrentExec(String concurrentExec) {
        this.concurrentExec = concurrentExec;
    }

    public List<String> getTriggeredJobKeyList() {
        return triggeredJobKeyList;
    }

    public void setTriggeredJobKeyList(List<String> triggeredJobKeyList) {
        this.triggeredJobKeyList = triggeredJobKeyList;
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

    @Override
    public String toString() {
        return Lato.string(this);
    }
}
