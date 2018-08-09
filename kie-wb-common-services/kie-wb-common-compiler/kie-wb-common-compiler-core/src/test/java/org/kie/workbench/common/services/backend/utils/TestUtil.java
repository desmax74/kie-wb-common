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

package org.kie.workbench.common.services.backend.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.TestName;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class TestUtil {

    private static Logger logger = LoggerFactory.getLogger(TestUtil.class);
    private static final String JENKINS_SETTINGS_XML_FILE = "JENKINS_SETTINGS_XML_FILE";

    public static void copyTree(Path source,
                                Path target) throws IOException {
        FileUtils.copyDirectory(source.toFile(),
                                target.toFile());
    }

    public static void rm(File f) {
        try{
            FileUtils.deleteDirectory(f);
        }catch (Exception e){
            logger.error("Couldn't delete file {}", f);
            logger.error(e.getMessage(), e);
        }
    }

    public static void writeMavenOutputIntoTargetFolder(final java.nio.file.Path tmp,
                                                        final List<String> mavenOutput,
                                                        final String testName) throws Exception {
        writeMavenOutputIntoTargetFolder(tmp.toFile(), mavenOutput, testName);
    }

    public static void writeMavenOutputIntoTargetFolder(final Path tmp,
                                                        final List<String> mavenOutput,
                                                        final String testName) throws Exception {
        writeMavenOutputIntoTargetFolder(tmp.toFile(), mavenOutput, testName);
    }

    public static void writeMavenOutputIntoTargetFolder(final File tmp,
                                                        final List<String> mavenOutput,
                                                        final String testName) throws Exception {
        //File dir = tmp.toAbsolutePath().toFile();
        File target = new File(tmp.toString() + "/target/");
        if (!target.exists()) {
            logger.info("Creating target folder");
            target.mkdir();
        }
        if (mavenOutput.size() > 0) {
            String sb = target.toString() + testName + ".test.log";
            File fileOut = new File(sb);
            logger.info("Writing error output on {}", fileOut.toString());
            FileOutputStream fos = new FileOutputStream(fileOut);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String item : mavenOutput) {
                bw.write(item);
                bw.newLine();
            }
            bw.close();
        }
    }

    public static String getMavenRepo() throws Exception {
        List<String> repos = Arrays.asList("M2_REPO", "MAVEN_REPO_LOCAL", "MAVEN_REPO", "M2_REPO_LOCAL");
        String mavenRepo = "";
        for (String repo : repos) {
            if (System.getenv(repo) != null) {
                mavenRepo = System.getenv(repo);
                break;
            }
        }
        return StringUtils.isEmpty(mavenRepo) ? createMavenRepo().toAbsolutePath().toString() : mavenRepo;
    }

    public static Path createMavenRepo() throws Exception {
        Path mavenRepository = Paths.get(System.getProperty("user.home"),
                                         "/.m2/repository");
        if (!Files.exists(mavenRepository)) {
            logger.info("Creating a m2_repo into " + mavenRepository);
            if (!Files.exists(Files.createDirectories(mavenRepository))) {
                throw new Exception("Folder not writable in the project");
            }
        }
        return mavenRepository;
    }

    public static Path createAndCopyToDirectory(Path root, String dirName, String copyTree) throws IOException {
        //NIO creation and copy content
        Path dir = Files.createDirectories(Paths.get(root.toString(), dirName));
        TestUtil.copyTree(Paths.get(copyTree), dir);
        return dir;
        //end NIO
    }

    public static String getSettingsFile(){
        String jenkinsFile = System.getenv().get(JENKINS_SETTINGS_XML_FILE);
        if(jenkinsFile != null){
            logger.info("Using settings.xml file provided by JENKINS:{}", jenkinsFile);
            return jenkinsFile;
        }else {
            logger.info("Using local settings.xml file.");
            return new File("src/test/settings.xml").getAbsolutePath();
        }
    }

    public static void saveMavenLogIfCompilationResponseNotSuccessfull(Path tmp, CompilationResponse response, Class<?> testClass, TestName testName) throws Exception{
        String logName = testClass.getSimpleName() + "." + testName.getMethodName();
        if (!response.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmp, response.getMavenOutput(), logName);
        }
    }

    public static void saveMavenLogIfCompilationResponseNotSuccessfull(java.nio.file.Path tmp, CompilationResponse response, Class<?> testClass, TestName testName) throws Exception{
        String logName = testClass.getSimpleName() + "." + testName.getMethodName();
        if (!response.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmp, response.getMavenOutput(), logName);
        }
    }
}
