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

package org.kie.workbench.common.services.backend.builder.compiler.impl;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.kie.workbench.common.services.backend.builder.compiler.ConfigurationStrategy;
import org.kie.workbench.common.services.backend.builder.compiler.PomEditor;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.ConfigurationKeys;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenGoals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

public class DefaultPomEditor implements PomEditor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPomEditor.class);
    public final String POM = "pom";
    public final String TRUE = "true";
    private Compilers compiler;
    private Map<ConfigurationKeys, String> conf;
    private MavenXpp3Reader reader;
    private MavenXpp3Writer writer;
    private Set<PomPlaceHolder> history;

    public DefaultPomEditor(Set<PomPlaceHolder> history, ConfigurationStrategy config, Compilers compiler) {
        conf = config.loadConfiguration();
        reader = new MavenXpp3Reader();
        writer = new MavenXpp3Writer();
        this.history = history;
        this.compiler = compiler;
    }

    public Set<PomPlaceHolder> getHistory() {
        return Collections.unmodifiableSet(history);
    }

    @Override
    public void cleanHistory() {
        history.clear();
    }


    public PomPlaceHolder readSingle(File pom) {
        PomPlaceHolder holder = new PomPlaceHolder();
        try {
            Model model = reader.read(new FileInputStream(pom));
            holder = new PomPlaceHolder(pom.getPath(), model.getArtifactId(), model.getGroupId(), model.getVersion(), model.getPackaging());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return holder;
    }

    /**
     * @Override public org.apache.maven.plugin.MojoExecution newMojoExecution() {
     * MojoExecution execution = super.newMojoExecution();
     * Xpp3Dom compilerId = new Xpp3Dom("compilerId");
     * compilerId.setValue("jdt");
     * execution.getConfiguration().addChild(compilerId);
     * return execution;
     * }
     */

    public void write(File pom) {

        try {
            Model model = reader.read(new FileInputStream(pom));
            if (model == null) {
                logger.error("Model null from pom file:", pom.getPath());
                return;
            }

            PomPlaceHolder pomPH = new PomPlaceHolder(pom.getPath(), model.getArtifactId(), model.getGroupId(), model.getVersion(), model.getPackaging());
            if (model.getPackaging().equals(POM) && !history.contains(pomPH)) {
                updatePom(model);
                writer.write(new FileOutputStream(pom), model);
                history.add(pomPH);

            } else {
                //we process the build plugin only in the parent poms (packaging pom)
                history.add(pomPH);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    private void updatePom(Model model) {
        Build build = model.getBuild();
        if (build == null) {  //pom without build tag
            model.setBuild(new Build());
            build = model.getBuild();
        }

        List<Plugin> buildPlugins = build.getPlugins();
        for (Plugin plugin : buildPlugins) {
            if (plugin.getGroupId().equals(conf.get(ConfigurationKeys.MAVEN_PLUGINS)) && plugin.getArtifactId().equals(conf.get(ConfigurationKeys.MAVEN_COMPILER_PLUGIN))) {
                disableMavenCompilerAlreadyPresent(plugin); // disable the maven compiler if present
            }
        }
        //@TODO search if takari plugin is already present before to add
        build.addPlugin(getNewCompilerPlugin());
    }

    private Plugin getNewCompilerPlugin() {

        Plugin newCompilerPlugin = new Plugin();
        newCompilerPlugin.setGroupId(conf.get(ConfigurationKeys.ALTERNATIVE_COMPILER_PLUGINS));
        newCompilerPlugin.setArtifactId(conf.get(ConfigurationKeys.ALTERNATIVE_COMPILER_PLUGIN));
        newCompilerPlugin.setVersion(conf.get(ConfigurationKeys.ALTERNATIVE_COMPILER_PLUGIN_VERSION));

        PluginExecution execution = new PluginExecution();
        execution.setId(MavenGoals.COMPILE);
        execution.setGoals(Arrays.asList(MavenGoals.COMPILE));
        execution.setPhase(MavenGoals.COMPILE);

        Xpp3Dom compilerId = new Xpp3Dom(conf.get(ConfigurationKeys.MAVEN_COMPILER_ID));
        compilerId.setValue(compiler.name().toLowerCase());
        Xpp3Dom configuration = new Xpp3Dom(conf.get(ConfigurationKeys.MAVEN_PLUGIN_CONFIGURATION));
        configuration.addChild(compilerId);

        execution.setConfiguration(configuration);
        newCompilerPlugin.setExecutions(Arrays.asList(execution));

        return newCompilerPlugin;
    }

    private void disableMavenCompilerAlreadyPresent(Plugin plugin) {
        Xpp3Dom skipMain = new Xpp3Dom(conf.get(ConfigurationKeys.MAVEN_SKIP_MAIN));
        skipMain.setValue(TRUE);
        Xpp3Dom skip = new Xpp3Dom(conf.get(ConfigurationKeys.MAVEN_SKIP));
        skip.setValue(TRUE);

        Xpp3Dom configuration = new Xpp3Dom(conf.get(ConfigurationKeys.MAVEN_PLUGIN_CONFIGURATION));
        configuration.addChild(skipMain);
        configuration.addChild(skip);

        plugin.setConfiguration(configuration);
    }

}
