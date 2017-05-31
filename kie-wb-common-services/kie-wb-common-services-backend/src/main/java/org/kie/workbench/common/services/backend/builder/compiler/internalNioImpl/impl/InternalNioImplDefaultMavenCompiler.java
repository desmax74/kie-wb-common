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
package org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl;

import org.drools.core.rule.KieModuleMetaInfo;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.external339.KieMavenCli;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.impl.ProcessedPoms;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.InternalNioImplCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.InternalNioImplIncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.InternalNioImplMavenCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;

import java.io.*;
import java.util.Optional;

/**
 * Run maven with https://maven.apache.org/ref/3.5.0/maven-embedder/xref/index.html
 * to use Takari plugins like a black box
 * <p>
 * <p>
 * MavenCompiler compiler = new DefaultMavenCompiler(Paths.get("<path_to_maven_repo>"));
 * WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get("path_to_prj"), URI.create("git://<address></>:<port></>/<repo>"), compiler, cloned);
 * CompilationRequest req = new DefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE});
 * CompilationResponse res = compiler.compileSync(req);
 */
public class InternalNioImplDefaultMavenCompiler implements InternalNioImplMavenCompiler {

    private static final Logger logger = LoggerFactory.getLogger(InternalNioImplDefaultMavenCompiler.class);

    private KieMavenCli cli;

    private Path mavenRepo;

    private InternalNioImplIncrementalCompilerEnabler enabler;

    public InternalNioImplDefaultMavenCompiler(Path mavenRepo) {
        this.mavenRepo = mavenRepo;
        cli = new KieMavenCli();
        enabler = new InternalNioImplDefaultIncrementalCompilerEnabler(Compilers.JAVAC);
    }


    /**
     * Check if the folder exists and if it's writable and readable
     *
     * @param mavenRepo
     * @return
     */
    public static Boolean isValidMavenRepo(Path mavenRepo) {
        if (mavenRepo.getParent() == null)
            return Boolean.FALSE;// used because Path("") is considered for Files.exists...
        return Files.exists(mavenRepo) && Files.isDirectory(mavenRepo) && Files.isWritable(mavenRepo) && Files.isReadable(mavenRepo);
    }


    /**
     * Perform a "mvn -v" call to check if the maven home is correct
     *
     * @return
     */
    @Override
    public Boolean isValid() {
        return isValidMavenRepo(this.mavenRepo);
    }

    @Override
    public Path getMavenRepo() {
        return mavenRepo;
    }


    @Override
    public CompilationResponse compileSync(InternalNioImplCompilationRequest req) {
        if (logger.isDebugEnabled()) {
            logger.debug("KieCompilationRequest:{}", req);
        }

        if (!req.getInfo().getEnhancedMainPomFile().isPresent()) {
            ProcessedPoms processedPoms = enabler.process(req);
            if (!processedPoms.getResult()) {
                return new DefaultCompilationResponse(Boolean.FALSE, Optional.of("Processing poms failed"));
            }
        }
        req.getKieCliRequest().getRequest().setLocalRepositoryPath(mavenRepo.toAbsolutePath().toString());
        int exitCode = cli.doMain(req.getKieCliRequest());
        if (exitCode == 0) {
            if (req.getInfo().isKiePluginPresent()) {
                Optional<KieModuleMetaInfo> kieModuleMetaInfo = readKieModule(req);
                if (kieModuleMetaInfo.isPresent()) {
                    return new DefaultCompilationResponse(Boolean.TRUE, kieModuleMetaInfo.get());
                }
            }
            return new DefaultCompilationResponse(Boolean.TRUE);
        } else {
            return new DefaultCompilationResponse(Boolean.FALSE);
        }
    }

    private Optional<KieModuleMetaInfo> readKieModule(InternalNioImplCompilationRequest req) {

        Optional<KieModuleMetaInfo> kModule = Optional.empty();
        try {
            /** This part is mandatory because the object loaded in the kie maven plugin is
             * loaded in a different classloader and every accessing cause a ClassCastException
             * */
            Object o = req.getKieCliRequest().getMap().get(req.getKieCliRequest().getRequestUUID());
            if (o != null) {
                kModule = Optional.of((KieModuleMetaInfo) readObjectFromADifferentClassloader(o));
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return kModule;
    }

    private Object readObjectFromADifferentClassloader(Object o) {
        ObjectInput in = null;
        ObjectOutput out = null;
        ByteArrayInputStream bis = null;
        ByteArrayOutputStream bos = null;

        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            out.flush();
            byte[] objBytes = bos.toByteArray();
            bis = new ByteArrayInputStream(objBytes);
            in = new ObjectInputStream(bis);
            Object newObj = in.readObject();
            return newObj;
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            try {
                bos.close();

                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
        return null;
    }

}