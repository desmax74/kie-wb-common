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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigurationPropertiesStrategy implements ConfigurationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationPropertiesStrategy.class);

    protected Map<ConfigurationKeys, String> conf;

    private String propertiesFile = "IncrementalCompiler.properties";

    private Boolean valid = Boolean.TRUE;


    public ConfigurationPropertiesStrategy() {
        //@TODO missing binding
        Properties props = loadProperties();
        conf = new HashMap<>();


    }

    @Override
    public Boolean isValid() {
        return null;
    }

    private Properties loadProperties() {
        Properties prop = new Properties();
        InputStream in = getClass().getResourceAsStream("propertiesFile");
        try {
            prop.load(in);
            in.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            valid = Boolean.FALSE;
        }
        return prop;
    }

    @Override
    public Map<ConfigurationKeys, String> loadConfiguration() {
        return conf;
    }
}
