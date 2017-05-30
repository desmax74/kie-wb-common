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

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kproject.xml.DependencyFilter;
import org.drools.core.rule.KieModuleMetaInfo;
import org.drools.core.rule.TypeMetaInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.builder.KieModule;
import org.kie.internal.utils.KieMeta;
import org.kie.scanner.KieModuleMetaData;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.KieClassLoaderProvider;
import org.kie.workbench.common.services.backend.builder.compiler.TestUtil;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOClassLoaderProviderImpl;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIODefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOMavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOWorkspaceCompilationInfo;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.drools.compiler.kproject.xml.DependencyFilter.COMPILE_FILTER;
import static org.junit.Assert.*;

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

    @Ignore //@Test
    public void compileAndloadKieMetadata() throws Exception {
        //compile and install
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/GuvnorM2RepoDependencyExample2"), tmp);

        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(tmp, compiler);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE, MavenArgs.INSTALL},new HashMap<>());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        TestUtil.rm(tmpRoot.toFile());

        Optional<KieModule> metaDataOptional = res.getKieModule();
        Assert.assertTrue(metaDataOptional.isPresent());
        InternalKieModule kModule = (InternalKieModule) metaDataOptional.get();
        KieModuleMetaData metaData =  KieModuleMetaData.Factory.newKieModuleMetaData( kModule, DependencyFilter.COMPILE_FILTER );

        //Check packages
        final Set<String> packageNames = new HashSet<>();
        final Iterator<String> packageNameIterator = metaData.getPackages().iterator();
        while ( packageNameIterator.hasNext() ) {
            packageNames.add( packageNameIterator.next() );
        }
        assertEquals( 2,
                packageNames.size() );
        assertTrue( packageNames.contains( "defaultpkg" ) );
        assertTrue( packageNames.contains( "org.kie.workbench.common.services.builder.tests.test1" ) );

        //Check classes
        final String packageName = "org.kie.workbench.common.services.builder.tests.test1";
        assertEquals( 1,
                metaData.getClasses( packageName ).size() );
        final String className = metaData.getClasses( packageName ).iterator().next();
        assertEquals( "Bean",
                className );

        //Check metadata
        final Class clazz = metaData.getClass( packageName,
                className );
        final TypeMetaInfo typeMetaInfo = metaData.getTypeMetaInfo( clazz );
        assertNotNull( typeMetaInfo );
        assertFalse( typeMetaInfo.isEvent() );


    }


    @Test
    public void compileAndloadKieJarMetadata() throws Exception {
        //compile and install
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/kjar-2-all-resources"), tmp);

        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(tmp, compiler);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE, MavenArgs.INSTALL},new HashMap<>());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        TestUtil.rm(tmpRoot.toFile());

        Optional<KieModule> metaDataOptional = res.getKieModule();
        Assert.assertTrue(metaDataOptional.isPresent());
        InternalKieModule kModule = (InternalKieModule) metaDataOptional.get();
        KieModuleMetaData metaData =  KieModuleMetaData.Factory.newKieModuleMetaData( kModule, DependencyFilter.COMPILE_FILTER );

        //Check packages
        final Set<String> packageNames = new HashSet<>();
        final Iterator<String> packageNameIterator = metaData.getPackages().iterator();
        while ( packageNameIterator.hasNext() ) {
            packageNames.add( packageNameIterator.next() );
        }
        assertEquals( 2,
                packageNames.size() );
        assertTrue( packageNames.contains( "defaultpkg" ) );
        assertTrue( packageNames.contains( "org.kie.workbench.common.services.builder.tests.test1" ) );

        //Check classes
        final String packageName = "org.kie.workbench.common.services.builder.tests.test1";
        assertEquals( 1,
                metaData.getClasses( packageName ).size() );
        final String className = metaData.getClasses( packageName ).iterator().next();
        assertEquals( "Bean",
                className );

        //Check metadata
        final Class clazz = metaData.getClass( packageName,
                className );
        final TypeMetaInfo typeMetaInfo = metaData.getTypeMetaInfo( clazz );
        assertNotNull( typeMetaInfo );
        assertFalse( typeMetaInfo.isEvent() );


    }
}
