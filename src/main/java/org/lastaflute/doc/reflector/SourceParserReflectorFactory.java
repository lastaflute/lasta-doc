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

import java.util.List;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfReflectionUtil.ReflectionFailureException;
import org.lastaflute.doc.agent.maven.MavenVersionFinder;
import org.dbflute.util.DfStringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class SourceParserReflectorFactory {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger _log = LoggerFactory.getLogger(SourceParserReflectorFactory.class);
    private static final String JAVA_PARSER_CLASS_NAME = "com.github.javaparser.JavaParser";
    private static final float JAVA_PARSER_VERSION = 3.1f;

    // ===================================================================================
    //                                                                           Reflector
    //                                                                           =========
    public OptionalThing<SourceParserReflector> reflector(List<String> srcDirList) { // empty allowed if not found
        final String className = JAVA_PARSER_CLASS_NAME;
        SourceParserReflector reflector = null;
        try {
            DfReflectionUtil.forName(className);
            _log.debug("...Loading java parser for document: {}", className);
            validateVersion(JAVA_PARSER_VERSION);
            reflector = createJavaparserSourceParserReflector(srcDirList);
        } catch (ReflectionFailureException ignored) {
            reflector = null;
        }
        return OptionalThing.ofNullable(reflector, () -> {
            throw new IllegalStateException("Not found the java parser: " + className);
        });
    }

    public void validateVersion(float leastVersion) {
        getJavaparserVersion().ifPresent(version -> {
            _log.debug("...Loading java parser for version: {}", version);
            String majorMinorVersionStr = version.replaceAll("[^\\d.]", "").replaceAll("(\\d+(\\.\\d+)?).*", "$1");
            if (DfStringUtil.is_NotNull_and_NotEmpty(majorMinorVersionStr)) {
                float majorMinorVersion = Float.parseFloat(majorMinorVersionStr);
                if (majorMinorVersion < leastVersion) {
                    String msg = "Upgrade javaparser-core version to (at least) " + JAVA_PARSER_VERSION + " for rich LastaDoc.";
                    throw new PleaseUpgradeJavaParserVersion(msg);
                }
            } else {
                _log.debug("...Loading java parser for version: Could not analyze.");
            }
        }).orElse(() -> {
            _log.debug("...Loading java parser for version: Not found.");
        });
    }

    protected MavenVersionFinder createMavenVersionFinder() {
        return new MavenVersionFinder();
    }

    protected OptionalThing<String> getJavaparserVersion() {
        return createMavenVersionFinder().findVersion("com.github.javaparser", "javaparser-core");
    }

    protected static class PleaseUpgradeJavaParserVersion extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public PleaseUpgradeJavaParserVersion(String msg) {
            super(msg);
        }
    }

    protected JavaparserSourceParserReflector createJavaparserSourceParserReflector(List<String> srcDirList) {
        return new JavaparserSourceParserReflector(srcDirList);
    }
}
