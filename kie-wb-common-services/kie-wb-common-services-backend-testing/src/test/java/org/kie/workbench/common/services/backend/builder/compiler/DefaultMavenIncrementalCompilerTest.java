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
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.impl.MavenGoals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


public class DefaultMavenIncrementalCompilerTest {

    private final static File mavenHome = new File("src/test/resources/maven-3.5.0/");
    private final static File mavenRepo = new File("src/test/resources/.ignore/m2_repo/");

    @Before
    public void setUp() throws Exception{
        if(!mavenRepo.exists()){
            System.out.println("Creating a m2_repo into src/test/resources/.ignore/m2_repo/");
            if(!mavenRepo.mkdir()){
                throw new Exception("Folder not writable in the project");
            }
        }
    }

    @Test
    public void testIsValidMavenHome() {
        DefaultMavenCompiler compiler = new DefaultMavenCompiler(mavenHome);
        Assert.assertTrue(compiler.isValid());
    }

    @Test
    public void testDefaultCompilerWithMAvenHome(){
        DefaultMavenCompiler compiler = new DefaultMavenCompiler(mavenHome, mavenRepo);
        Assert.assertTrue(compiler.isValid());
    }

    @Test()
    public void testIncompleteArguments() {
        DefaultMavenCompiler compiler = new DefaultMavenCompiler(new File(""), new File(""));
        Assert.assertFalse(compiler.isValid());
    }

    @Test
    public void testCompileSyncProjectDefaultWithPluginEnabled() {
        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome);
        CompilationRequest req = new DefaultCompilationRequest(new File("src/test/projects/dummy"), Boolean.FALSE, new ArrayList<>());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
    }

    @Test
    public void testCompileSyncProjectWithPluginEnabled() {
        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome);
        CompilationRequest req = new DefaultCompilationRequest(new File("src/test/projects/dummy"), Boolean.FALSE, Arrays.asList("compile"));
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
    }

    @Test
    public void testIncrementalWithPluginEnabled() {
        String prjPath = "src/test/projects/dummy";
        File prj = new File(prjPath);

        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome);

        CompilationRequest req = new DefaultCompilationRequest(prj, Boolean.FALSE, Arrays.asList(MavenGoals.CLEAN));
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        File incrementalFolder = new File(prjPath +"/target/incremental");
        Assert.assertFalse(incrementalFolder.exists());

        req = new DefaultCompilationRequest(prj, Boolean.FALSE, Arrays.asList(MavenGoals.COMPILE));
        res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        File incrementalConfiguration = new File(prjPath +"/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        Assert.assertTrue(incrementalConfiguration.exists());

        req = new DefaultCompilationRequest(prj, Boolean.FALSE, Arrays.asList(MavenGoals.CLEAN));
        res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        Assert.assertFalse(incrementalFolder.exists());
    }

    @Test
    public void testCompileSyncProjectWithExplicitGoalAndLocalRepo() {
        Assert.assertTrue(DefaultMavenCompiler.isValidMavenHome(mavenHome));
        Assert.assertTrue(DefaultMavenCompiler.isValidMavenRepo(mavenRepo));
        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome, mavenRepo);
        CompilationRequest req = new DefaultCompilationRequest(new File("src/test/projects/dummy"), Boolean.FALSE, Arrays.asList("compile"));
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
    }

    @Test
    public void testIncrementalWithoutPluginEnabled() {
        String prjPath = "src/test/projects/dummyuntouched";
        File prj = new File(prjPath);

        MavenCompiler compiler = new DefaultMavenCompiler(mavenHome);

        CompilationRequest req = new DefaultCompilationRequest(prj, Boolean.FALSE, Arrays.asList(MavenGoals.CLEAN));
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        File incrementalFolder = new File(prjPath +"/target/incremental");
        Assert.assertFalse(incrementalFolder.exists());

        req = new DefaultCompilationRequest(prj, Boolean.FALSE, Arrays.asList(MavenGoals.COMPILE));
        res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        File incrementalConfiguration = new File(prjPath +"/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        Assert.assertTrue(incrementalConfiguration.exists());

        req = new DefaultCompilationRequest(prj, Boolean.FALSE, Arrays.asList(MavenGoals.CLEAN));
        res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        Assert.assertFalse(incrementalFolder.exists());
    }

}
