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

package org.kie.workbench.common.services.backend.builder.compiler.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.core.rule.KieModuleMetaInfo;
import org.drools.core.rule.TypeMetaInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.builder.KieModule;
import org.kie.scanner.KieModuleMetaData;
import org.kie.scanner.KieModuleMetaDataImpl;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.TestUtil;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOMavenCompiler;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIODefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOMavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOWorkspaceCompilationInfo;
import org.uberfire.java.nio.file.api.FileSystemProviders;
import org.uberfire.java.nio.file.spi.FileSystemProvider;

public class NioKieMetadataTest {

    private Path mavenRepo;

    private Path tmpRoot;

    @After
    public void tearDown() {
        mavenRepo = null;
        TestUtil.rm(tmpRoot.toFile());
    }

    @Before
    public void setUp() throws Exception {
        FileSystemProvider fs = FileSystemProviders.getDefaultProvider();
        tmpRoot = Files.createTempDirectory("repo");

        mavenRepo = Paths.get(System.getProperty("user.home"),
                              "/.m2/repository");

        if (System.getProperty("M2_REPO") == null) {
            if (!Files.exists(mavenRepo)) {
                System.out.println("Creating a m2_repo into" + mavenRepo);
                if (!Files.exists(Files.createDirectories(mavenRepo))) {
                    throw new Exception("Folder not writable in the project");
                }
            }
        }
    }

    @Test
    public void compileAndloadKieJarMetadata() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                     "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/kjar-2-all-resources"),
                          tmp);

        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(mavenRepo,
                                                                        Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(tmp,
                                                                           compiler);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info,
                                                                     new String[]{MavenArgs.COMPILE/*, MavenArgs.DEPS_BUILD_CLASSPATH, sb.toString()*/},
                                                                     new HashMap<>(),
                                                                     Optional.empty());
        CompilationResponse res = compiler.compileSync(req);
        if (res.getErrorMessage().isPresent()) {
            System.out.println(res.getErrorMessage().get());
        }

        Assert.assertTrue(res.isSuccessful());

        Optional<KieModuleMetaInfo> metaDataOptional = res.getKieModuleMetaInfo();
        Assert.assertTrue(metaDataOptional.isPresent());
        KieModuleMetaInfo kieModuleMetaInfo = metaDataOptional.get();
        Assert.assertNotNull(kieModuleMetaInfo);

        Map<String, Set<String>> rulesBP = kieModuleMetaInfo.getRulesByPackage();
        Assert.assertEquals(rulesBP.size(),
                            8);
        Map<String, TypeMetaInfo> typesMI = kieModuleMetaInfo.getTypeMetaInfos();
        Assert.assertEquals(typesMI.size(),
                            35);

        Optional<KieModule> kieModuleOptional = res.getKieModule();
        Assert.assertTrue(kieModuleOptional.isPresent());
        KieModule kModule = kieModuleOptional.get();
        Assert.assertTrue(res.getProjectDependencies().isPresent());
        KieModuleMetaData kieModuleMetaData = new KieModuleMetaDataImpl((InternalKieModule) kModule,
                                                                        res.getProjectDependencies().get());
        //KieModuleMetaData kieModuleMetaData = KieModuleMetaData.Factory.newKieModuleMetaData(kModule); broken with maven embedder used into appformer
        Assert.assertNotNull(kieModuleMetaData);
        //comment if you want read the log file after the test run
        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void compileAndloadKieJarSingleMetadata() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                     "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/kjar-2-single-resources"),
                          tmp);

        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(mavenRepo,
                                                                        Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(tmp,
                                                                           compiler);

        //StringBuilder sb = new StringBuilder(MavenArgs.MAVEN_DEP_PLUGING_OUTPUT_FILE).append(MavenArgs.CLASSPATH_FILENAME).append(MavenArgs.CLASSPATH_EXT);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info,
                                                                     new String[]{MavenArgs.COMPILE/*, MavenArgs.DEPS_BUILD_CLASSPATH, sb.toString()*/},
                                                                     new HashMap<>(),
                                                                     Optional.empty());
        CompilationResponse res = compiler.compileSync(req);
        if (res.getErrorMessage().isPresent()) {
            System.out.println(res.getErrorMessage().get());
        }

        Assert.assertTrue(res.isSuccessful());

        Optional<KieModuleMetaInfo> metaDataOptional = res.getKieModuleMetaInfo();
        Assert.assertTrue(metaDataOptional.isPresent());
        KieModuleMetaInfo kieModuleMetaInfo = metaDataOptional.get();
        Assert.assertNotNull(kieModuleMetaInfo);

        Map<String, Set<String>> rulesBP = kieModuleMetaInfo.getRulesByPackage();
        Assert.assertEquals(rulesBP.size(),
                            1);

        Optional<KieModule> kieModuleOptional = res.getKieModule();
        Assert.assertTrue(kieModuleOptional.isPresent());
        KieModule kModule = kieModuleOptional.get();

        Assert.assertTrue(res.getProjectDependencies().isPresent());
        KieModuleMetaData kieModuleMetaData = new KieModuleMetaDataImpl((InternalKieModule) kModule,
                                                                        res.getProjectDependencies().get());

        //KieModuleMetaData kieModuleMetaData = KieModuleMetaData.Factory.newKieModuleMetaData(kModule); // broken
        Assert.assertNotNull(kieModuleMetaData);

        //comment if you want read the log file after the test run
        TestUtil.rm(tmpRoot.toFile());
    }
}
