/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.services.backend.compiler.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.assertThat;

import org.guvnor.common.services.project.backend.server.utils.configuration.ConfigurationKey;
import org.guvnor.common.services.project.backend.server.utils.configuration.ConfigurationStrategy;
import org.junit.Test;

public class ConfigurationTest {

    private static final String PROPERTIES_FILE = "IncrementalCompiler.properties";
    private String removedPropValue;

    @Test
    public void loadConfig() {
        ConfigurationContextProvider provider = new ConfigurationContextProvider();
        Map<ConfigurationKey, String> conf = provider.loadConfiguration();
        assertThat(conf.keySet()).hasSize(14);
    }

    @Test
    public void loadStaticConfig() {
        ConfigurationStrategy strategy = new ConfigurationStaticStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        assertThat(strategy.isValid()).isTrue();
        assertThat(conf.keySet()).hasSize(14);
    }

    @Test
    public void loadPropertiesConfig() {
        ConfigurationStrategy strategy = new ConfigurationPropertiesStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        assertThat(strategy.isValid()).isTrue();
        assertThat(conf.keySet()).hasSize(14);
    }

    @Test
    public void loadNotValidPropertiesConfig() {
        try {
            removePropertyFromFile();
        } catch (Exception ex) {
            fail("Removing property from the file failed.", ex);
        }

        ConfigurationStrategy strategy = new ConfigurationPropertiesStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        assertThat(strategy.isValid()).isFalse();
        assertThat(conf.size()).isNotEqualTo(14);

        try {
            addPropertyBackToFile();
        } catch (Exception ex) {
            fail("Adding property back to the file failed.", ex);
        }
    }

    @Test
    public void loadEnvironmentConfig() {
        ConfigurationStrategy strategy = new ConfigurationEnvironmentStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        assertThat(conf).isEmpty();

        strategy = new ConfigurationEnvironmentStrategy(getMapForEnv());
        conf = strategy.loadConfiguration();
        assertThat(conf.isEmpty()).isFalse();
        assertThat(strategy.isValid()).isTrue();
        assertThat(conf.keySet()).hasSize(14);
    }

    @Test
    public void loadNotValidEnvironmentConfigu() {
        ConfigurationStrategy strategy = new ConfigurationEnvironmentStrategy();
        Map<ConfigurationKey, String> conf = strategy.loadConfiguration();
        assertThat(conf).isEmpty();

        Map<String, String> notValidEnv = getMapForEnv();
        notValidEnv.remove(ConfigurationKey.MAVEN_COMPILER_PLUGIN_VERSION.name());

        strategy = new ConfigurationEnvironmentStrategy(notValidEnv);
        conf = strategy.loadConfiguration();
        assertThat(conf).isNotNull();
        assertThat(strategy.isValid()).isFalse();
    }

    private Map<String, String> getMapForEnv() {
        Map conf = new HashMap<>();
        conf.put(ConfigurationKey.COMPILER.name(), "jdt");
        conf.put(ConfigurationKey.SOURCE_VERSION.name(), "1.8");
        conf.put(ConfigurationKey.TARGET_VERSION.name(), "1.8");
        conf.put(ConfigurationKey.MAVEN_COMPILER_PLUGIN_GROUP.name(), "org.apache.maven.plugins");
        conf.put(ConfigurationKey.MAVEN_COMPILER_PLUGIN_ARTIFACT.name(), "maven-compiler-plugin");
        conf.put(ConfigurationKey.MAVEN_COMPILER_PLUGIN_VERSION.name(), "3.7.0");
        conf.put(ConfigurationKey.FAIL_ON_ERROR.name(), "false");
        conf.put(ConfigurationKey.TAKARI_COMPILER_PLUGIN_GROUP.name(), "kie.io.takari.maven.plugins");
        conf.put(ConfigurationKey.TAKARI_COMPILER_PLUGIN_ARTIFACT.name(), "kie-takari-lifecycle-plugin");
        conf.put(ConfigurationKey.TAKARI_COMPILER_PLUGIN_VERSION.name(), "1.13.3");
        conf.put(ConfigurationKey.KIE_MAVEN_PLUGINS.name(), "org.kie");
        conf.put(ConfigurationKey.KIE_MAVEN_PLUGIN.name(), "kie-maven-plugin");
        conf.put(ConfigurationKey.KIE_TAKARI_PLUGIN.name(), "kie-takari-plugin");
        conf.put(ConfigurationKey.KIE_VERSION.name(), "7.7.0");
        return conf;
    }

    private void removePropertyFromFile() throws Exception {
        Properties properties = loadPropertiesFile();

        try (FileOutputStream out = createOutputStream()) {
            removedPropValue = properties.getProperty(ConfigurationKey.KIE_VERSION.name());
            properties.remove(ConfigurationKey.KIE_VERSION.name());
            properties.store(out, PROPERTIES_FILE);
        }
    }

    private void addPropertyBackToFile() throws Exception {
        Properties properties = loadPropertiesFile();

        try (FileOutputStream out = createOutputStream()) {
            properties.put(ConfigurationKey.KIE_VERSION.name(), removedPropValue);
            properties.store(out, PROPERTIES_FILE);
        }
    }

    private Properties loadPropertiesFile() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            properties.load(in);
        }

        return properties;
    }

    private FileOutputStream createOutputStream() throws URISyntaxException, FileNotFoundException {
        URL url = getClass().getClassLoader().getResource(PROPERTIES_FILE);
        File fileObject = new File(url.toURI());

        return new FileOutputStream(fileObject);
    }
}
