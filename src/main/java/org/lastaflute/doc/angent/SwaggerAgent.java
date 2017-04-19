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
package org.lastaflute.doc.angent;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.doc.util.MavenVersionFinder;

/**
 * @author p1us2er0
 * @since 0.2.3 (2017/04/20 Thursday)
 */
public class SwaggerAgent {

    public OptionalThing<String> getSwaggerUiUrl(String swaggerJsonUrl) {
        return getSwaggerUiVersion().map(version -> String.format("../webjars/swagger-ui/%s/index.html?url=%s", version, swaggerJsonUrl));
    }

    protected MavenVersionFinder createMavenVersionFinder() {
        return new MavenVersionFinder();
    }

    protected OptionalThing<String> getSwaggerUiVersion() {
        return createMavenVersionFinder().getVersion("org.webjars", "swagger-ui");
    }
}
