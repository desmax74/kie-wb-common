/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.services.backend.builder.compiler.configuration;

import org.kie.workbench.common.services.backend.builder.compiler.ConfigurationStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation, this class can be extended to change the protected configuration Map
 */
public class ConfigurationStaticStrategy implements ConfigurationStrategy {

    protected Map<ConfigurationKeys, String> conf;

    private Boolean valid = Boolean.FALSE;

    public ConfigurationStaticStrategy() {
        conf = new HashMap<>();

        conf.put(ConfigurationKeys.MAVEN_PLUGIN_CONFIGURATION, "configuration");
        conf.put(ConfigurationKeys.MAVEN_COMPILER_ID, "compilerId");
        conf.put(ConfigurationKeys.MAVEN_SKIP, "skip");
        conf.put(ConfigurationKeys.MAVEN_SKIP_MAIN, "skipMain");

        conf.put(ConfigurationKeys.MAVEN_PLUGINS, "org.apache.maven.plugins");
        conf.put(ConfigurationKeys.MAVEN_COMPILER_PLUGIN, "maven-compiler-plugin");
        conf.put(ConfigurationKeys.MAVEN_COMPILER_PLUGIN_VERSION, "3.6.1");

        conf.put(ConfigurationKeys.ALTERNATIVE_COMPILER_PLUGINS, "io.takari.maven.plugins");
        conf.put(ConfigurationKeys.ALTERNATIVE_COMPILER_PLUGIN, "takari-lifecycle-plugin");
        conf.put(ConfigurationKeys.ALTERNATIVE_COMPILER_PLUGIN_VERSION, "1.12.4");
        valid = Boolean.TRUE;
    }

    @Override
    public Map<ConfigurationKeys, String> loadConfiguration() {
        return conf;
    }

    @Override
    public Boolean isValid() {
        return valid;
    }
}
