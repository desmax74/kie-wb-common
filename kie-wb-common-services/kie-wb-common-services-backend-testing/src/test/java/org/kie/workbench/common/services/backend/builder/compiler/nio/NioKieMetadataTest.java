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

package org.kie.workbench.common.services.backend.builder.compiler.nio;

import org.drools.core.rule.KieModuleMetaInfo;
import org.drools.core.rule.TypeMetaInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.builder.KieModule;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.TestUtil;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIODefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOMavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOWorkspaceCompilationInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NioKieMetadataTest {

    private final Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");

    @Before
    public void setUp() throws Exception {
        if (!Files.exists(mavenRepo)) {
            System.out.println("Creating a m2_repo into src/test/resources/.ignore/m2_repo/");
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }
    }


    @Test
    public void compileAndloadKieJarMetadata() throws Exception {
        /**
         * If the test fail check if the Drools core classes used, KieModuleMetaInfo and TypeMetaInfo implements Serializable
         * */
        //compile and install
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/kjar-2-all-resources"), tmp);

        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(tmp, compiler);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE }, new HashMap<>());
        CompilationResponse res = compiler.compileSync(req);
        if(res.getErrorMessage().isPresent()){
            System.out.println(res.getErrorMessage().get());
        }
        Assert.assertTrue(res.isSuccessful());
        TestUtil.rm(tmpRoot.toFile());

        Optional<KieModuleMetaInfo> metaDataOptional = res.getKieModuleMetaInfo();
        Assert.assertTrue(metaDataOptional.isPresent());
        KieModuleMetaInfo kieModuleMetaInfo = metaDataOptional.get();
        Assert.assertNotNull(kieModuleMetaInfo);

        Map<String, Set<String>> rulesBP = kieModuleMetaInfo.getRulesByPackage();
        Assert.assertEquals(rulesBP.size(), 8);
        Map<String, TypeMetaInfo> typesMI = kieModuleMetaInfo.getTypeMetaInfos();
        Assert.assertEquals(typesMI.size(), 35);

        Optional<KieModule> kieModuleOptional = res.getKieModule();
        Assert.assertTrue(kieModuleOptional.isPresent());

        TestUtil.rm(tmpRoot.toFile());
    }
}
