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

package org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl;

import org.drools.core.rule.KieModuleMetaInfo;
import org.drools.core.rule.TypeMetaInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.TestUtil;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl.InternalNioImplDefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl.InternalNioImplMavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl.InternalNioImplWorkspaceCompilationInfo;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InternalNioImplKieMetadataTest {

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

        //NIO creation and copy content
        java.nio.file.Path temp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/kjar-2-all-resources"), temp);
        //end NIO

        InternalNioImplMavenCompiler compiler = InternalNioImplMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        InternalNioImplWorkspaceCompilationInfo info = new InternalNioImplWorkspaceCompilationInfo(tmp, compiler);
        InternalNioImplCompilationRequest req = new InternalNioImplDefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE, "-o"}, new HashMap<>());
        CompilationResponse res = compiler.compileSync(req);
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

        // KieModuleMetaData kieModuleMetaData =
        //@TODO continue
        /*KieModule kModule = new KieModu
        KieModuleMetaData metaData =  KieModuleMetaData.Factory.newKieModuleMetaData( kModule, DependencyFilter.COMPILE_FILTER );

        //Check metadata
        final Class clazz = metaData.getClass( packageName,
                className );
        final TypeMetaInfo typeMetaInfo = metaData.getTypeMetaInfo( clazz );
        assertNotNull( typeMetaInfo );
        assertFalse( typeMetaInfo.isEvent() );*/

        InternalNioImplTestUtil.rm(tmpRoot.toFile());
    }
}

