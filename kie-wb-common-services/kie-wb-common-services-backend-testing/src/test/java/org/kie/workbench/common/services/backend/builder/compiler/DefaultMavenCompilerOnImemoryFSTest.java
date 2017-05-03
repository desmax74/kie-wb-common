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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenGoals;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.impl.KieCliRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DefaultMavenCompilerOnImemoryFSTest {

    private final static Path mavenHome = Paths.get("src/test/resources/maven-3.5.0/");
    private final static Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");

    private FileSystem fs;

    @Before
    public void setup() throws Exception {
        if (!Files.exists(mavenRepo)) {
            System.out.println("Creating a m2_repo into src/test/resources/.ignore/m2_repo/");
            if (!Files.exists(Files.createDirectory(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }

        fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @After
    public void tearDown() throws IOException {
        fs.close();
    }

    @Test
    public void buildInMemoryTest() throws IOException {

        Path dummy = fs.getPath("/dummy");
        Path src = fs.getPath("/dummy/src");
        Path main = fs.getPath("/dummy/src/main");
        Path java = fs.getPath("/dummy/src/main/java");
        Path dummyPk = fs.getPath("/dummy/src/main/java/dummy");

        List<Path> folders = new ArrayList<>();
        folders.add(dummy);
        folders.add(src);
        folders.add(main);
        folders.add(java);
        folders.add(dummyPk);
        for (Path item : folders) {
            Files.createDirectory(item);
        }

        Path pom = dummy.resolve("/dummy/pom.xml");
        InputStream isXml = new FileInputStream("src/test/projects/dummy/pom.xml");
        Files.copy(isXml, pom);
        isXml.close();

        Path dummyjava = dummy.resolve("/dummy/src/main/java/dummy/Dummy.java");
        InputStream isJ = new FileInputStream("src/test/projects/dummy/src/main/java/dummy/Dummy.java");
        Files.copy(isJ, dummyjava);
        isJ.close();


        MavenCompiler compiler = new DefaultMavenCompiler(mavenRepo);
        CompilationRequest req = null;
        try {
            KieCliRequest kieCliRequest = new KieCliRequest(dummy, new String[]{MavenGoals.CLEAN});
            req = new DefaultCompilationRequest(kieCliRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        //JGitUtil.

        /*Files.list(dummy).forEach(file -> {
            try {
                System.out.println(String.format("%s (%db)", file, Files.readAllBytes(file).length));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });*/
        //}
    }

}
