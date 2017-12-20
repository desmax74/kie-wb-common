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
package org.kie.workbench.common.services.backend.builder.af.impl;

import java.util.concurrent.CompletableFuture;

import org.kie.workbench.common.services.backend.builder.af.AFBuilder;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.MavenCompilerFactory;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.uberfire.java.nio.file.Paths;

public class DefaultAFBuilder implements AFBuilder {

    private final AFCompiler compiler;
    private final WorkspaceCompilationInfo info;
    private final String mavenRepo;
    private CompilationRequest req;

    public DefaultAFBuilder(String projectRepo,
                            String mavenRepo,
                            Boolean skipPrjDependenciesCreationList) {
        /**In the construct we create the objects ready for a call to the build() without params to reuse all the internal objects,
         * only in the internal maven compilation new objects ill be created in the compileSync */
        this.mavenRepo = mavenRepo;
        compiler = MavenCompilerFactory.getCompiler(Decorator.LOG_OUTPUT_AFTER);
        info = new WorkspaceCompilationInfo(Paths.get(projectRepo));
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            new String[]{MavenCLIArgs.COMPILE},
                                            Boolean.TRUE,
                                            skipPrjDependenciesCreationList);
    }

    public DefaultAFBuilder(String projectRepo,
                            String mavenRepo) {
        /**In the construct we create the objects ready for a call to the build() without params to reuse all the internal objects,
         * only in the internal maven compilation new objects ill be created in the compileSync */
        this.mavenRepo = mavenRepo;
        compiler = MavenCompilerFactory.getCompiler(Decorator.LOG_OUTPUT_AFTER);
        info = new WorkspaceCompilationInfo(Paths.get(projectRepo));
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            new String[]{MavenCLIArgs.COMPILE},
                                            Boolean.TRUE, Boolean.FALSE);
    }

    /***
     *Constructor to define the default behaviour called with the build method
     * @param projectRepo
     * @param mavenRepo
     * @param args maven cli args
     */
    public DefaultAFBuilder(String projectRepo,
                            String mavenRepo,
                            String[] args) {
        /**In the construct we create the objects ready for a call to the build() without params to reuse all the internal objects,
         * only in the internal maven compilation new objects ill be created in the compileSync */
        this.mavenRepo = mavenRepo;
        compiler = MavenCompilerFactory.getCompiler(Decorator.LOG_OUTPUT_AFTER);
        info = new WorkspaceCompilationInfo(Paths.get(projectRepo));
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            args,
                                            Boolean.TRUE, Boolean.FALSE);
    }

    /***
     *Constructor to define the default behaviour called with the build method
     * @param projectRepo
     * @param mavenRepo
     * @param args maven cli args
     */
    public DefaultAFBuilder(String projectRepo,
                            String mavenRepo,
                            String[] args,
                            Boolean skipPrjDependenciesCreationList) {
        /**In the construct we create the objects ready for a call to the build() without params to reuse all the internal objects,
         * only in the internal maven compilation new objects ill be created in the compileSync */
        this.mavenRepo = mavenRepo;
        compiler = MavenCompilerFactory.getCompiler(Decorator.LOG_OUTPUT_AFTER);
        info = new WorkspaceCompilationInfo(Paths.get(projectRepo));
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            args,
                                            Boolean.TRUE,
                                            skipPrjDependenciesCreationList);
    }

    public DefaultAFBuilder(String projectRepo,
                            String mavenRepo, AFCompiler compiler) {
        /**In the construct we create the objects ready for a call to the build() without params to reuse all the internal objects,
         * only in the internal maven compilation new objects ill be created in the compileSync */
        this.mavenRepo = mavenRepo;
        this.compiler = compiler;
        info = new WorkspaceCompilationInfo(Paths.get(projectRepo));
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            new String[]{MavenCLIArgs.COMPILE},
                                            Boolean.TRUE, Boolean.FALSE);
    }

    public DefaultAFBuilder(String projectRepo,
                            String mavenRepo, AFCompiler compiler,
                            Boolean skipPrjDependenciesCreationList) {
        /**In the construct we create the objects ready for a call to the build() without params to reuse all the internal objects,
         * only in the internal maven compilation new objects ill be created in the compileSync */
        this.mavenRepo = mavenRepo;
        this.compiler = compiler;
        info = new WorkspaceCompilationInfo(Paths.get(projectRepo));
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            new String[]{MavenCLIArgs.COMPILE},
                                            Boolean.TRUE,
                                            skipPrjDependenciesCreationList);
    }

    /*******************************************************************************************************************************/

    @Override
    public CompletableFuture<Boolean> cleanInternalCache() {
        return CompletableFuture.completedFuture(compiler.cleanInternalCache());
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAsync() {
        req.getKieCliRequest().getMap().clear();
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAndPackageAsync() {
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            new String[]{MavenCLIArgs.PACKAGE},
                                            Boolean.TRUE, Boolean.FALSE);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAndPackageAsync(Boolean skipPrjDependenciesCreationList) {
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            new String[]{MavenCLIArgs.PACKAGE},
                                            Boolean.TRUE, skipPrjDependenciesCreationList);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAndInstallAsync() {

        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            new String[]{MavenCLIArgs.INSTALL},
                                            Boolean.TRUE, Boolean.FALSE);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAndInstallAsync(Boolean skipPrjDependenciesCreationList) {
        req = new DefaultCompilationRequest(mavenRepo,
                                            info,
                                            new String[]{MavenCLIArgs.INSTALL},
                                            Boolean.TRUE, skipPrjDependenciesCreationList);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAsync(String mavenRepo) {
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE, Boolean.FALSE);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAsync(String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE, skipPrjDependenciesCreationList);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAsync(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(projectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE, skipPrjDependenciesCreationList);
        return CompletableFuture.completedFuture(compiler.compileSync(req));


    }

    @Override
    public CompletableFuture<CompilationResponse> buildAsync(String projectPath, String mavenRepo) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(projectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE, Boolean.FALSE);
        return CompletableFuture.completedFuture(compiler.compileSync(req));

    }

    @Override
    public CompletableFuture<CompilationResponse> buildAndPackageAsync(String projectPath, String mavenRepo) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(projectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.PACKAGE},
                                                               Boolean.TRUE, Boolean.FALSE);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAndPackageAsync(String projectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(projectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.PACKAGE},
                                                               Boolean.TRUE, skipPrjDependenciesCreationList);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildAndInstallAsync(String kieProjectPath, String mavenRepo, Boolean skipPrjDependenciesCreationList) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(kieProjectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL},
                                                               Boolean.TRUE, skipPrjDependenciesCreationList);
        return CompletableFuture.completedFuture(compiler.compileSync(req));


    }

    @Override
    public CompletableFuture<CompilationResponse> buildAndInstallAsync(String kieProjectPath, String mavenRepo) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(kieProjectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL},
                                                               Boolean.TRUE, Boolean.FALSE);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildSpecializedAsync(String kieProjectPath, String mavenRepo, String[] args) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(kieProjectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               args,
                                                               Boolean.TRUE, Boolean.FALSE);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildSpecializedAsync(String kieProjectPath, String mavenRepo, String[] args, Boolean skipPrjDependenciesCreationList) {
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(kieProjectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               args,
                                                               Boolean.TRUE, skipPrjDependenciesCreationList);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildSpecializedAsync(String kieProjectPath, String mavenRepo, String[] args, Decorator decorator) {
        AFCompiler compiler = MavenCompilerFactory.getCompiler(decorator);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(kieProjectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               args,
                                                               Boolean.TRUE, Boolean.FALSE);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }

    @Override
    public CompletableFuture<CompilationResponse> buildSpecializedAsync(String kieProjectPath, String mavenRepo, String[] args, Decorator decorator, Boolean skipPrjDependenciesCreationList) {
        AFCompiler compiler = MavenCompilerFactory.getCompiler(decorator);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(kieProjectPath));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               args,
                                                               Boolean.TRUE, skipPrjDependenciesCreationList);
        return CompletableFuture.completedFuture(compiler.compileSync(req));
    }
}
