package org.kie.workbench.common.services.backend.compiler.nio.decorators.kie;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.drools.compiler.kie.builder.impl.FileKieModule;
import org.drools.core.rule.KieModuleMetaInfo;
import org.kie.api.builder.KieModule;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.AFClassLoaderProvider;
import org.kie.workbench.common.services.backend.compiler.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultKieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.nio.NIOCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.nio.NIOKieMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.nio.impl.NIOClassLoaderProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NIOKieAfterDecorator extends NIOKieCompilerDecorator {

    private static final Logger logger = LoggerFactory.getLogger(NIOKieAfterDecorator.class);
    private NIOKieMavenCompiler compiler;

    public NIOKieAfterDecorator(NIOKieMavenCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public KieCompilationResponse compileSync(NIOCompilationRequest req) {
        KieCompilationResponse res = compiler.compileSync(req);
        if (res.isSuccessful()) {

            if (req.getInfo().isKiePluginPresent()) {
                return handleKieMavenPlugin(req,
                                            res);
            }
        }
        return res;
    }

    private KieCompilationResponse handleKieMavenPlugin(NIOCompilationRequest req,
                                                        CompilationResponse res) {

        NIOKieAfterDecorator.KieTuple kieModuleMetaInfoTuple = readKieModuleMetaInfo(req);
        NIOKieAfterDecorator.KieTuple kieModuleTuple = readKieModule(req);
        if (kieModuleMetaInfoTuple.getOptionalObject().isPresent() && kieModuleTuple.getOptionalObject().isPresent()) {

            AFClassLoaderProvider provider = new NIOClassLoaderProviderImpl();
            Optional<List<URI>> optionalDeps = provider.getURISFromAllDependencies(req.getInfo().getPrjPath().toAbsolutePath().toString());
            return new DefaultKieCompilationResponse(Boolean.TRUE,
                                                     (KieModuleMetaInfo) kieModuleMetaInfoTuple.getOptionalObject().get(),
                                                     (KieModule) kieModuleTuple.getOptionalObject().get(),
                                                     res.getMavenOutput(),
                                                     optionalDeps);
        } else {
            StringBuilder sb = new StringBuilder();
            if (kieModuleMetaInfoTuple.getErrorMsg().isPresent()) {
                sb.append(" Error in the kieModuleMetaInfo from the kieMap:").append(kieModuleMetaInfoTuple.getErrorMsg().get());
            }
            if (kieModuleTuple.getErrorMsg().isPresent()) {
                sb.append(" Error in the kieModule:").append(kieModuleTuple.getErrorMsg().get());
            }
            return new DefaultKieCompilationResponse(Boolean.FALSE,
                                                     Optional.of(sb.toString()),
                                                     res.getMavenOutput());
        }
    }

    private NIOKieAfterDecorator.KieTuple readKieModuleMetaInfo(NIOCompilationRequest req) {
        /** This part is mandatory because the object loaded in the kie maven plugin is
         * loaded in a different classloader and every accessing cause a ClassCastException
         * Standard for the kieMap's keys -> compilationID + dot + classname
         * */
        StringBuilder sb = new StringBuilder(req.getKieCliRequest().getRequestUUID()).append(".").append(KieModuleMetaInfo.class.getName());
        Object o = req.getKieCliRequest().getMap().get(sb.toString());
        if (o != null) {

            NIOKieAfterDecorator.KieTuple tuple = readObjectFromADifferentClassloader(o);

            if (tuple.getOptionalObject().isPresent()) {

                return new NIOKieAfterDecorator.KieTuple(tuple.getOptionalObject(),
                                                         Optional.empty());
            } else {

                return new NIOKieAfterDecorator.KieTuple(Optional.empty(),
                                                         tuple.getErrorMsg());
            }
        } else {
            return new NIOKieAfterDecorator.KieTuple(Optional.empty(),
                                                     Optional.of("kieModuleMetaInfo not present in the map"));
        }
    }

    private NIOKieAfterDecorator.KieTuple readKieModule(NIOCompilationRequest req) {

        /** This part is mandatory because the object loaded in the kie maven plugin is
         * loaded in a different classloader and every accessing cause a ClassCastException
         * Standard for the kieMap's keys -> compilationID + dot + classname
         * */
        StringBuilder sb = new StringBuilder(req.getKieCliRequest().getRequestUUID()).append(".").append(FileKieModule.class.getName());
        Object o = req.getKieCliRequest().getMap().get(sb.toString());

        if (o != null) {
            NIOKieAfterDecorator.KieTuple tuple = readObjectFromADifferentClassloader(o);

            if (tuple.getOptionalObject().isPresent()) {

                return new NIOKieAfterDecorator.KieTuple(tuple.getOptionalObject(),
                                                         Optional.empty());
            } else {

                return new NIOKieAfterDecorator.KieTuple(Optional.empty(),
                                                         tuple.getErrorMsg());
            }
        } else {

            return new NIOKieAfterDecorator.KieTuple(Optional.empty(),
                                                     Optional.of("kieModule not present in the map"));
        }
    }

    private NIOKieAfterDecorator.KieTuple readObjectFromADifferentClassloader(Object o) {

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
            return new NIOKieAfterDecorator.KieTuple(Optional.of(newObj),
                                                     Optional.empty());
        } catch (NotSerializableException nse) {
            nse.printStackTrace();
            StringBuilder sb = new StringBuilder("NotSerializableException:").append(nse.getMessage());
            return new NIOKieAfterDecorator.KieTuple(Optional.empty(),
                                                     Optional.of(sb.toString()));
        } catch (IOException ioe) {
            StringBuilder sb = new StringBuilder("IOException:").append(ioe.getMessage());
            return new NIOKieAfterDecorator.KieTuple(Optional.empty(),
                                                     Optional.of(sb.toString()));
        } catch (ClassNotFoundException cnfe) {
            StringBuilder sb = new StringBuilder("ClassNotFoundException:").append(cnfe.getMessage());
            return new NIOKieAfterDecorator.KieTuple(Optional.empty(),
                                                     Optional.of(sb.toString()));
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Exception:").append(e.getMessage());
            return new NIOKieAfterDecorator.KieTuple(Optional.empty(),
                                                     Optional.of(sb.toString()));
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    static class KieTuple {

        private Optional<Object> optionalObj;
        private Optional<String> errorMsg;

        public KieTuple(Optional<Object> optionalObj,
                        Optional<String> errorMsg) {
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