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

import org.apache.commons.cli.CommandLine;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.classworlds.ClassWorld;

import java.nio.file.Path;
import java.util.Properties;

public class KieCliRequest {

    private String[] args;
    private CommandLine commandLine;
    private ClassWorld classWorld;
    private String workingDirectory;
    private Path multiModuleProjectDirectory;
    private boolean debug;
    private boolean quiet;
    private boolean showErrors = true;
    private Properties userProperties = new Properties();
    private Properties systemProperties = new Properties();
    private MavenExecutionRequest request;

    public KieCliRequest(Path multiModuleProjectDirectory, String[] args) {
        this.multiModuleProjectDirectory = multiModuleProjectDirectory;
        this.workingDirectory = multiModuleProjectDirectory.toString();
        this.request = new DefaultMavenExecutionRequest();
        this.args = args;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    public ClassWorld getClassWorld() {
        return classWorld;
    }

    public void setClassWorld(ClassWorld classWorld) {
        this.classWorld = classWorld;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Path getMultiModuleProjectDirectory() {
        return multiModuleProjectDirectory;
    }

    public void setMultiModuleProjectDirectory(Path multiModuleProjectDirectory) {
        this.multiModuleProjectDirectory = multiModuleProjectDirectory;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public boolean isShowErrors() {
        return showErrors;
    }

    public void setShowErrors(boolean showErrors) {
        this.showErrors = showErrors;
    }

    public Properties getUserProperties() {
        return userProperties;
    }

    public Properties getSystemProperties() {
        return systemProperties;
    }

    public MavenExecutionRequest getRequest() {
        return request;
    }

}
