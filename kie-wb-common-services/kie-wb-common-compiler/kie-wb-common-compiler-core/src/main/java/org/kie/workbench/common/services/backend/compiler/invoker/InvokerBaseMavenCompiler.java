package org.kie.workbench.common.services.backend.compiler.invoker;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenConfig;
import org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultKieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.incrementalenabler.DefaultIncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.compiler.impl.incrementalenabler.IncrementalCompilerEnabler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.uberfire.java.nio.file.Path;

public class InvokerBaseMavenCompiler<T extends CompilationResponse> implements AFCompiler<T> {

    private static final Logger logger = LoggerFactory.getLogger(BaseMavenCompiler.class);
    private int writeBlockSize = 1024;
    private IncrementalCompilerEnabler enabler;

    public InvokerBaseMavenCompiler(){
        enabler = new DefaultIncrementalCompilerEnabler();
    }

    @Override
    public T compile(CompilationRequest req) {
        MDC.clear();
        MDC.put(MavenConfig.COMPILATION_ID, req.getRequestUUID());
        Thread.currentThread().setName(req.getRequestUUID());
        if (logger.isDebugEnabled()) {
            logger.debug("KieCompilationRequest:{}", req);
        }

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(req.getInfo().getPrjPath().toAbsolutePath().toString()+"/pom.xml"));
        request.setBaseDirectory(req.getInfo().getPrjPath().toFile());
        request.setGoals(Arrays.asList(req.getOriginalArgs()));
        request.setShowErrors(true);


        DefaultInvoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File("/home/garfield/dev/maven-3.3.9/"));
        InvokerLogger logger = new KieInvokerLogger();
        logger.setThreshold(InvokerLogger.INFO);
        invoker.setLogger(logger);

        try {
            InvocationResult result = invoker.execute(request);
            int exitCode = result.getExitCode();
            if (exitCode == 0) {
                return (T) new DefaultKieCompilationResponse(Boolean.TRUE);
            } else {
                return (T) new DefaultKieCompilationResponse(Boolean.FALSE);
            }
        }catch (MavenInvocationException ex){
            logger.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public T compile(CompilationRequest req, Map<Path, InputStream> override) {
        return null;
    }

    public Boolean cleanInternalCache() {
        return enabler.cleanHistory() ;
    }
}
