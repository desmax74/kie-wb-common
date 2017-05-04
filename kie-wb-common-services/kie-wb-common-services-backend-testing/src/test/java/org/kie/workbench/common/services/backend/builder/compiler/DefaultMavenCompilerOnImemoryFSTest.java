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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.uberfire.java.nio.fs.jgit.JGitFileSystemProvider;
import org.uberfire.java.nio.fs.jgit.util.JGitUtil;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.uberfire.java.nio.fs.jgit.util.JGitUtil.branchList;


public class DefaultMavenCompilerOnImemoryFSTest {

    private final static Path mavenHome = Paths.get("src/test/resources/maven-3.5.0/");
    private final static Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");

    private FileSystem fs;

    protected static File createTempDirectory() throws IOException {
        final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return temp;
    }

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
    public void buildWithCheckoutTest() throws IOException {

        //Path dummy = createInMemoryFS();
        File temp = createTempDirectory();
        File gitFolder = new File(temp, "repo.git");
        Git origin = JGitUtil.newRepository(gitFolder, true);
        assertNotNull(origin);

        Repository gitRepo = origin.getRepository();

        //URI newRepo = URI.create("git://diff-repo");

        Map<String, Object> env = new HashMap<String, Object>() {{
            put(JGitFileSystemProvider.GIT_ENV_KEY_DEFAULT_REMOTE_NAME, origin.getRepository().getDirectory().toString());
        }};

        JGitUtil.commit(origin,
                "master",
                "name",
                "name@example.com",
                "master-1",
                null,
                null,
                false,
                new HashMap<String, File>() {{
                    put("/dummy/src/main/java/dummy/Dummy.java", createContentWithFile("Dummy", ".java", "src/test/projects/dummy/src/main/java/dummy/Dummy.java"));
                    put("/dummy/pom.xml", createContentWithFile("pom", ".xml", "src/test/projects/dummy/pom.xml"));
                }});

        assertEquals(JGitUtil.branchList(origin).size(), 1);


        final File gitClonedFolder = new File(temp, "myclone.git");
        Git cloned = JGitUtil.cloneRepository(gitClonedFolder, origin.getRepository().getDirectory().toString(), true, CredentialsProvider.getDefault());

        assertTrue(JGitUtil.branchList(cloned, ALL).size()== 2);

        assertEquals(branchList(cloned, ALL).get(0).getName(),"refs/heads/master");
        assertEquals(branchList(cloned, ALL).get(1).getName(),"refs/remotes/origin/master");

        /*MavenCompiler compiler = new DefaultMavenCompiler(mavenRepo);
        CompilationRequest req = null;
        try {
            KieCliRequest kieCliRequest = new KieCliRequest(dummy, new String[]{MavenGoals.CLEAN});
            req = new DefaultCompilationRequest(kieCliRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
*/

    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private Path createInMemoryRepo() throws IOException {
        fs.getFileStores().forEach(file -> {
            StringBuilder sb = new StringBuilder();
            sb.append("name:").append(file.name()).append(" type:").append(file.type());
            System.out.println(sb.toString());
        });

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

        createContentWithNIO(dummy, "/dummy/pom.xml", "src/test/projects/dummy/pom.xml");
        createContentWithNIO(dummy, "/dummy/src/main/java/dummy/Dummy.java", "src/test/projects/dummy/src/main/java/dummy/Dummy.java");

        explore(dummy);
        return dummy;
    }

    private void createContentWithNIO(Path path, String contentName, String origin) throws IOException {
        Path dummyjava = path.resolve(contentName);
        InputStream isJ = new FileInputStream(origin);
        Files.copy(isJ, dummyjava);
        isJ.close();
    }

    private void explore(Path dummy) throws IOException {
        Files.list(dummy).forEach(file -> {
            try {
                if (Files.isRegularFile(file)) {
                    System.out.println(String.format("%s (%db)", file, Files.readAllBytes(file).length));
                } else {
                    System.out.println(file.getFileName() + " is a directory");
                    explore(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public File tempFile(final String content) throws IOException {
        final File file = File.createTempFile("bar", "foo");
        final OutputStream out = new FileOutputStream(file);

        if (content != null && !content.isEmpty()) {
            out.write(content.getBytes());
            out.flush();
        }

        out.close();
        return file;
    }

    public File createContentWithFile(final String fileName, final String suffix, final String source) throws IOException {
        final File file = File.createTempFile(fileName, suffix);
        final OutputStream out = new FileOutputStream(file);

        if (source != null && !source.isEmpty()) {
            out.write(source.getBytes());
            out.flush();
        }

        out.close();
        return file;
    }

}
