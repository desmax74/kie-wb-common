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
package org.kie.workbench.common.services.backend.builder.compiler.nio.impl;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.drools.compiler.kie.builder.impl.FileKieModule;
import org.drools.core.rule.KieModuleMetaInfo;
import org.kie.api.builder.KieModule;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.external339.KieMavenCli;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.impl.ProcessedPoms;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOIncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOMavenCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;


/**
 * Run maven with https://maven.apache.org/ref/3.5.0/maven-embedder/xref/index.html
 * to use Takari plugins like a black box
 * <p>
 * <p>
 * MavenCompiler compiler = new DefaultMavenCompiler(Paths.get("<path_to_maven_repo>"));
 * WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get("path_to_prj"), URI.create("git://<address></>:<port></>/<repo>"), compiler, cloned);
 * CompilationRequest req = new DefaultCompilationRequest(info, new String[]{MavenArgs.COMPILE});
 * CompilationResponse res = compiler.compileSync(req);
 */
public class NIODefaultMavenCompiler implements NIOMavenCompiler {

    private static final Logger logger = LoggerFactory.getLogger(NIODefaultMavenCompiler.class);

    private KieMavenCli cli;

    private Path mavenRepo;

    private NIOIncrementalCompilerEnabler enabler;

    public NIODefaultMavenCompiler(Path mavenRepo) {
        this.mavenRepo = mavenRepo;
        cli = new KieMavenCli();
        enabler = new NIODefaultIncrementalCompilerEnabler(Compilers.JAVAC);
    }

    public NIODefaultMavenCompiler(Path mavenRepo, PrintStream output) {
        this.mavenRepo = mavenRepo;
        cli = new KieMavenCli(output);
        enabler = new NIODefaultIncrementalCompilerEnabler(Compilers.JAVAC);
    }


    /**
     * Check if the folder exists and if it's writable and readable
     *
     * @param mavenRepo
     * @return
     */
    public static Boolean isValidMavenRepo(Path mavenRepo) {
        if (mavenRepo.getParent() == null)
            return Boolean.FALSE;// used because Path("") is considered for Files.exists...
        return Files.exists(mavenRepo) && Files.isDirectory(mavenRepo) && Files.isWritable(mavenRepo) && Files.isReadable(mavenRepo);
    }


    /**
     * Perform a "mvn -v" call to check if the maven home is correct
     *
     * @return
     */
    @Override
    public Boolean isValid() {
        return isValidMavenRepo(this.mavenRepo);
    }

    @Override
    public Path getMavenRepo() {
        return mavenRepo;
    }


    @Override
    public CompilationResponse compileSync(NIOCompilationRequest req) {
        if (logger.isDebugEnabled()) {
            logger.debug("KieCompilationRequest:{}", req);
        }
        ClassLoader contextClassloader = Thread.currentThread().getContextClassLoader();
        if (!req.getInfo().getEnhancedMainPomFile().isPresent()) {
            ProcessedPoms processedPoms = enabler.process(req);
            if (!processedPoms.getResult()) {
                return new DefaultCompilationResponse(Boolean.FALSE, Optional.of("Processing poms failed"));
            }
        }
        req.getKieCliRequest().getRequest().setLocalRepositoryPath(mavenRepo.toAbsolutePath().toString());
        if (req.getInfo().isKiePluginPresent()) {
            StringBuilder sb = new StringBuilder(req.getKieCliRequest().getRequestUUID()).append(".").append(ClassLoader.class.getName());
            req.getKieCliRequest().getMap().put(sb.toString(),contextClassloader);
            //logger.info("NioDefaultMetadataCompiler classloader available with :"+Thread.currentThread().getContextClassLoader());
            //System.out.println("NioDefaultmetadatacompieler classloader available with :"+Thread.currentThread().getContextClassLoader());
            //System.out.println("NioDefaultmetadatacompieler classloader available with key :"+sb.toString());
        }
        int exitCode = cli.doMain(req.getKieCliRequest());
        if (exitCode == 0) {

            Collection<ClassRealm> realms = req.getKieCliRequest().getClassWorld().getRealms();
            for(ClassRealm realm : realms){
                realm.setParentClassLoader(contextClassloader);
            }
            if (req.getInfo().isKiePluginPresent()) {
                return handleKieMavenPlugin(req,contextClassloader);
            }
            return new DefaultCompilationResponse(Boolean.TRUE);

        } else {

            return new DefaultCompilationResponse(Boolean.FALSE);
        }
    }

    private CompilationResponse handleKieMavenPlugin(NIOCompilationRequest req, ClassLoader contextClassloader ) {

        //System.out.println("contextClassloader in handlekiemaven plugin origin:"+Thread.currentThread().getContextClassLoader());

        Thread.currentThread().setContextClassLoader(contextClassloader);
        //System.out.println("contextClassloader in handlekiemaven plugin modified:"+Thread.currentThread().getContextClassLoader());

        StringBuilder sbKieModuleMetaInfo = new StringBuilder(req.getKieCliRequest().getRequestUUID()).append(".").append(KieModuleMetaInfo.class.getName());
        StringBuilder sbFileKieModule = new StringBuilder(req.getKieCliRequest().getRequestUUID()).append(".").append(FileKieModule.class.getName());
        //System.out.println("kieModuleMetaInfo classloader:"+req.getKieCliRequest().getMap().get(sbKieModuleMetaInfo.toString()).getClass().getClassLoader());
        //System.out.println("kieModule classloader:"+req.getKieCliRequest().getMap().get(sbFileKieModule.toString()).getClass().getClassLoader());
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        //System.out.println("current classloader:"+(URLClassLoader)cl);
        KieModuleMetaInfo kieModuleMetaInfo = (KieModuleMetaInfo) req.getKieCliRequest().getMap().get(sbKieModuleMetaInfo.toString());
        FileKieModule kieModule = (FileKieModule) req.getKieCliRequest().getMap().get(sbFileKieModule.toString());


        KieTuple kieModuleMetaInfoTuple = readKieModuleMetaInfo(req);
        KieTuple kieModuleTuple = readKieModule(req);
        if (kieModuleMetaInfoTuple.getOptionalObject().isPresent()  && kieModuleTuple.getOptionalObject().isPresent()) {
             return new DefaultCompilationResponse(Boolean.TRUE, (KieModuleMetaInfo) kieModuleMetaInfoTuple.getOptionalObject().get(), (KieModule) kieModuleTuple.getOptionalObject().get());
        }else{
            StringBuilder sb = new StringBuilder();
            if(kieModuleMetaInfoTuple.getErrorMsg().isPresent()) {
                sb.append(" Error in the kieModuleMetaInfo from the kieMap:").append(kieModuleMetaInfoTuple.getErrorMsg().get());
            }
            if(kieModuleTuple.getErrorMsg().isPresent()){
                sb.append(" Error in the kieModule:").append(kieModuleTuple.getErrorMsg().get());
            }
            return new DefaultCompilationResponse(Boolean.FALSE,
                                                  Optional.of(sb.toString()));
        }
    }

    private KieTuple readKieModuleMetaInfo(NIOCompilationRequest req) {
            /** This part is mandatory because the object loaded in the kie maven plugin is
             * loaded in a different classloader and every accessing cause a ClassCastException
             * Standard for the kieMap's keys -> compilationID + dot + classname
             * */
            StringBuilder sb = new StringBuilder(req.getKieCliRequest().getRequestUUID()).append(".").append(KieModuleMetaInfo.class.getName());
            Object o = req.getKieCliRequest().getMap().get(sb.toString());
            if (o != null) {
                KieTuple tuple = readObjectFromADifferentClassloader(o);
                if(tuple.getOptionalObject().isPresent()){
                    return new KieTuple(tuple.getOptionalObject(), Optional.empty());
                }else{
                    return new KieTuple(Optional.empty(), tuple.getErrorMsg());
                }
            }else{
                return new KieTuple(Optional.empty(), Optional.of("kieModuleMetaInfo not present in the map"));
            }
    }

    private KieTuple readKieModule(NIOCompilationRequest req) {

            /** This part is mandatory because the object loaded in the kie maven plugin is
             * loaded in a different classloader and every accessing cause a ClassCastException
             * Standard for the kieMap's keys -> compilationID + dot + classname
             * */
            StringBuilder sb = new StringBuilder(req.getKieCliRequest().getRequestUUID()).append(".").append(FileKieModule.class.getName());
            byte[] o = (byte[])req.getKieCliRequest().getMap().get(sb.toString());

            if (o != null) {
                KieTuple tuple = readObjectFromADifferentClassloader(o);//readObjectRawFromADifferentClassloader(o);//readObjectFromADifferentClassloader(o);
                if(tuple.getOptionalObject().isPresent()){
                    return new KieTuple(tuple.getOptionalObject(), Optional.empty());
                }else{
                    return new KieTuple(Optional.empty(), tuple.getErrorMsg());
                }
            }else{
                return new KieTuple(Optional.empty(), Optional.of("kieModule not present in the map"));
            }
    }



    private KieTuple readObjectRawFromADifferentClassloader(byte[] stream)  {
        if(stream == null || stream.length == 0){
            return new KieTuple(Optional.empty(), Optional.of("stream empty"));
        }
        ObjectInput in = null;
        ByteArrayInputStream bis = null;

        try {
            bis = new ByteArrayInputStream(stream);
            in = new ObjectInputStream(bis);
            Object newObj = in.readObject();
            return new KieTuple(Optional.of(newObj), Optional.empty());
        }catch (NotSerializableException nse){
            StringBuilder sb = new StringBuilder("NotSerializableException:").append(nse.getMessage());
            return new KieTuple(Optional.empty(), Optional.of(sb.toString()));
        }
        catch (IOException ioe){
            ioe.printStackTrace();
            StringBuilder sb = new StringBuilder("IOException:").append(ioe.getMessage());
            return new KieTuple(Optional.empty(), Optional.of(sb.toString()));
        }
        catch (ClassNotFoundException cnfe){
            StringBuilder sb = new StringBuilder("ClassNotFoundException:").append(cnfe.getMessage());
            return new KieTuple(Optional.empty(), Optional.of(sb.toString()));
        }
        catch (Exception e){
            StringBuilder sb = new StringBuilder("Exception:").append(e.getMessage());
            return new KieTuple(Optional.empty(), Optional.of(sb.toString()));
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    private KieTuple readObjectFromADifferentClassloader(Object o)  {

        ObjectInput in = null;
        ObjectOutput out = null;
        ByteArrayInputStream bis = null;
        ByteArrayOutputStream bos = null;

        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            out.flush();
            byte[] objBytes = bos.toByteArray();
            bis = new ByteArrayInputStream(objBytes);
            in = new ObjectInputStream(bis);
            Object newObj = in.readObject();
            return new KieTuple(Optional.of(newObj), Optional.empty());
        }catch (NotSerializableException nse){
            StringBuilder sb = new StringBuilder("NotSerializableException:").append(nse.getMessage());
            return new KieTuple(Optional.empty(), Optional.of(sb.toString()));
        }
        catch (IOException ioe){
            ioe.printStackTrace();
            StringBuilder sb = new StringBuilder("IOException:").append(ioe.getMessage());
            return new KieTuple(Optional.empty(), Optional.of(sb.toString()));
        }
        catch (ClassNotFoundException cnfe){
            StringBuilder sb = new StringBuilder("ClassNotFoundException:").append(cnfe.getMessage());
            return new KieTuple(Optional.empty(), Optional.of(sb.toString()));
        }
        catch (Exception e){
            StringBuilder sb = new StringBuilder("Exception:").append(e.getMessage());
            return new KieTuple(Optional.empty(), Optional.of(sb.toString()));
        }
        finally {
            try {
                bos.close();
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    class KieTuple{
        private Optional<Object> optionalObj;
        private Optional<String> errorMsg;

        public KieTuple(Optional<Object> optionalObj, Optional<String> errorMsg) {
            this.optionalObj = optionalObj;
            this.errorMsg = errorMsg;
        }

        public Optional<Object> getOptionalObject() {
            return optionalObj;
        }

        public Optional<String> getErrorMsg() {
            return errorMsg;
        }
    }

}