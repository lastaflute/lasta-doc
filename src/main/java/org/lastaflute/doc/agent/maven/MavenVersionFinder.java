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
package org.lastaflute.doc.agent.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.dbflute.optional.OptionalThing;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.2.3 (2017/04/20 Thursday)
 */
public class MavenVersionFinder {

    public OptionalThing<String> findVersion(final String groupId, final String artifactId) {
        final String propPath = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        String version = null;
        try {
            final Enumeration<?> urls = Thread.currentThread().getContextClassLoader().getResources(propPath);
            while (urls.hasMoreElements()) {
                final URL url = (URL) urls.nextElement();
                try (InputStream is = url.openStream()) {
                    final Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version");
                    break;
                }
            }
        } catch (final IOException ignored) {}
        return OptionalThing.ofNullable(version, () -> {
            String msg = "Not found the version in maven: path=" + propPath;
            throw new IllegalStateException(msg);
        });
    }
}
