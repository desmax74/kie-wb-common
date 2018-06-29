package org.kie.workbench.common.services.backend.compiler.offprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenConfig;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultKieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Paths;

public class CompilerIPCCoordinatorImpl implements CompilerIPCCoordinator {

    private String javaHome;
    private String javaBin;
    private int bufferSizeChars = 100;
    private Logger logger = LoggerFactory.getLogger(CompilerIPCCoordinatorImpl.class);
    private String classpath;
    private static String prefixInfoChars = "/tmp/info-";
    private static String prefixInfoObjs = "/tmp/obj-";

    public CompilerIPCCoordinatorImpl(){
        javaHome = System.getProperty("java.home");
        javaBin = javaHome + File.separator + "bin" + File.separator + "java";

    }

    @Override
    public CompilationResponse compile(CompilationRequest req, int secondsTimeout) {
        return internalBuild(req.getMavenRepo(),
                             req.getInfo().getPrjPath().toAbsolutePath().toString(),
                             Paths.get("src/main/resources/compiler_classpath_prj").toAbsolutePath().toString(),
                             getAlternateSettings(req.getOriginalArgs()), secondsTimeout);
    }

    private String getAlternateSettings(String[] args) {
        for(String arg: args){
            if(arg.startsWith(MavenCLIArgs.ALTERNATE_USER_SETTINGS)){
                return arg.substring(2, arg.length());
            }
        }
        return "";
    }



    private CompilationResponse internalBuild(String mavenRepo, String projectPath, String compilerPomPath, String alternateSettingsAbsPath, int secondsTimeout){
        if(classpath == null){
            classpath = setCompilerClasspath(mavenRepo, compilerPomPath);
        }
        try {
            String uuid = UUID.randomUUID().toString();
            invokeServerBuild(mavenRepo, projectPath, uuid, classpath, alternateSettingsAbsPath, secondsTimeout);
            int bufferSizeRes =  ClientIPC.staticListenChars(100, prefixInfoChars+uuid);
            logger.info("bufferSizeRes {}.",bufferSizeRes);
            KieCompilationResponse res = ClientIPC.staticListenObjs(bufferSizeRes, prefixInfoObjs +uuid);
            if(res != null){
                return res;
            }else {
                return new DefaultKieCompilationResponse(true);
            }
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            return new DefaultKieCompilationResponse(false);
        }
    }

    private String setCompilerClasspath(String mavenRepo, String compilerPath){
        String cp = "";
        logger.info("\n********************************\nBuild to generate the classpath\n********************************");
        cp = createClasspathFile(mavenRepo, compilerPath);
        logger.info("\n************************************\nEnd build to generate the classpath\n************************************\n\n");
        return cp;
    }

    private void invokeServerBuild(String mavenRepo, String projectPath, String uuid, String classpath, String alternateSettingsAbsPath, int secondsTimeout) throws Exception {
        String[] commandArrayServer =
                {
                        javaBin,
                        "-cp",
                        System.getProperty("user.dir") + "/" + "target/kie-wb-common-compiler-offprocess-7.8.0-SNAPSHOT.jar:" + classpath,
                        "org.kie.workbench.common.services.backend.compiler.offprocess.ServerIPC",
                        String.valueOf(bufferSizeChars),
                        uuid,
                        projectPath,
                        mavenRepo,
                        alternateSettingsAbsPath
                };
        logger.info("************************** \n Invoking server in a separate process with args: \n{} \n{} \n{} \n{} \n{} \n{} \n{} \n**************************", commandArrayServer);
        ProcessBuilder serverPb = new ProcessBuilder(commandArrayServer);
        File workingDir = new File(projectPath);
        serverPb.directory(workingDir);
        serverPb.redirectErrorStream(true);
        serverPb.inheritIO();
        writeStdOut(serverPb, "Waiting for client.", secondsTimeout);
    }

    private KieCompilationResponse invokeClientReader(String projectPath, String uuid, String classpath, int secondsTimeout) throws Exception{
        String[] commandArray =
                {
                        javaBin,
                        "-cp",
                        System.getProperty("user.dir") +"/"+"target/kie-wb-common-compiler-offprocess-7.8.0-SNAPSHOT.jar:" + classpath,
                        "org.kie.workbench.common.services.backend.compiler.offprocess.ClientIPC",
                        String.valueOf(bufferSizeChars),
                        uuid
                };

        logger.info("************************** \n Invoking client in a separate process with args: \n{} \n{} \n{} \n{} \n{} \n{} \n**************************", commandArray);
        ProcessBuilder pb = new ProcessBuilder(commandArray);
        File workingDir = new File(projectPath);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        writeStdOut(pb, "Received from server.", secondsTimeout);
        return new DefaultKieCompilationResponse(false);
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

    private void writeStdOut(ProcessBuilder builder, String terminationMsg, int secondsTimeout) throws Exception {
        Process process = builder.start();
        process.waitFor(secondsTimeout, TimeUnit.SECONDS);
        BufferedReader reader = new BufferedReader(new InputStreamReader( process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null && !line.endsWith(terminationMsg)) {
                logger.info(line);
            }
            if (line != null) {
                logger.info(line);
                return;
            }
        }
}
