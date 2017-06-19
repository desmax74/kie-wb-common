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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.kie.workbench.common.services.backend.builder.compiler.PluginPresents;
import org.kie.workbench.common.services.backend.builder.compiler.PomEditor;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.ConfigurationKey;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.ConfigurationProvider;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPomEditor implements PomEditor {

    public final String POM = "pom";
    public final String TRUE = "true";
    public final String POM_NAME = "pom.xml";
    public final String MAVEN_COMPILER_EXECUTION_ID = "default-compile";
    public final String MAVEN_COMPILER_EXECUTION_PHASE = "none";
    protected final Logger logger = LoggerFactory.getLogger(DefaultPomEditor.class);
    protected Compilers compiler;
    protected Map<ConfigurationKey, String> conf;
    protected MavenXpp3Reader reader;
    protected MavenXpp3Writer writer;
    protected Set<PomPlaceHolder> history;

    public DefaultPomEditor(Set<PomPlaceHolder> history,
                            ConfigurationProvider config,
                            Compilers compiler) {
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

    protected PluginPresents updatePom(Model model) {

        Build build = model.getBuild();
        if (build == null) {  //pom without build tag
            model.setBuild(new Build());
            build = model.getBuild();
        }

        Boolean defaultCompilerPluginPresent = Boolean.FALSE;
        //int defaultCompilerPosition = 0;
        Boolean alternativeCompilerPluginPresent = Boolean.FALSE;
        int alternativeCompilerPosition = 0;
        Boolean kiePluginPresent = Boolean.FALSE;
        int kieMavenPluginPosition = 0;

        int i = 0;
        Boolean disableMavenCompiler = Boolean.TRUE;
        for (Plugin plugin : build.getPlugins()) {

            // Check if is present the default maven compiler
            if (plugin.getGroupId().equals(conf.get(ConfigurationKey.MAVEN_PLUGINS)) &&
                    plugin.getArtifactId().equals(conf.get(ConfigurationKey.MAVEN_COMPILER_PLUGIN))) {
                defaultCompilerPluginPresent = Boolean.TRUE;

                List<PluginExecution> executions = plugin.getExecutions();
                for (PluginExecution exec : executions) {
                    if (exec.getId().equals(MAVEN_COMPILER_EXECUTION_ID) && exec.getPhase().equals(MAVEN_COMPILER_EXECUTION_PHASE)) {
                        disableMavenCompiler = Boolean.FALSE;
                    }
                }
                //defaultCompilerPosition = i;
                if (disableMavenCompiler) {
                    disableMavenCompilerAlreadyPresent(plugin); // disable the maven compiler if present
                }
            }
            //check if is present the alternative maven compiler
            if (plugin.getGroupId().equals(conf.get(ConfigurationKey.ALTERNATIVE_COMPILER_PLUGINS)) &&
                    plugin.getArtifactId().equals(conf.get(ConfigurationKey.ALTERNATIVE_COMPILER_PLUGIN))) {
                alternativeCompilerPluginPresent = Boolean.TRUE;
                alternativeCompilerPosition = i;
            }

            //check if is present the kie plugin and move after takari
            if (plugin.getGroupId().equals(conf.get(ConfigurationKey.KIE_MAVEN_PLUGINS)) &&
                    plugin.getArtifactId().equals(conf.get(ConfigurationKey.KIE_MAVEN_PLUGIN))) {
                kiePluginPresent = Boolean.TRUE;
                kieMavenPluginPosition = i;
            }
            i++;
        }

        Boolean overwritePOM = updatePOMModel(build,
                                              defaultCompilerPluginPresent,
                                              alternativeCompilerPluginPresent,
                                              alternativeCompilerPosition,
                                              kiePluginPresent,
                                              kieMavenPluginPosition,
                                              i);

        return new DefaultPluginPresents(defaultCompilerPluginPresent,
                                         alternativeCompilerPluginPresent,
                                         kiePluginPresent,
                                         overwritePOM);
    }

    private Boolean updatePOMModel(Build build,
                                   Boolean defaultCompilerPluginPresent,
                                   Boolean alternativeCompilerPluginPresent,
                                   int alternativeCompilerPosition,
                                   Boolean kiePluginPresent,
                                   int kieMavenPluginPosition,
                                   int i) {
        Boolean overwritePOM = Boolean.FALSE;

        if (!alternativeCompilerPluginPresent && !kiePluginPresent) {
            build.addPlugin(getNewCompilerPlugin());
            alternativeCompilerPosition = i;
            overwritePOM = Boolean.TRUE;
        }

        if (!defaultCompilerPluginPresent && !kiePluginPresent) {
            //if default maven compiler is not present we add the skip and phase none  to avoid its use
            Plugin disabledDefaultCompiler = new Plugin();
            disabledDefaultCompiler.setArtifactId(conf.get(ConfigurationKey.MAVEN_COMPILER_PLUGIN));
            disableMavenCompilerAlreadyPresent(disabledDefaultCompiler);
            build.addPlugin(disabledDefaultCompiler);
            overwritePOM = Boolean.TRUE;
        }

        if (kiePluginPresent && alternativeCompilerPluginPresent) {
            //if kieplugin is present must be after the alternative compiler
            if (kieMavenPluginPosition <= alternativeCompilerPosition) {
                //swap the positions
                Plugin kieMaven = build.getPlugins().get(kieMavenPluginPosition);
                Plugin alternativeCompiler = build.getPlugins().get(alternativeCompilerPosition);
                build.getPlugins().set(kieMavenPluginPosition,
                                       alternativeCompiler);
                build.getPlugins().set(alternativeCompilerPosition,
                                       kieMaven);
                overwritePOM = Boolean.TRUE;
            }
        }

        if (kiePluginPresent && !alternativeCompilerPluginPresent) {
            //if kieplugin is present must be after the alternative compiler
            build.addPlugin(getNewCompilerPlugin());
            alternativeCompilerPosition = getPluginPosition(build,conf.get(ConfigurationKey.ALTERNATIVE_COMPILER_PLUGIN)) -1;
            overwritePOM = Boolean.TRUE;

            if (kieMavenPluginPosition <= alternativeCompilerPosition){
                //swap the positions
                Plugin kieMaven = build.getPlugins().get(kieMavenPluginPosition);
                Plugin alternativeCompiler = build.getPlugins().get(alternativeCompilerPosition);
                build.getPlugins().set(kieMavenPluginPosition,
                                       alternativeCompiler);
                build.getPlugins().set(alternativeCompilerPosition,
                                       kieMaven);
                overwritePOM = Boolean.TRUE;
            }
        }

        if (!defaultCompilerPluginPresent && kiePluginPresent) {
            //if default maven compiler is not present we add the skip and phase none  to avoid its use
            Plugin disabledDefaultCompiler = new Plugin();
            disabledDefaultCompiler.setArtifactId(conf.get(ConfigurationKey.MAVEN_COMPILER_PLUGIN));
            disableMavenCompilerAlreadyPresent(disabledDefaultCompiler);
            build.addPlugin(disabledDefaultCompiler);
            overwritePOM = Boolean.TRUE;
        }
        return overwritePOM;
    }

    protected int getPluginPosition( Build build , String artifactPlugin){
        int i = 0;
        for (Plugin plugin : build.getPlugins()) {
            if(plugin.getGroupId().equals(artifactPlugin)){
                break;
            }
            i++;
        }
        return i;
    }

    protected Plugin getNewCompilerPlugin() {

        Plugin newCompilerPlugin = new Plugin();
        newCompilerPlugin.setGroupId(conf.get(ConfigurationKey.ALTERNATIVE_COMPILER_PLUGINS));
        newCompilerPlugin.setArtifactId(conf.get(ConfigurationKey.ALTERNATIVE_COMPILER_PLUGIN));
        newCompilerPlugin.setVersion(conf.get(ConfigurationKey.ALTERNATIVE_COMPILER_PLUGIN_VERSION));

        PluginExecution execution = new PluginExecution();
        execution.setId(MavenArgs.COMPILE);
        execution.setGoals(Arrays.asList(MavenArgs.COMPILE));
        execution.setPhase(MavenArgs.COMPILE);

        Xpp3Dom compilerId = new Xpp3Dom(conf.get(ConfigurationKey.MAVEN_COMPILER_ID));
        compilerId.setValue(compiler.name().toLowerCase());
        Xpp3Dom configuration = new Xpp3Dom(conf.get(ConfigurationKey.MAVEN_PLUGIN_CONFIGURATION));
        configuration.addChild(compilerId);

        execution.setConfiguration(configuration);
        newCompilerPlugin.setExecutions(Arrays.asList(execution));

        return newCompilerPlugin;
    }

    protected void disableMavenCompilerAlreadyPresent(Plugin plugin) {
        Xpp3Dom skipMain = new Xpp3Dom(conf.get(ConfigurationKey.MAVEN_SKIP_MAIN));
        skipMain.setValue(TRUE);
        Xpp3Dom skip = new Xpp3Dom(conf.get(ConfigurationKey.MAVEN_SKIP));
        skip.setValue(TRUE);

        Xpp3Dom configuration = new Xpp3Dom(conf.get(ConfigurationKey.MAVEN_PLUGIN_CONFIGURATION));
        configuration.addChild(skipMain);
        configuration.addChild(skip);

        plugin.setConfiguration(configuration);

        PluginExecution exec = new PluginExecution();
        exec.setId(conf.get(ConfigurationKey.MAVEN_DEFAULT_COMPILE));
        exec.setPhase(conf.get(ConfigurationKey.MAVEN_PHASE_NONE));
        List<PluginExecution> executions = new ArrayList<>();
        executions.add(exec);
        plugin.setExecutions(executions);
    }
}
