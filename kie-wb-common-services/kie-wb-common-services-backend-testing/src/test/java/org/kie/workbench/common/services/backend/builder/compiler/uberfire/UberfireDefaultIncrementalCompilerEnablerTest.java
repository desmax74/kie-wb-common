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
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.nio.TestUtil;
import org.kie.workbench.common.services.backend.builder.compiler.uberfire.impl.UberfireDefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.uberfire.impl.UberfireDefaultIncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.builder.compiler.uberfire.impl.UberfireMavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.uberfire.impl.UberfireWorkspaceCompilationInfo;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;
import org.uberfire.java.nio.file.api.FileSystemProviders;
import org.uberfire.java.nio.file.spi.FileSystemProvider;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class UberfireDefaultIncrementalCompilerEnablerTest {


    //private final Path prj = Paths.get("src/test/projects/dummy_multimodule");

    //private final Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");

    @Test
    public void testReadPomsInaPrjTest() throws Exception {

        FileSystemProvider fs = FileSystemProviders.getDefaultProvider();
        Path mavenRepo = fs.getPath(URI.create("file://src/test/resources/.ignore/m2_repo/"));
        Path tmpRoot = Files.createTempDirectory("repo");
        //NIO creation and copy content
        java.nio.file.Path temp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/dummy_multimodule_untouched"), temp);
        //end NIO
        Path tmp = Paths.get(tmpRoot.toAbsolutePath().toString(), "dummy");

        Path mainPom = Paths.get(temp.toAbsolutePath().toString(), "pom.xml");

        byte[] encoded = Files.readAllBytes(Paths.get(temp.toAbsolutePath().toString(), "pom.xml"));
        String pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));
        UberfireWorkspaceCompilationInfo info = new UberfireWorkspaceCompilationInfo(tmp, UberfireMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE));
        UberfireCompilationRequest req = new UberfireDefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE});
        UberfireDefaultIncrementalCompilerEnabler enabler = new UberfireDefaultIncrementalCompilerEnabler(Compilers.JAVAC);
        Assert.assertTrue(enabler.process(req));

        encoded = Files.readAllBytes(Paths.get(mainPom.toString()));
        pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertTrue(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        UberfireTestUtil.rm(tmpRoot.toFile());
    }


    @Test
    public void testReadKiePluginTest() throws Exception {

        Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");
        Path tmpRoot = Files.createTempDirectory("repo");

        //NIO creation and copy content
        java.nio.file.Path temp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/dummy_kie_multimodule_untouched"), temp);
        //end NIO
        Path tmp = Paths.get(tmpRoot.toAbsolutePath().toString(), "dummy");

        Path mainPom = Paths.get(tmp.toAbsolutePath().toString(), "pom.xml");
        byte[] encoded = Files.readAllBytes(Paths.get(tmp.toAbsolutePath().toString(), "pom.xml"));
        String pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        UberfireWorkspaceCompilationInfo info = new UberfireWorkspaceCompilationInfo(tmp, UberfireMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE));
        UberfireCompilationRequest req = new UberfireDefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE});
        UberfireDefaultIncrementalCompilerEnabler enabler = new UberfireDefaultIncrementalCompilerEnabler(Compilers.JAVAC);
        Assert.assertTrue(enabler.process(req));

        Assert.assertTrue(info.isKiePluginPresent());

        encoded = Files.readAllBytes(Paths.get(mainPom.toString()));
        pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertTrue(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        Assert.assertTrue(pomAsAstring.contains("kie-maven-plugin"));

        UberfireTestUtil.rm(tmpRoot.toFile());
    }

}
