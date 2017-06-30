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

package org.kie.workbench.common.services.backend.compiler.plugin;

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
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.TestUtil;
import org.kie.workbench.common.services.backend.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.compiler.internalNioImpl.InternalNioImplCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.internalNioImpl.InternalNioImplMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.internalNioImpl.InternalNioImplTestUtil;
import org.kie.workbench.common.services.backend.compiler.internalNioImpl.impl.InternalNioImplDefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.internalNioImpl.impl.InternalNioImplMavenCompilerFactory;
import org.kie.workbench.common.services.backend.compiler.internalNioImpl.impl.InternalNioImplWorkspaceCompilationInfo;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class InternalNioImplKieMetadataTest {

    private Path mavenRepo;

    @After
    public void tearDown() {
        mavenRepo = null;
    }

    @Before
    public void setUp() throws Exception {
        mavenRepo = Paths.get(System.getProperty("user.home"),
                              "/.m2/repository");

        if (System.getProperty("M2_REPO") == null) {
            if (!Files.exists(mavenRepo)) {
                System.out.println("Creating a m2_repo into:" + mavenRepo.toString());
                if (!Files.exists(Files.createDirectories(mavenRepo))) {
                    throw new Exception("Folder not writable in the project");
                }
            }
        }
    }

    @Test
    public void compileAndLoadKieJarMetadataAllResourcesPackagedJar() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        //compile and install
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(),
                                                     "dummy"));

        //NIO creation and copy content
        java.nio.file.Path temp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(),
                                                                                                "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/kjar-2-all-resources"),
                          temp);
        //end NIO

        InternalNioImplMavenCompiler compiler = InternalNioImplMavenCompilerFactory.getCompiler(
                Decorator.KIE_AFTER);

        InternalNioImplWorkspaceCompilationInfo info = new InternalNioImplWorkspaceCompilationInfo(tmp,
                                                                                                   compiler);
        InternalNioImplCompilationRequest req = new InternalNioImplDefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                                                             info,
                                                                                             new String[]{MavenArgs.INSTALL},
                                                                                             new HashMap<>(),
                                                                                             Optional.empty());
        CompilationResponse res = compiler.compileSync(req);

        if (res.getErrorMessage().isPresent()) {
            System.out.println("Error:" + res.getErrorMessage().get());
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

        Assert.assertTrue(res.getProjectDependencies().isPresent());
        Assert.assertTrue(res.getProjectDependencies().get().size() == 5);
        KieModule kModule = kieModuleOptional.get();

        KieModuleMetaData kieModuleMetaData = new KieModuleMetaDataImpl((InternalKieModule) kModule,
                                                                        res.getProjectDependencies().get());
        Assert.assertNotNull(kieModuleMetaData);
        //comment if you want read the log file after the test run
        InternalNioImplTestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void compileAndloadKieJarSingleMetadata() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        java.nio.file.Path tmpRoot = java.nio.file.Files.createTempDirectory("repo");
        java.nio.file.Path tmp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(),
                                                                                               "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/kjar-2-single-resources"),
                          tmp);

        InternalNioImplMavenCompiler compiler = InternalNioImplMavenCompilerFactory.getCompiler(
                Decorator.KIE_AND_LOG_AFTER);

        InternalNioImplWorkspaceCompilationInfo info = new InternalNioImplWorkspaceCompilationInfo(Paths.get(tmp.toUri()),
                                                                                                   compiler);
        InternalNioImplCompilationRequest req = new InternalNioImplDefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                                                             info,
                                                                                             new String[]{MavenArgs.INSTALL},
                                                                                             new HashMap<>(),
                                                                                             Optional.of("log"));
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.getMavenOutput().isPresent());
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

        Assert.assertTrue(res.getProjectDependencies().isPresent());
        Assert.assertTrue(res.getProjectDependencies().get().size() == 5);

        //comment if you want read the log file after the test run
        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void compileAndloadKieJarSingleMetadataWithPackagedJar() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        java.nio.file.Path tmpRoot = java.nio.file.Files.createTempDirectory("repo");
        java.nio.file.Path tmp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(),
                                                                                               "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/kjar-2-single-resources"),
                          tmp);

        InternalNioImplMavenCompiler compiler = InternalNioImplMavenCompilerFactory.getCompiler(
                Decorator.KIE_AFTER);

        InternalNioImplWorkspaceCompilationInfo info = new InternalNioImplWorkspaceCompilationInfo(Paths.get(tmp.toUri()),
                                                                                                   compiler);
        InternalNioImplCompilationRequest req = new InternalNioImplDefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                                                             info,
                                                                                             new String[]{MavenArgs.INSTALL},
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
        Assert.assertTrue(res.getProjectDependencies().get().size() == 5);

        KieModuleMetaData kieModuleMetaData = new KieModuleMetaDataImpl((InternalKieModule) kModule,
                                                                        res.getProjectDependencies().get());

        //KieModuleMetaData kieModuleMetaData = KieModuleMetaData.Factory.newKieModuleMetaData(kModule); // broken
        Assert.assertNotNull(kieModuleMetaData);

        //comment if you want read the log file after the test run
        TestUtil.rm(tmpRoot.toFile());
    }
}

