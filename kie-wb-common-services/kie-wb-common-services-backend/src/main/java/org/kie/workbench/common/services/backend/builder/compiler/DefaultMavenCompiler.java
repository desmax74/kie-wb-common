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
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultIncrementalCompilerEnabler;
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
    public DefaultMavenCompiler(File mavenHome, Boolean writeOnFS) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(new File(System.getProperty("user.home") + "/.m2/repository"));
        invoker.setLogger(new SystemOutLogger());
        invoker.setOutputHandler(new SystemOutHandler());
        enabler = new DefaultIncrementalCompilerEnabler(Compilers.JAVAC, writeOnFS);
    }

    /**
     * The local repo used will be /<user_home>/.m2
     *
     * @param mavenHome The maven installation folder
     */
    public DefaultMavenCompiler(File mavenHome, Compilers compiler, Boolean writeOnFS) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(new File(System.getProperty("user.home") + "/.m2/repository"));
        invoker.setLogger(new SystemOutLogger());
        invoker.setOutputHandler(new SystemOutHandler());
        enabler = new DefaultIncrementalCompilerEnabler(compiler, writeOnFS);
    }


    /**
     * @param mavenHome
     * @param localRepository
     */
    public DefaultMavenCompiler(File mavenHome, File localRepository, Boolean writeOnFS) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(localRepository);
        invoker.setOutputHandler(new SystemOutHandler());
        enabler = new DefaultIncrementalCompilerEnabler(Compilers.JAVAC, writeOnFS);
    }


    /**
     * Run the maven in a specific directory outside the base directory of the processed POM.
     *
     * @param mavenHome
     * @param localRepository
     * @param workingDirectory
     */
    public DefaultMavenCompiler(File mavenHome, File localRepository, File workingDirectory, Boolean writeOnFS) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(localRepository);
        invoker.setWorkingDirectory(workingDirectory);
        invoker.setOutputHandler(new SystemOutHandler());
        enabler = new DefaultIncrementalCompilerEnabler(Compilers.JAVAC, writeOnFS);
    }


    public DefaultMavenCompiler(File mavenHome, File localRepository, File workingDirectory, InvocationOutputHandler ioHandler, Boolean writeOnFS) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(localRepository);
        invoker.setWorkingDirectory(workingDirectory);
        invoker.setOutputHandler(ioHandler);
        enabler = new DefaultIncrementalCompilerEnabler(Compilers.JAVAC, writeOnFS);
    }

    public DefaultMavenCompiler(File mavenHome, File localRepository, File workingDirectory, InvocationOutputHandler ioHandler, InputStream inputStream, Boolean writeOnFS) {
        invoker = new DefaultInvoker();
        invoker.setMavenHome(mavenHome);
        invoker.setLocalRepositoryDirectory(localRepository);
        invoker.setWorkingDirectory(workingDirectory);
        invoker.setOutputHandler(ioHandler);
        invoker.setInputStream(inputStream);
        enabler = new DefaultIncrementalCompilerEnabler(Compilers.JAVAC, writeOnFS);
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

    /**
     * Perform a "mvn -v" call to check if the maven home is correct
     *
     * @return
     */
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
        if (logger.isDebugEnabled()) {
            logger.debug("CompilationRequest:{}", request);
        }

        Boolean result = enabler.process(request);
        if (!result) {
            return new DefaultCompilationResponse(Boolean.FALSE, Optional.of("Processing poms failed"));
        }

        InvocationResult res;
        try {
            res = invoker.execute(request);
            if (logger.isDebugEnabled()) {
                logger.debug("Invocation Response:{}", res);
            }
        } catch (MavenInvocationException e) {
            return new DefaultCompilationResponse(Boolean.FALSE, Optional.of(e.getMessage()));
        } finally {
            if (request.getPomFile() != null) {
                Boolean deleted = request.getPomFile().delete();
                if (logger.isDebugEnabled()) {
                    logger.debug("Deleted {} result:{}", request.getPomFile(), deleted);
                }
            }
        }
        if (res.getExitCode() == 0) {
            return new DefaultCompilationResponse(Boolean.TRUE);
        } else {
            return new DefaultCompilationResponse(Boolean.FALSE);
        }

    }

}