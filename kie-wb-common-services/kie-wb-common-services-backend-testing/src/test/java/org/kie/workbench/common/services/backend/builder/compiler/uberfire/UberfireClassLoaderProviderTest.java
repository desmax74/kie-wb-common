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

package org.kie.workbench.common.services.backend.builder.compiler.uberfire;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.KieClassLoaderProvider;
import org.kie.workbench.common.services.backend.builder.compiler.TestUtil;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.uberfire.impl.*;
import org.slf4j.Logger;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class UberfireClassLoaderProviderTest {

    //private final Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");

    @Before
    public void setUp() throws Exception {
        Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");
        if (!Files.exists(mavenRepo)) {
            System.out.println("Creating a m2_repo into src/test/resources/.ignore/m2_repo/");
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }
    }


    @Test
    public void loadProjectClassloaderTest() throws Exception {
        //we use NIO for this part of the test because Uberfire lack the implementation to copy a tree
        java.nio.file.Path tmpRoot = java.nio.file.Files.createTempDirectory("repo");
        java.nio.file.Path tmp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/dummy_kie_multimodule_classloader"), tmp);
        Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");
        UberfireMavenCompiler compiler = UberfireMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        Path uberfireTmp = Paths.get(tmp.toAbsolutePath().toString());
        UberfireWorkspaceCompilationInfo info = new UberfireWorkspaceCompilationInfo(uberfireTmp, compiler);
        UberfireCompilationRequest req = new UberfireDefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE, MavenArgs.INSTALL, MavenArgs.DEBUG}, new HashMap<>(), UUID.randomUUID().toString());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());


        //Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");
        KieClassLoaderProvider kieClazzLoaderProvider = new UberfireClassLoaderProviderImpl();
        List<String> pomList = new ArrayList<>();
        UberfireMavenUtils.searchPoms(Paths.get("src/test/projects/dummy_kie_multimodule_classloader/"), pomList);
        Optional<ClassLoader> clazzLoader = kieClazzLoaderProvider.loadDependenciesClassloaderFromProject(pomList, mavenRepo.toAbsolutePath().toString());
        assertNotNull(clazzLoader);
        assertTrue(clazzLoader.isPresent());
        ClassLoader prjClassloader = clazzLoader.get();

        //we try to load the only dep in the prj with a simple call method to see if is loaded or not
        Class clazz;
        try {
            clazz = prjClassloader.loadClass("org.slf4j.LoggerFactory");
            assertFalse(clazz.isInterface());

            Method m = clazz.getMethod("getLogger", String.class);
            Logger logger = (Logger) m.invoke(clazz, "Dummy");
            assertTrue(logger.getName().equals("Dummy"));
            logger.info("dependency loaded from the prj classpath");
        } catch (ClassNotFoundException e) {
            fail();
        }

        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void loadProjectClassloaderFromStringTest() throws Exception {
        //we use NIO for this part of the test because Uberfire lack the implementation to copy a tree
        java.nio.file.Path tmpRoot = java.nio.file.Files.createTempDirectory("repo");
        java.nio.file.Path tmp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/dummy_kie_multimodule_classloader"), tmp);
        Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");
        UberfireMavenCompiler compiler = UberfireMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        Path uberfireTmp = Paths.get(tmp.toAbsolutePath().toString());
        UberfireWorkspaceCompilationInfo info = new UberfireWorkspaceCompilationInfo(uberfireTmp, compiler);
        UberfireCompilationRequest req = new UberfireDefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE, MavenArgs.INSTALL, MavenArgs.DEBUG},new HashMap<>(), UUID.randomUUID().toString());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());


        //Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");
        KieClassLoaderProvider kieClazzLoaderProvider = new UberfireClassLoaderProviderImpl();
        /*List<String> pomList = new ArrayList<>();
        UberfireMavenUtils.searchPoms(Paths.get("src/test/projects/dummy_kie_multimodule_classloader/"), pomList);*/
        Optional<ClassLoader> clazzLoader = kieClazzLoaderProvider.loadDependenciesClassloaderFromProject(uberfireTmp.toAbsolutePath().toString(), mavenRepo.toAbsolutePath().toString());
        assertNotNull(clazzLoader);
        assertTrue(clazzLoader.isPresent());
        ClassLoader prjClassloader = clazzLoader.get();

        //we try to load the only dep in the prj with a simple call method to see if is loaded or not
        Class clazz;
        try {
            clazz = prjClassloader.loadClass("org.slf4j.LoggerFactory");
            assertFalse(clazz.isInterface());

            Method m = clazz.getMethod("getLogger", String.class);
            Logger logger = (Logger) m.invoke(clazz, "Dummy");
            assertTrue(logger.getName().equals("Dummy"));
            logger.info("dependency loaded from the prj classpath");
        } catch (ClassNotFoundException e) {
            fail();
        }

        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void loadTargetFolderClassloaderTest() throws Exception {
        //we use NIO for this part of the test because Uberfire lack the implementation to copy a tree
        java.nio.file.Path tmpRoot = java.nio.file.Files.createTempDirectory("repo");
        java.nio.file.Path tmp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/dummy_kie_multimodule_classloader"), tmp);
        Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");
        UberfireMavenCompiler compiler = UberfireMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        Path uberfireTmp = Paths.get(tmp.toAbsolutePath().toString());
        UberfireWorkspaceCompilationInfo info = new UberfireWorkspaceCompilationInfo(uberfireTmp, compiler);
        UberfireCompilationRequest req = new UberfireDefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE, MavenArgs.DEBUG},new HashMap<>(), UUID.randomUUID().toString());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());


        KieClassLoaderProvider kieClazzLoaderProvider = new UberfireClassLoaderProviderImpl();
        List<String> pomList = new ArrayList<>();
        UberfireMavenUtils.searchPoms(uberfireTmp, pomList);
        Optional<ClassLoader> clazzLoader = kieClazzLoaderProvider.getClassloaderFromProjectTargets(pomList, Boolean.FALSE);
        assertNotNull(clazzLoader);
        assertTrue(clazzLoader.isPresent());
        ClassLoader prjClassloader = clazzLoader.get();

        //we try to load the only dep in the prj with a simple call method to see if is loaded or not
        Class clazz;
        try {
            clazz = prjClassloader.loadClass("dummy.DummyB");
            assertFalse(clazz.isInterface());
            Object obj = clazz.newInstance();

            assertTrue(obj.toString().startsWith("dummy.DummyB"));

            Method m = clazz.getMethod("greetings", new Class[]{});
            Object greeting = m.invoke(obj, new Object[]{});
            assertTrue(greeting.toString().equals("Hello World !"));

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail();
        }

        TestUtil.rm(tmpRoot.toFile());
    }
}
