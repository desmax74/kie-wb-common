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
package org.kie.workbench.common.services.backend.compiler.service;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;

import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.service.executors.CompilerLogLevel;
import org.kie.workbench.common.services.backend.compiler.service.executors.DefaultLocalExecutor;
import org.kie.workbench.common.services.backend.compiler.service.executors.DefaultRemoteExecutor;
import org.uberfire.java.nio.file.Path;
/**
 * Define the Default Implementation of a AppFormer Compiler Service
 */
@ApplicationScoped
public class DefaultKieCompilerService implements AFCompilerService {

    private DefaultLocalExecutor localExecutor;
    private DefaultRemoteExecutor remoteExecutor;

    public DefaultKieCompilerService(){
        localExecutor = new DefaultLocalExecutor(Executors.newCachedThreadPool());
        remoteExecutor = new DefaultRemoteExecutor(Executors.newCachedThreadPool());
    }

    private String[] getArgsWithDebug(CompilerLogLevel logLevel, String goal){
        return (logLevel.name().equals(CompilerLogLevel.DEBUG) ? new String[]{goal, MavenCLIArgs.DEBUG} : new String[]{goal});
    }

    /************************************ Suitable for the Local Builds ***********************************************/

    @Override
    public CompletableFuture<KieCompilationResponse> build(Path projectPath, String mavenRepo, CompilerLogLevel logLevel) {
        return localExecutor.build(projectPath, mavenRepo, logLevel);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(Path projectPath, String mavenRepo, Map<Path, InputStream> override, CompilerLogLevel logLevel) {
        return localExecutor.build(projectPath, mavenRepo, override, logLevel);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(Path projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList, CompilerLogLevel logLevel) {
        return localExecutor.build(projectPath, mavenRepo, skipPrjDependenciesCreationList, logLevel);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(Path projectPath, String mavenRepo, CompilerLogLevel logLevel) {
        return localExecutor.buildAndInstall(projectPath, mavenRepo, logLevel);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(Path projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList, CompilerLogLevel logLevel) {
        return localExecutor.buildAndInstall(projectPath, mavenRepo, skipPrjDependenciesCreationList, logLevel);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(Path projectPath, String mavenRepo, String[] args) {
        return localExecutor.buildSpecialized(projectPath, mavenRepo, args);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(Path projectPath, String mavenRepo, String[] args, Boolean skipPrjDependenciesCreationList) {
        return localExecutor.buildSpecialized(projectPath, mavenRepo, args, skipPrjDependenciesCreationList);
    }

    /************************************ Suitable for the REST Builds ************************************************/

    @Override
    public CompletableFuture<KieCompilationResponse> build(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList,CompilerLogLevel logLevel) {
        return remoteExecutor.build(projectPath, mavenRepo, skipPrjDependenciesCreationList, logLevel);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> build(String projectPath, String mavenRepo, CompilerLogLevel logLevel) {
        return remoteExecutor.build(projectPath, mavenRepo, logLevel);
    }


    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(String projectPath, String mavenRepo, CompilerLogLevel logLevel) {
        return remoteExecutor.buildAndInstall(projectPath, mavenRepo, logLevel);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildAndInstall(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList, CompilerLogLevel logLevel) {
        return remoteExecutor.buildAndInstall(projectPath, mavenRepo, skipPrjDependenciesCreationList, logLevel);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath, String mavenRepo, String[] args) {
        return remoteExecutor.buildSpecialized(projectPath, mavenRepo, args);
    }

    @Override
    public CompletableFuture<KieCompilationResponse> buildSpecialized(String projectPath, String mavenRepo, String[] args, Boolean skipPrjDependenciesCreationList) {
        return remoteExecutor.buildSpecialized(projectPath, mavenRepo, args, skipPrjDependenciesCreationList);
    }
}
