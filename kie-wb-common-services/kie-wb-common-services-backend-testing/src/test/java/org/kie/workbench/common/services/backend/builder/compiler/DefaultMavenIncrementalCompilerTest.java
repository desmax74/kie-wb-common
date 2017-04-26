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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenGoals;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationRequest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;


public class DefaultMavenIncrementalCompilerTest {

    private final static File mavenHome = new File("src/test/resources/maven-3.5.0/");
    private final static File mavenRepo = new File("src/test/resources/.ignore/m2_repo/");
    private final Path prjFolder = Paths.get("src/test/projects/dummy");
    private final File prj_untouched = new File("src/test/projects/dummy_multimodule_untouched");

    @Before
    public void setUp() throws Exception {
        if (!mavenRepo.exists()) {
            System.out.println("Creating a m2_repo into src/test/resources/.ignore/m2_repo/");
            if (!mavenRepo.mkdir()) {
                throw new Exception("Folder not writable in the project");
            }
        }
    }

    @Test
    public void testIsValidMavenHome() {
        DefaultMavenCompiler compiler = new DefaultMavenCompiler(mavenHome, mavenRepo, Boolean.TRUE);
        Assert.assertTrue(compiler.isValid());
    }

    @Test
    public void testDefaultCompilerWithMAvenHome() {
        DefaultMavenCompiler compiler = new DefaultMavenCompiler(mavenHome, mavenRepo, Boolean.TRUE);
        Assert.assertTrue(compiler.isValid());
    }

    @Test()
    public void testIncompleteArguments() {
        DefaultMavenCompiler compiler = new DefaultMavenCompiler(new File(""), new File(""), Boolean.TRUE);
        Assert.assertFalse(compiler.isValid());
    }

    @Test
    public void testCompileSyncProjectDefaultWithPluginEnabled() {
        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome, mavenRepo, Boolean.TRUE);
        CompilationRequest req = new DefaultCompilationRequest(prjFolder.toFile(), Boolean.FALSE, new ArrayList<>());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
    }

    @Test
    public void testCompileSyncProjectWithPluginEnabled() {
        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome, mavenRepo, Boolean.TRUE);
        CompilationRequest req = new DefaultCompilationRequest(prjFolder.toFile(), Boolean.FALSE, Arrays.asList("compile"));
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
    }

    @Test
    public void testIncrementalWithPluginEnabled() {
        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome, mavenRepo, Boolean.TRUE);

        CompilationRequest req = new DefaultCompilationRequest(prjFolder.toFile(), Boolean.FALSE, Arrays.asList(MavenGoals.CLEAN));
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        Path incrementalFolder = Paths.get("src/test/projects/dummy/target/incremental");
        Assert.assertFalse(incrementalFolder.toFile().exists());

        req = new DefaultCompilationRequest(prjFolder.toFile(), Boolean.FALSE, Arrays.asList(MavenGoals.COMPILE));
        res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        Path incrementalConfiguration = Paths.get("src/test/projects/dummy/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        Assert.assertTrue(incrementalConfiguration.toFile().exists());

        req = new DefaultCompilationRequest(prjFolder.toFile(), Boolean.FALSE, Arrays.asList(MavenGoals.CLEAN));
        res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        Assert.assertFalse(incrementalFolder.toFile().exists());
    }

    @Test
    public void testCompileSyncProjectWithExplicitGoalAndLocalRepo() {
        Assert.assertTrue(DefaultMavenCompiler.isValidMavenHome(mavenHome));
        Assert.assertTrue(DefaultMavenCompiler.isValidMavenRepo(mavenRepo));
        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome, mavenRepo, Boolean.TRUE);
        CompilationRequest req = new DefaultCompilationRequest(prjFolder.toFile(), Boolean.FALSE, Arrays.asList("compile"));
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
    }


    @Test
    public void testIncrementalCompilationWithHiddenPOMTest() throws Exception {

        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome, mavenRepo, new File("src/test/projects/dummy_multimodule_untouched/"), Boolean.FALSE);

        Path incrementalFolder = Paths.get("src/test/projects/dummy_multimodule_untouched/target/incremental");
        Assert.assertFalse(incrementalFolder.toFile().exists());

        byte[] encoded = Files.readAllBytes(Paths.get("src/test/projects/dummy_multimodule_untouched/pom.xml"));
        String pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        CompilationRequest req = new DefaultCompilationRequest(prj_untouched, Boolean.FALSE, Arrays.asList(MavenGoals.COMPILE));
        req.setDebug(Boolean.TRUE);
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        Path incrementalConfiguration = Paths.get("src/test/projects/dummy_multimodule_untouched/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        Assert.assertTrue(incrementalConfiguration.toFile().exists());

        encoded = Files.readAllBytes(Paths.get("src/test/projects/dummy_multimodule_untouched/pom.xml"));
        pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        req = new DefaultCompilationRequest(prj_untouched, Boolean.FALSE, Arrays.asList(MavenGoals.CLEAN));
        req.setDebug(Boolean.TRUE);
        res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
    }

}
