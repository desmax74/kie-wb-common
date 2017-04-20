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
package org.kie.workbench.common.services.backend.builder.compiler;

import org.apache.maven.shared.invoker.*;
import org.drools.workbench.models.datamodel.util.PortablePreconditions;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.impl.ErrorMessage;
import org.kie.workbench.common.services.backend.builder.compiler.impl.IncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.builder.compiler.impl.PomChangedHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

/**
 * Run maven with https://maven.apache.org/shared/maven-invoker/
 * to use Takari plugins like a black box
 * <p>
 * <p>
 * String mavenHome = "<path_to_maven_home>";
 * MavenCompiler compiler = new DefaultMavenCompiler(mavenHome);
 * CompilationRequest req = new DefaultCompilationRequest(new File("<path_to_prj>"), Boolean.FALSE, Arrays.asList("compile"));
 * CompilationResponse res = compiler.compileSync(req);
 */
public class DefaultMavenCompiler implements MavenCompiler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMavenCompiler.class);

    private Invoker invoker;

    private IncrementalCompilerEnabler enabler;

    /**
     * The local repo used will be /<user_home>/.m2
     *
     * @param mavenHome The maven installation folder
     */
    public DefaultMavenCompiler(File mavenHome) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(new File(System.getProperty("user.home") + "/.m2/repository"));
        invoker.setLogger(new SystemOutLogger());
        invoker.setOutputHandler(new SystemOutHandler());
    }

    /**
     * The local repo used will be /<user_home>/.m2
     *
     * @param mavenHome The maven installation folder
     */
    public DefaultMavenCompiler(File mavenHome, Boolean enablePomEditing) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(new File(System.getProperty("user.home") + "/.m2/repository"));
        invoker.setLogger(new SystemOutLogger());
        invoker.setOutputHandler(new SystemOutHandler());
        if(enablePomEditing){
            enabler = new IncrementalCompilerEnabler();
        }

    }


    /**
     * @param mavenHome
     * @param localRepository
     */
    public DefaultMavenCompiler(File mavenHome, File localRepository) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(localRepository);
        invoker.setOutputHandler(new SystemOutHandler());
    }


    /**
     * Run the maven in a specific directory outside the base directory of the processed POM.
     *
     * @param mavenHome
     * @param localRepository
     * @param workingDirectory
     */
    public DefaultMavenCompiler(File mavenHome, File localRepository, File workingDirectory) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(localRepository);
        invoker.setWorkingDirectory(workingDirectory);
        invoker.setOutputHandler(new SystemOutHandler());
    }


    public DefaultMavenCompiler(File mavenHome, File localRepository, String workingDirectory, Boolean enableIncrementalCompilationOnPOM) {
        this(mavenHome, localRepository);
        PortablePreconditions.checkNotEmpty("workingDirectory", workingDirectory);
        if (enableIncrementalCompilationOnPOM) {
            IncrementalCompilerEnabler disabler = new IncrementalCompilerEnabler();//TODO impl
        }
    }


    public DefaultMavenCompiler(File mavenHome, File localRepository, File workingDirectory, InvocationOutputHandler ioHandler) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(localRepository);
        invoker.setWorkingDirectory(workingDirectory);
        invoker.setOutputHandler(ioHandler);
    }

    public DefaultMavenCompiler(File mavenHome, File localRepository, File workingDirectory, InvocationOutputHandler ioHandler, InputStream inputStream) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(localRepository);
        invoker.setWorkingDirectory(workingDirectory);
        invoker.setOutputHandler(ioHandler);
        invoker.setInputStream(inputStream);
    }


    /**
     * Check if the folder exists and if it's writable and readable
     *
     * @param mavenRepo
     * @return
     */
    public static Boolean isValidMavenRepo(File mavenRepo) {
        return mavenRepo.exists() && mavenRepo.canWrite() && mavenRepo.canRead();
    }


    /**
     * Perform a "mvn -v" to check if mavne works correctly
     *
     * @param mavenHome
     * @return
     */
    public static Boolean isValidMavenHome(File mavenHome) {
        DefaultInvocationRequest req = new DefaultInvocationRequest();
        req.setGoals(Arrays.asList("-v"));
        InvocationResult result;
        try {
            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(mavenHome);
            result = invoker.execute(req);
        } catch (MavenInvocationException e) {
            logger.error(e.getMessage());
            return Boolean.FALSE;
        }
        return result.getExitCode() == 0;
    }

    @Override
    public Boolean isValid() {
        return isValidMavenHome(invoker.getMavenHome()) && isValidMavenRepo(invoker.getLocalRepositoryDirectory());
    }

    @Override
    public String getMavenHome() {
        return invoker.getMavenHome().toString();
    }

    @Override
    public String getLocalRepo() {
        return invoker.getLocalRepositoryDirectory().toString();
    }

    @Override
    public CompilationResponse compileSync(CompilationRequest request) {
        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
            return new DefaultCompilationResponse(Boolean.FALSE, Optional.of(new ErrorMessage(e.getMessage())));
        }
        return new DefaultCompilationResponse(Boolean.TRUE);
    }

}