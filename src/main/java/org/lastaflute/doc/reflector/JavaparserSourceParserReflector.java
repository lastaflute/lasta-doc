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
package org.lastaflute.doc.reflector;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.doc.meta.ActionDocMeta;
import org.lastaflute.doc.meta.JobDocMeta;
import org.lastaflute.doc.meta.TypeDocMeta;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class JavaparserSourceParserReflector implements SourceParserReflector {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final Pattern CLASS_METHOD_COMMENT_END_PATTERN = Pattern.compile("(.+)[.。]?.*(\r?\n)?");
    protected static final Pattern FIELD_COMMENT_END_PATTERN = Pattern.compile("([^.。\\*]+).* ?\\*?");
    protected static final Pattern RETURN_STMT_PATTERN = Pattern.compile("^[^)]+\\)");

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** src dir list. (NotNull) */
    protected final List<String> srcDirList;

    /** cacheCompilationUnitMap. (NotNull) */
    protected final static Map<String, CacheCompilationUnit> CACHE_COMPILATION_UNIT_MAP = DfCollectionUtil.newHashMap();

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JavaparserSourceParserReflector(List<String> srcDirList) {
        this.srcDirList = srcDirList;
    }

    // ===================================================================================
    //                                                                         Method List
    //                                                                         ===========
    @Override
    public List<Method> getMethodListOrderByDefinition(Class<?> clazz) {
        List<String> methodDeclarationList = DfCollectionUtil.newArrayList();
        parseClass(clazz).ifPresent(compilationUnit -> {
            VoidVisitorAdapter<Void> adapter = new VoidVisitorAdapter<Void>() {
                public void visit(final MethodDeclaration methodDeclaration, final Void arg) {
                    methodDeclarationList.add(methodDeclaration.getNameAsString());
                    super.visit(methodDeclaration, arg);
                }
            };
            adapter.visit(compilationUnit, null);
        });

        List<Method> methodList = Arrays.stream(clazz.getMethods()).sorted(Comparator.comparing(method -> {
            return methodDeclarationList.indexOf(method.getName());
        })).collect(Collectors.toList());

        return methodList;
    }

    // ===================================================================================
    //                                                               Reflect ActionDocMeta
    //                                                               =====================
    @Override
    public void reflect(ActionDocMeta meta, Method method) {
        parseClass(method.getDeclaringClass()).ifPresent(compilationUnit -> {
            Map<String, List<String>> returnMap = DfCollectionUtil.newLinkedHashMap();
            VoidVisitorAdapter<ActionDocMeta> adapter = createActionDocMetaVisitorAdapter(method, returnMap);
            adapter.visit(compilationUnit, meta);
            List<String> descriptionList = DfCollectionUtil.newArrayList();
            Arrays.asList(meta.getTypeComment(), meta.getMethodComment()).forEach(comment -> {
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    Matcher matcher = CLASS_METHOD_COMMENT_END_PATTERN.matcher(comment);
                    if (matcher.find()) {
                        descriptionList.add(matcher.group(1));
                    }
                }
            });
            if (!descriptionList.isEmpty()) {
                meta.setDescription(String.join(", ", descriptionList));
            }
            List<TypeDocMeta> parameterTypeDocMetaList = meta.getParameterTypeDocMetaList();
            Parameter[] parameters = method.getParameters();
            for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
                if (parameterIndex < parameterTypeDocMetaList.size()) {
                    Parameter parameter = parameters[parameterIndex];
                    TypeDocMeta typeDocMeta = parameterTypeDocMetaList.get(parameterIndex);
                    meta.setUrl(meta.getUrl().replace("{" + parameter.getName() + "}", "{" + typeDocMeta.getName() + "}"));
                }
            }
            String methodName = method.getName();
            if (returnMap.containsKey(methodName) && !returnMap.get(methodName).isEmpty()) {
                meta.getReturnTypeDocMeta().setValue(String.join(",", returnMap.get(methodName)));
            }
        });
    }

    protected VoidVisitorAdapter<ActionDocMeta> createActionDocMetaVisitorAdapter(Method method, Map<String, List<String>> returnMap) {
        return new ActionDocMetaVisitorAdapter(method, returnMap);
    }

    public class ActionDocMetaVisitorAdapter extends VoidVisitorAdapter<ActionDocMeta> {

        protected final Method method;
        protected final Map<String, List<String>> returnMap;

        public ActionDocMetaVisitorAdapter(Method method, Map<String, List<String>> returnMap) {
            this.method = method;
            this.returnMap = returnMap;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ActionDocMeta actionDocMeta) {
            classOrInterfaceDeclaration.getBegin().ifPresent(begin -> {
                classOrInterfaceDeclaration.getEnd().ifPresent(end -> {
                    actionDocMeta.setFileLineCount(end.line - begin.line);
                });
            });
            String comment = adjustComment(classOrInterfaceDeclaration);
            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                actionDocMeta.setTypeComment(comment);
            }
            super.visit(classOrInterfaceDeclaration, actionDocMeta);
        }

        @Override
        public void visit(MethodDeclaration methodDeclaration, ActionDocMeta actionDocMeta) {
            if (!methodDeclaration.getNameAsString().equals(method.getName())) {
                return;
            }

            methodDeclaration.getBegin().ifPresent(begin -> {
                methodDeclaration.getEnd().ifPresent(end -> {
                    actionDocMeta.setMethodLineCount(end.line - begin.line);
                });
            });
            String comment = adjustComment(methodDeclaration);
            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                actionDocMeta.setMethodComment(comment);
            }
            IntStream.range(0, actionDocMeta.getParameterTypeDocMetaList().size()).forEach(parameterIndex -> {
                if (parameterIndex < methodDeclaration.getParameters().size()) {
                    TypeDocMeta typeDocMeta = actionDocMeta.getParameterTypeDocMetaList().get(parameterIndex);
                    com.github.javaparser.ast.body.Parameter parameter = methodDeclaration.getParameters().get(parameterIndex);
                    typeDocMeta.setName(parameter.getNameAsString());
                    if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                        // parse parameter comment
                        Pattern pattern = Pattern.compile(".*@param\\s?" + parameter.getNameAsString() + "\\s?(.*)\r?\n.*", Pattern.DOTALL);
                        Matcher matcher = pattern.matcher(comment);
                        if (matcher.matches()) {
                            typeDocMeta.setComment(matcher.group(1).replaceAll("\r?\n.*", ""));
                            typeDocMeta.setDescription(typeDocMeta.getComment().replaceAll(" ([^\\p{Alnum}]|e\\.g\\. )+.*", ""));
                        }
                    }
                }
            });

            methodDeclaration.accept(new VoidVisitorAdapter<ActionDocMeta>() {
                @Override
                public void visit(ReturnStmt returnStmt, ActionDocMeta actionDocMeta) {
                    prepareReturnStmt(methodDeclaration, returnStmt);
                    super.visit(returnStmt, actionDocMeta);
                }
            }, actionDocMeta);
            super.visit(methodDeclaration, actionDocMeta);
        }

        protected void prepareReturnStmt(MethodDeclaration methodDeclaration, ReturnStmt returnStmt) {
            returnStmt.getExpression().ifPresent(expression -> {
                String returnStmtStr = expression.toString();
                Matcher matcher = RETURN_STMT_PATTERN.matcher(returnStmtStr);
                if (!returnMap.containsKey(methodDeclaration.getNameAsString())) {
                    returnMap.put(methodDeclaration.getNameAsString(), DfCollectionUtil.newArrayList());
                }
                returnMap.get(methodDeclaration.getNameAsString()).add(matcher.find() ? matcher.group(0) : "##unanalyzable##");
            });
        }
    }

    // ===================================================================================
    //                                                                  Reflect JobDocMeta
    //                                                                  ==================
    @Override
    public void reflect(JobDocMeta jobDocMeta, Class<?> clazz) {
        parseClass(clazz).ifPresent(compilationUnit -> {
            VoidVisitorAdapter<JobDocMeta> adapter = createJobDocMetaVisitorAdapter();
            adapter.visit(compilationUnit, jobDocMeta);
            List<String> descriptionList = DfCollectionUtil.newArrayList();
            Arrays.asList(jobDocMeta.getTypeComment(), jobDocMeta.getMethodComment()).forEach(comment -> {
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    Matcher matcher = CLASS_METHOD_COMMENT_END_PATTERN.matcher(comment);
                    if (matcher.find()) {
                        descriptionList.add(matcher.group(1));
                    }
                }
            });
            if (!descriptionList.isEmpty()) {
                jobDocMeta.setDescription(String.join(", ", descriptionList));
            }
        });
    }

    protected VoidVisitorAdapter<JobDocMeta> createJobDocMetaVisitorAdapter() {
        return new JobDocMetaVisitorAdapter();
    }

    public class JobDocMetaVisitorAdapter extends VoidVisitorAdapter<JobDocMeta> {

        @Override
        public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, JobDocMeta jobDocMeta) {
            classOrInterfaceDeclaration.getBegin().ifPresent(begin -> {
                classOrInterfaceDeclaration.getEnd().ifPresent(end -> {
                    jobDocMeta.setFileLineCount(end.line - begin.line);
                });
            });
            String comment = adjustComment(classOrInterfaceDeclaration);
            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                jobDocMeta.setTypeComment(comment);
            }
            super.visit(classOrInterfaceDeclaration, jobDocMeta);
        }

        @Override
        public void visit(MethodDeclaration methodDeclaration, JobDocMeta jobDocMeta) {
            if (!methodDeclaration.getNameAsString().equals(jobDocMeta.getMethodName())) {
                return;
            }

            methodDeclaration.getBegin().ifPresent(begin -> {
                methodDeclaration.getEnd().ifPresent(end -> {
                    jobDocMeta.setMethodLineCount(end.line - begin.line);
                });
            });
            String comment = adjustComment(methodDeclaration);
            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                jobDocMeta.setMethodComment(comment);
            }
            super.visit(methodDeclaration, jobDocMeta);
        }
    }

    // ===================================================================================
    //                                                                 Reflect TypeDocMeta
    //                                                                 ===================
    @Override
    public void reflect(TypeDocMeta typeDocMeta, Class<?> clazz) {
        List<Class<?>> classList = DfCollectionUtil.newArrayList();
        for (Class<?> targetClass = clazz; targetClass != null; targetClass = targetClass.getSuperclass()) {
            classList.add(targetClass);
        }
        Collections.reverse(classList);
        classList.forEach(targetClass -> {
            parseClass(targetClass).ifPresent(compilationUnit -> {
                VoidVisitorAdapter<TypeDocMeta> adapter = createTypeDocMetaVisitorAdapter();
                adapter.visit(compilationUnit, typeDocMeta);
            });
        });
    }

    protected VoidVisitorAdapter<TypeDocMeta> createTypeDocMetaVisitorAdapter() {
        return new TypeDocMetaVisitorAdapter();
    }

    public class TypeDocMetaVisitorAdapter extends VoidVisitorAdapter<TypeDocMeta> {

        @Override
        public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDocMeta typeDocMeta) {
            prepareClassComment(classOrInterfaceDeclaration, typeDocMeta);
            super.visit(classOrInterfaceDeclaration, typeDocMeta);
        }

        protected void prepareClassComment(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDocMeta typeDocMeta) {
            if (DfStringUtil.is_Null_or_Empty(typeDocMeta.getComment())
                    && classOrInterfaceDeclaration.getNameAsString().equals(typeDocMeta.getSimpleTypeName())) {
                String comment = adjustComment(classOrInterfaceDeclaration);
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    typeDocMeta.setComment(comment);
                    if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                        Matcher matcher = CLASS_METHOD_COMMENT_END_PATTERN.matcher(comment);
                        if (matcher.find()) {
                            typeDocMeta.setDescription(matcher.group(1));
                        }
                    }
                }
            }
        }

        @Override
        public void visit(FieldDeclaration fieldDeclaration, TypeDocMeta typeDocMeta) {
            prepareFieldComment(fieldDeclaration, typeDocMeta);
            super.visit(fieldDeclaration, typeDocMeta);
        }

        protected void prepareFieldComment(FieldDeclaration fieldDeclaration, TypeDocMeta typeDocMeta) {
            if (fieldDeclaration.getVariables().stream().anyMatch(variable -> variable.getNameAsString().equals(typeDocMeta.getName()))) {
                String comment = adjustComment(fieldDeclaration);
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    typeDocMeta.setComment(comment);
                    Matcher matcher = FIELD_COMMENT_END_PATTERN.matcher(saveFieldCommentSpecialExp(comment));
                    if (matcher.find()) {
                        String description = matcher.group(1).trim();
                        typeDocMeta.setDescription(restoreFieldCommentSpecialExp(description));
                    }
                }
            }
        }

        protected String saveFieldCommentSpecialExp(String comment) {
            return comment.replace("e.g.", "$$edotgdot$$");
        }

        protected String restoreFieldCommentSpecialExp(String comment) {
            return comment.replace("$$edotgdot$$", "e.g.");
        }
    }

    // ===================================================================================
    //                                                                      Adjust Comment
    //                                                                      ==============
    protected String adjustComment(NodeWithJavadoc<?> nodeWithJavadoc) {
        try {
            return nodeWithJavadoc.getJavadoc().map(javadoc -> javadoc.toText().replaceAll("(^\r?\n|\r?\n$)", "")).orElse(null);
        } catch (Throwable t) {
            return "javadoc parse error. error messge=" + t.getMessage();
        }
    }

    // ===================================================================================
    //                                                                         Parse Class
    //                                                                         ===========
    protected OptionalThing<CompilationUnit> parseClass(Class<?> clazz) {
        for (String srcDir : srcDirList) {
            File file = new File(srcDir, clazz.getName().replace('.', File.separatorChar) + ".java");
            if (!file.exists()) {
                file = new File(srcDir, clazz.getName().replace('.', File.separatorChar).replaceAll("\\$.*", "") + ".java");
                if (!file.exists()) {
                    continue;
                }
            }
            if (CACHE_COMPILATION_UNIT_MAP.containsKey(clazz.getName())) {
                CacheCompilationUnit cacheCompilationUnit = CACHE_COMPILATION_UNIT_MAP.get(clazz.getName());
                if (cacheCompilationUnit != null && cacheCompilationUnit.fileLastModified == file.lastModified()
                        && cacheCompilationUnit.fileLength == file.length()) {
                    return OptionalThing.of(cacheCompilationUnit.compilationUnit);
                }
            }

            CacheCompilationUnit cacheCompilationUnit = new CacheCompilationUnit();
            cacheCompilationUnit.fileLastModified = file.lastModified();
            cacheCompilationUnit.fileLength = file.length();
            try {
                cacheCompilationUnit.compilationUnit = JavaParser.parse(file);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Source file don't exist.");
            }

            CACHE_COMPILATION_UNIT_MAP.put(clazz.getName(), cacheCompilationUnit);
            return OptionalThing.of(cacheCompilationUnit.compilationUnit);
        }
        return OptionalThing.ofNullable(null, () -> {
            throw new IllegalStateException("Source file don't exist.");
        });
    }

    /**
     * @author p1us2er0
     */
    private static class CacheCompilationUnit {

        /** file last modified. */
        private long fileLastModified;

        /** file length. */
        private long fileLength;

        /** compilation unit. */
        private CompilationUnit compilationUnit;
    }
}
