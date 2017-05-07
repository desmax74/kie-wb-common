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
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.impl.KieCliRequest;
import org.uberfire.java.nio.fs.jgit.util.JGitUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL;
import static org.junit.Assert.*;
import static org.uberfire.java.nio.fs.jgit.util.JGitUtil.branchList;


public class DefaultMavenCompilerOnInMemoryFSTest {

    private final static Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");

    private FileSystem fs;

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Before
    public void setUp() throws Exception {
        if (!Files.exists(mavenRepo)) {
            System.out.println("Creating a m2_repo into src/test/resources/.ignore/m2_repo/");
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
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
    public void buildWithCloneTest() throws IOException {

        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        File temp = tmp.toFile();
        copyTree(Paths.get("src/test/projects/dummy_multimodule_untouched"), Paths.get(temp.toString()));
        File gitFolder = new File(temp, ".repo.git");//@TODO why is mandatory use a .git folder name ?

        Git origin = JGitUtil.newRepository(gitFolder, false);
        assertNotNull(origin);

        JGitUtil.commit(origin,
                "master",
                "name",
                "name@example.com",
                "master-1",
                null,
                null,
                false,
                getFilesToCommit(temp)
        );

        assertEquals(JGitUtil.branchList(origin).size(), 1);

        Path tmpRootCloned = Files.createTempDirectory("cloned");
        Path tmpCloned = Files.createDirectories(Paths.get(tmpRootCloned.toString(), "dummy"));

        final File gitClonedFolder = new File(tmpCloned.toFile(), ".clone.git");
        //clone the repo
        Git cloned = JGitUtil.cloneRepository(gitClonedFolder, origin.getRepository().getDirectory().toString(), false, CredentialsProvider.getDefault());
        assertNotNull(cloned);

        //Compile the repo
        MavenCompiler compiler = new DefaultMavenCompiler(mavenRepo);

        byte[] encoded = Files.readAllBytes(Paths.get(gitClonedFolder + "/dummy/pom.xml"));
        String pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        KieCliRequest kcr = new KieCliRequest(Paths.get(gitClonedFolder + "/dummy/"), new String[]{MavenArgs.CLEAN, MavenArgs.COMPILE, MavenArgs.DEBUG});
        CompilationRequest req = new DefaultCompilationRequest(kcr);

        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        Path incrementalConfiguration = Paths.get(gitClonedFolder + "/dummy/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        Assert.assertTrue(incrementalConfiguration.toFile().exists());

        encoded = Files.readAllBytes(Paths.get(gitClonedFolder + "/dummy/pom.xml"));
        pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        cloned.close();
        origin.close();

        rm(tmpRootCloned.toFile());
        rm(tmpRoot.toFile());
    }

    private Map<String, File> getFilesToCommit(File temp) {
        Map<String, File> map = new HashMap<>();
        map.put("/dummy/pom.xml", new File(temp.toString() + "/pom.xml"));
        map.put("/dummy/dummyA/src/main/java/dummy/DummyA.java", new File(temp.toString() + "/dummyA/src/main/java/dummy/DummyA.java"));
        map.put("/dummy/dummyB/src/main/java/dummy/DummyB.java", new File(temp.toString() + "/dummyB/src/main/java/dummy/DummyB.java"));
        map.put("/dummy/dummyA/pom.xml", new File(temp.toString() + "/dummyA/pom.xml"));
        map.put("/dummy/dummyB/pom.xml", new File(temp.toString() + "/dummyB/pom.xml"));
        return map;
    }


    //Work in progress
    @Test
    public void buildWithPullRebaseTest() throws Exception {

        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        File temp = tmp.toFile();
        copyTree(Paths.get("src/test/projects/dummy_multimodule_untouched"), Paths.get(temp.toString()));
        File gitFolder = new File(temp, ".repo.git");//@TODO why ?

        Git origin = JGitUtil.newRepository(gitFolder, false);
        assertNotNull(origin);

        JGitUtil.commit(origin,
                "master",
                "name",
                "name@example.com",
                "master",
                null,
                null,
                false,
                getFilesToCommit(temp)
        );

        assertEquals(JGitUtil.branchList(origin).size(), 1);

        Path tmpRootCloned = Files.createTempDirectory("cloned");
        Path tmpCloned = Files.createDirectories(Paths.get(tmpRootCloned.toString(), "dummy"));


        /*Ref ref = origin.checkout().setName("master").call();
            System.out.println(ref.getName());
            CheckoutResult ckres = origin.checkout().getResult();
            Assert.assertTrue(ckres.getStatus().equals(CheckoutResult.Status.OK));
        */
        ;

        final File gitClonedFolder = new File(tmpCloned.toFile(), ".clone.git");
        //clone the repo
        Git cloned = JGitUtil.cloneRepository(gitClonedFolder, origin.getRepository().getDirectory().toString(), false, CredentialsProvider.getDefault());
        assertNotNull(cloned);


        assertTrue(JGitUtil.branchList(cloned, ALL).size() == 2);

        assertEquals(branchList(cloned, ALL).get(0).getName(), "refs/heads/master");
        assertEquals(branchList(cloned, ALL).get(1).getName(), "refs/remotes/origin/master");

        //git pull rebase
        //mode 1
        /*PullCommand pullCommand = origin.pull();
        pullCommand.setRemoteBranchName("origin/master");
        pullCommand.setRebase(true);
        PullResult ret = pullCommand.call();
        assertTrue(ret.isSuccessful());
*/

        //mode2
        //PullCommand pc = cloned.pull().setRemote("origin/master").setRebase(Boolean.TRUE);
        PullCommand pc = cloned.pull().setRemote("origin").setRebase(Boolean.TRUE);
        PullResult pullRes = pc.call();
        assertTrue(pullRes.getRebaseResult().getStatus().equals(RebaseResult.Status.UP_TO_DATE));// nothing changed yet

        RebaseCommand rb = cloned.rebase().setUpstream("origin/master");
        RebaseResult rbResult = rb.setPreserveMerges(true).call();
        assertTrue(rbResult.getStatus().isSuccessful());


        //Compile the repo
        MavenCompiler compiler = new DefaultMavenCompiler(mavenRepo);

        byte[] encoded = Files.readAllBytes(Paths.get(gitClonedFolder + "/dummy/pom.xml"));
        String pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        KieCliRequest kcr = new KieCliRequest(Paths.get(gitClonedFolder + "/dummy/"), new String[]{MavenArgs.CLEAN, MavenArgs.COMPILE, MavenArgs.DEBUG});
        CompilationRequest req = new DefaultCompilationRequest(kcr);

        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        Path incrementalConfiguration = Paths.get(gitClonedFolder + "/dummy/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        Assert.assertTrue(incrementalConfiguration.toFile().exists());

        encoded = Files.readAllBytes(Paths.get(gitClonedFolder + "/dummy/pom.xml"));
        pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        cloned.close();
        origin.close();

        rm(tmpRootCloned.toFile());
        rm(tmpRoot.toFile());
    }

    private void copyTree(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new CopyFileVisitor(source, target));
    }

    private void showCommittedFile(Repository gitRepoCloned, String filename) throws IOException {
        ObjectId lastCommitId = gitRepoCloned.resolve(Constants.HEAD);

        try (RevWalk revWalk = new RevWalk(gitRepoCloned)) {
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();

            try (TreeWalk treeWalk = new TreeWalk(gitRepoCloned)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filename));
                while (treeWalk.next()) {
                    if (treeWalk.isSubtree()) {
                        System.out.println("dir: " + treeWalk.getPathString());
                        treeWalk.enterSubtree();
                    } else {
                        System.out.println("\n");
                        System.out.println("file: " + treeWalk.getPathString());
                        ObjectId objectId = treeWalk.getObjectId(0);
                        ObjectLoader loader = gitRepoCloned.open(objectId);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        loader.copyTo(out);
                        System.out.println(out.toString());
                    }
                }
            }
            revWalk.dispose();
        }
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
        Path dummy = path.resolve(contentName);
        Path file = Files.createFile(dummy);
        Assert.assertNotNull(file);
        InputStream isJ = new FileInputStream(origin);
        Files.copy(isJ, dummy);
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

    private void rm(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                rm(c);
        }
        if (!f.delete())
            System.err.println("Couldn't delete file " + f);
    }

    class CopyFileVisitor extends SimpleFileVisitor<Path> {

        private Path srcPath, dstPath;

        CopyFileVisitor(Path srcPath, Path dstPath) {
            this.srcPath = srcPath;
            this.dstPath = dstPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetPath = dstPath.resolve(srcPath.relativize(dir));
            Files.copy(dir, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attr)
                throws IOException {
            Path targetPath = dstPath.resolve(srcPath.relativize(file));
            Files.copy(file, targetPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
        }
    }
}
