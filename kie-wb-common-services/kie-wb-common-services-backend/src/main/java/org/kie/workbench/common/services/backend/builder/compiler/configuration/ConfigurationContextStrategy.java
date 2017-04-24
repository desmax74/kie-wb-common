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

import java.util.Map;

/**
 * THis implementation first try to load properties files called IncrementalCompiler.properties like the following example
 * <p>
 * MAVEN_PLUGIN_CONFIGURATION =configuration
 * MAVEN_COMPILER_ID =compilerId
 * MAVEN_SKIP =skip
 * MAVEN_SKIP_MAIN =skipMain
 * <p>
 * MAVEN_PLUGINS =org.apache.maven.plugins
 * MAVEN_COMPILER_PLUGIN =maven-compiler-plugin
 * MAVEN_COMPILER_PLUGIN_VERSION=3.6.1
 * <p>
 * ALTERNATIVE_COMPILER_PLUGINS =io.takari.maven.plugins
 * ALTERNATIVE_COMPILER_PLUGIN =takari-lifecycle-plugin
 * ALTERNATIVE_COMPILER_PLUGIN_VERSION =1.12.4
 * <p>
 * If the fiel is not present try to check the Env variables with the same keys
 * <p>
 * it the keys aren't present the  ConfigurationStaticStrategy is used
 */
public class ConfigurationContextStrategy implements ConfigurationStrategy {

    private Map<ConfigurationKeys, String> conf;
    private Boolean valid = Boolean.FALSE;

    public ConfigurationContextStrategy() {
        getAWorkingConfig();

    }

    private void getAWorkingConfig() {
        ConfigurationStrategy properties = new ConfigurationPropertiesStrategy();

        if (properties.isValid()) {
            loadAndValidate(properties);

        } else {
            properties = new ConfigurationStaticStrategy();
            if (properties.isValid()) {
                loadAndValidate(properties);
            }
        }
    }

    private void loadAndValidate(ConfigurationStrategy properties) {
        conf = properties.loadConfiguration();
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
