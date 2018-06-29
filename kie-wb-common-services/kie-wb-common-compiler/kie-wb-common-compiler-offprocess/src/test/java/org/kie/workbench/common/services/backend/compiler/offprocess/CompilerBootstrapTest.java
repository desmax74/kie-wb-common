/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.services.backend.compiler.offprocess;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenConfig;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.kie.workbench.common.services.backend.compiler.offprocess.utils.TestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class CompilerBootstrapTest {

    private static Path prjPath, compilerPath;
    private Path mavenRepo;
    private String alternateSettingsAbsPath;
    private String uuid;
    private String classpath;
    private Logger logger = LoggerFactory.getLogger(CompilerBootstrapTest.class);
    private String javaHome;
    private String javaBin;

    @Before
    public void setUp() throws Exception {
        javaHome = System.getProperty("java.home");
        javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        mavenRepo = TestUtil.createMavenRepo();
        prjPath = Paths.get("target/test-classes/kjar-2-single-resources");
        compilerPath = Paths.get("target/test-classes/compiler_classpath_prj");
        alternateSettingsAbsPath = new File("src/test/settings.xml").getAbsolutePath();
        uuid = UUID.randomUUID().toString();
        logger.info("\n \n");
        logger.info("\n********************************\nBuild to generate the classpath\n********************************");
        classpath = createClasspathFile(mavenRepo.toAbsolutePath().toString(), compilerPath.toAbsolutePath().toString());
        logger.info("\n************************************\nEnd build to generate the classpath\n************************************\n\n");
    }

    private String createClasspathFile(String mavenRepo, String projectPath) {
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.CLASSPATH_DEPS_AFTER_DECORATOR);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(URI.create("file://" + projectPath)));
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                               info,
                                                               new String[]{MavenConfig.DEPS_IN_MEMORY_BUILD_CLASSPATH},
                                                               Boolean.FALSE);
        CompilationResponse res = compiler.compile(req);
        StringBuffer cp = new StringBuffer();
        for (String dep : res.getDependencies()) {
            cp.append(dep.replace("file:", "")).append(":");
        }
        return cp.toString();
    }


    /*@Test
    public void bootstrapTestSameProcess() throws Exception {
        String[] args = {mavenRepo.toAbsolutePath().toString(), prjPath.toAbsolutePath().toString(), alternateSettingsAbsPath, uuid};
        CompilerBootstrap.main(args);
        CompilationResponse res = OffProcessSharedMap.getCompilationResponse(uuid, Boolean.TRUE);
        assertThat(res).isNotNull();
    }*/

    @Test
    public void bootstrapTestExternalProcess() throws Exception {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String[] commandArray =
                {
                        javaBin,
                        "-cp",
                        System.getProperty("user.dir") +"/"+"target/kie-wb-common-compiler-offprocess-7.8.0-SNAPSHOT.jar:" + classpath,
                        "org.kie.workbench.common.services.backend.compiler.offprocess.CompilerBootstrap",
                        mavenRepo.toAbsolutePath().toString(),
                        prjPath.toAbsolutePath().toString(),
                        alternateSettingsAbsPath,
                        uuid
                };

        File workingDir = new File(prjPath.toAbsolutePath().toString());
        ProcessBuilder pb = runVariant(commandArray, workingDir);
        Process p = pb.start();
        String input = IOUtils.toString(p.getInputStream());
        logger.info("input:{}",input);

        String errOut = IOUtils.toString(p.getErrorStream());
        logger.info("errOut:{}",errOut);
        OutputStream out = p.getOutputStream();

        /*CompilationResponse res = OffProcessSharedMap.getCompilationResponse(uuid, true);
        List<String> deps = res.getDependencies();
        assertThat(deps).hasSize(0);*/
    }

    @Test
    public void bootstrapIPCTestExternalProcess() throws Exception {
        File workingDir = new File(prjPath.toAbsolutePath().toString());
        // run a build to create classpath and request
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_LOG_AND_CLASSPATH_DEPS_AFTER);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(prjPath);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                info,
                                                new String[]{
                                                        MavenCLIArgs.COMPILE,
                                                        MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath
                                                },
                                                Boolean.FALSE,
                                                uuid);
        CompilationResponse res = compiler.compile(req);
        assertThat(res).isNotNull();
        int bufferSize = serialize(res).length; // read the response size
        logger.info("Response size:{}", bufferSize);

        // start server ipc passing the response size as a parameter
        String[] commandArrayServer =
                {
                        javaBin,
                        "-cp",
                        System.getProperty("user.dir") +"/"+"target/kie-wb-common-compiler-offprocess-7.8.0-SNAPSHOT.jar:" + classpath,
                        "org.kie.workbench.common.services.backend.compiler.offprocess.ServerIPCChars",
                        //params
                        String.valueOf(bufferSize),
                        uuid,
                        prjPath.toAbsolutePath().toString(),
                        mavenRepo.toAbsolutePath().toString(),
                        alternateSettingsAbsPath/*,
                        "> nul 2>&1"*/
                };
        logger.info("************************** \n Invoking server in a separate process with args: \n{} \n{} \n{} \n{} \n{} \n{} \n{} \n**************************", commandArrayServer);
        ProcessBuilder serverPb = new ProcessBuilder(commandArrayServer);
        serverPb.directory(workingDir);
        serverPb.redirectErrorStream(true);
        serverPb.inheritIO();
        Process pServer = serverPb.start();
        writeStdOut(pServer, "Waiting for client.");
        //writeStdErr(pServer, "Waiting for client.");


        // start client ipc
        String[] commandArray =
                {
                        javaBin,
                        "-cp",
                        System.getProperty("user.dir") +"/"+"target/kie-wb-common-compiler-offprocess-7.8.0-SNAPSHOT.jar:" + classpath,
                        "org.kie.workbench.common.services.backend.compiler.offprocess.ClientIPCChars",
                        //"org.kie.workbench.common.services.backend.compiler.offprocess.ClientIPCObjects",
                        String.valueOf(bufferSize),
                        uuid,
                        prjPath.toAbsolutePath().toString(),
                        mavenRepo.toAbsolutePath().toString(),
                        alternateSettingsAbsPath/*,
                        "> nul 2>&1"*/
                };

        logger.info("************************** \n Invoking client in a separate process with args: \n{} \n{} \n{} \n{} \n{} \n{} \n**************************", commandArray);
        ProcessBuilder pb = new ProcessBuilder(commandArray);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        writeStdOut(p, "Received from server.");
        //writeStdErr(p, "Received from server.");




        //assertThat(deps).hasSize(0);
    }


    @Test
    public void bootstrapIPCTestExternalProcessWithBuild() throws Exception {
        CompilerIPCCoordinator  compiler = new CompilerIPCCoordinatorImpl();
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(prjPath);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{
                                                                       MavenCLIArgs.COMPILE,
                                                                       MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath
                                                               },
                                                               Boolean.FALSE,
                                                               uuid);
        compiler.compile(req, 15);
    }

    private void writeStdOut(Process pServer, String terminationMsg) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(pServer.getInputStream()));
        String line ;
        while ( (line = reader.readLine()) != null && !line.endsWith(terminationMsg)) {
            logger.info(line);
        }
        if(line != null) {
            logger.info(line);
        }
    }
/*
    private void writeStdErr(Process pServer, String terminationMsg) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(pServer.getErrorStream ()));
        String line ;
        while ( (line = reader.readLine()) != null && !line.endsWith(terminationMsg)) {
            logger.info(line);
        }
        if(line != null) {
            logger.info(line);
        }
    }*/

    private ProcessBuilder runVariant(String commandArray[], File workingDirectory) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(commandArray);
        pb.directory(workingDirectory);
        return pb;
    }

    private static byte[] serialize(Object obj) throws IOException {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

   /* @Test
    public void testCoordinator(){
        CompilerCoordinator coordinator = new CompilerCoordinator();
        coordinator.compile(mavenRepo.toAbsolutePath().toString(), prjPath.toAbsolutePath().toString(), alternateSettingsAbsPath, uuid, classpath);
        //assertThat(response).isNotNull();
    }*/
}
