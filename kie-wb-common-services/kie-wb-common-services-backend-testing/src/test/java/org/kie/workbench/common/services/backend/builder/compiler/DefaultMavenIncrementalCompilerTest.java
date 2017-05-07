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
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.impl.KieCliRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class DefaultMavenIncrementalCompilerTest {

    private final static Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");

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
    public void testIsValidMavenHome() {
        DefaultMavenCompiler compiler = new DefaultMavenCompiler(mavenRepo);
        Assert.assertTrue(compiler.isValid());
        KieCliRequest kcr = new KieCliRequest(Paths.get("src/test/projects/dummy"), new String[]{MavenArgs.VERSION, MavenArgs.DEBUG});
        CompilationRequest req = new DefaultCompilationRequest(kcr);
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
    }

    @Test()
    public void testIncompleteArguments() {
        DefaultMavenCompiler compiler = new DefaultMavenCompiler(Paths.get(""));
        Assert.assertFalse(compiler.isValid());
    }


    @Test
    public void testIncrementalWithPluginEnabled() {
        MavenCompiler compiler = new DefaultMavenCompiler(mavenRepo);

        KieCliRequest kcr = new KieCliRequest(Paths.get("src/test/projects/dummy"), new String[]{MavenArgs.CLEAN, MavenArgs.COMPILE, MavenArgs.DEBUG});
        CompilationRequest req = new DefaultCompilationRequest(kcr);
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        Path incrementalConfiguration = Paths.get("src/test/projects/dummy/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        Assert.assertTrue(incrementalConfiguration.toFile().exists());
    }


    @Test
    public void testIncrementalCompilationWithHiddenPOMTest() throws Exception {

        MavenCompiler compiler = new DefaultMavenCompiler(mavenRepo);

        byte[] encoded = Files.readAllBytes(Paths.get("src/test/projects/dummy_multimodule_untouched/pom.xml"));
        String pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        KieCliRequest kcr = new KieCliRequest(Paths.get("src/test/projects/dummy_multimodule_untouched"), new String[]{MavenArgs.CLEAN, MavenArgs.COMPILE, MavenArgs.DEBUG});
        CompilationRequest req = new DefaultCompilationRequest(kcr);

        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        Path incrementalConfiguration = Paths.get("src/test/projects/dummy_multimodule_untouched/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        Assert.assertTrue(incrementalConfiguration.toFile().exists());

        encoded = Files.readAllBytes(Paths.get("src/test/projects/dummy_multimodule_untouched/pom.xml"));
        pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));
    }

}
