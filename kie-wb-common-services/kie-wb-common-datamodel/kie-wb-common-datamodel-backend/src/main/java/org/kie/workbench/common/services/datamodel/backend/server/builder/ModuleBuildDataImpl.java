package org.kie.workbench.common.services.datamodel.backend.server.builder;

import org.guvnor.common.services.backend.file.FileDiscoveryService;
import org.guvnor.common.services.project.builder.model.BuildMessage;
import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.model.Package;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.scanner.KieModuleMetaData;
import org.kie.soup.project.datamodel.commons.util.MVELEvaluator;
import org.kie.soup.project.datamodel.commons.util.RawMVELEvaluator;
import org.kie.soup.project.datamodel.oracle.ModuleDataModelOracle;
import org.kie.soup.project.datamodel.oracle.PackageDataModelOracle;
import org.kie.workbench.common.services.backend.compiler.service.AFCompilerService;
import org.kie.workbench.common.services.backend.file.EnumerationsFileFilter;
import org.kie.workbench.common.services.backend.file.GlobalsFileFilter;
import org.kie.workbench.common.services.backend.project.MapClassLoader;
import org.kie.workbench.common.services.datamodel.spi.DataModelExtension;
import org.kie.workbench.common.services.shared.project.KieModule;
import org.kie.workbench.common.services.shared.project.ProjectImportsService;
import org.kie.workbench.common.services.shared.whitelist.PackageNameWhiteListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.DirectoryStream;
import org.uberfire.java.nio.file.Path;

import javax.enterprise.inject.Instance;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.stream.StreamSupport.stream;

import static org.kie.workbench.common.services.backend.projects.ModuleDataModelOracleBuilder.newModuleOracleBuilder;
import static org.uberfire.backend.server.util.Paths.convert;

public class ModuleBuildDataImpl implements ModuleBuildData {

    private final ReentrantLock lock = new ReentrantLock();

    private static final MapClassLoader EMPTY_CLASSLOADER = new MapClassLoader(Collections.emptyMap(), new URLClassLoader(new URL[0], ClassLoader.getSystemClassLoader().getParent()));
    private static final ModuleDataModelOracle EMPTY_MODULE_ORACLE = newModuleOracleBuilder(new RawMVELEvaluator()).build();

    private static final Logger log = LoggerFactory.getLogger(ModuleBuildDataImpl.class);

    private static final DirectoryStream.Filter<Path> FILTER_ENUMERATIONS = new EnumerationsFileFilter();

    private static final DirectoryStream.Filter<Path> FILTER_GLOBALS = new GlobalsFileFilter();

    private final Executor asyncExecutorQueue = Executors.newSingleThreadExecutor();

    private final IOService ioService;
    private final ProjectImportsService importsService;
    private final PackageNameWhiteListService packageNameWhiteListService;
    private final FileDiscoveryService fileDiscoveryService;
    private final Instance<DataModelExtension> dataModelExtensionsProvider;
    private final MVELEvaluator evaluator;

    private final AFCompilerService compilerService;

    private final KieModule module;
    private final String mavenRepo;

    //private final ComputedValue<KieCompilationResponse> response = new ComputedValue<>();
    private ReleaseId releaseId;

    private KieModuleMetaData kieModuleMetaData;

    //private final ComputedValue<Map<String, byte[]>> declaredTypes = new ComputedValue<>();
    private final Set<String> eventTypes = new HashSet<>();
    private Path artifact;
    private Path workingDir;

    private Set<String> targetArtifacts = new HashSet<>();

    //private final ComputedValue<MapClassLoader> classLoader = new ComputedValue<>();

    //private final ComputedValue<ClassLoader> dependenciesClassLoader = new ComputedValue<>();
    private Set<String> dependencies = new HashSet<>();

    //private ComputedValue<ModuleDataModelOracle> moduleDataModelOracle = new ComputedValue<>();
    //private ComputedValue<Map<String, PackageDataModelOracle>> packageDataModelOracle = new ComputedValue<>();
    private String settingsXML = null;

    public ModuleBuildDataImpl(final AFCompilerService compilerService,
                               final IOService ioService,
                               final ProjectImportsService importsService,
                               final PackageNameWhiteListService packageNameWhiteListService,
                               final FileDiscoveryService fileDiscoveryService,
                               final Instance<DataModelExtension> dataModelExtensionsProvider,
                               final MVELEvaluator evaluator,
                               final KieModule module,
                               final String mavenRepo) {
        this.compilerService = compilerService;
        this.ioService = ioService;
        this.importsService = importsService;
        this.packageNameWhiteListService = packageNameWhiteListService;
        this.fileDiscoveryService = fileDiscoveryService;
        this.dataModelExtensionsProvider = dataModelExtensionsProvider;
        this.evaluator = evaluator;
        this.module = module;
        this.mavenRepo = mavenRepo;
    }

    @Override
    public List<BuildMessage> validate(Path resourcePath,
                                       InputStream inputStream) {
        return null;
    }

    @Override
    public BuildResults build() {
        return null;
    }

    @Override
    public BuildResults buildAndInstall() {
        return null;
    }

    @Override
    public boolean isBuilt() {
        return false;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public ModuleDataModelOracle getModuleDataModelOracle() {
        return null;
    }

    @Override
    public PackageDataModelOracle getPackageDataModelOracle(Package pkg) {
        return null;
    }

    @Override
    public void reBuild(Path changedPath) {

    }

    @Override
    public void reBuild(Collection<Path> changedPaths) {

    }

    @Override
    public Optional<KieContainer> getKieContainer() {
        return Optional.empty();
    }
}

