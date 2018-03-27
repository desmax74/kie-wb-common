package org.kie.workbench.common.services.backend.compiler.offprocess;



import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.uberfire.java.nio.file.Paths;

public class Bootstrap {

    public static void main(String[] args) {

            String mavenRepo = args[0] != null ? args[0] : "";
            String tmpRoot = args[1] != null ? args[1] : "";
                    //Files.createTempDirectory("repo");
            String alternateSettingsAbsPath = args[2] != null ? args[2] : "";
            if(mavenRepo .length()>0 && tmpRoot.length() >0 ){
                AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.JGIT_BEFORE_AND_KIE_AND_LOG_AND_CLASSPATH_AFTER);
                WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(tmpRoot));
                CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                                       info,
                                                                       new String[]{MavenCLIArgs.COMPILE,
                                                                               MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath
                                                                       },
                                                                       Boolean.FALSE);
                 CompilationResponse res = compiler.compile(req);
                 // here magic happens with memory mapped file

            }

    }

}
