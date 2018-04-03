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
 *
 */

package org.kie.workbench.common.services.backend.builder.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.enterprise.inject.Instance;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.guvnor.common.services.backend.file.FileDiscoveryService;
import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.model.Package;
import org.guvnor.common.services.shared.builder.model.BuildMessage;
import org.kie.api.builder.ReleaseId;
import org.kie.api.definition.type.Role;
import org.kie.api.runtime.KieContainer;
import org.kie.scanner.KieModuleMetaData;
import org.kie.scanner.KieModuleMetaDataImpl;
import org.kie.soup.project.datamodel.commons.util.MVELEvaluator;
import org.kie.soup.project.datamodel.commons.util.RawMVELEvaluator;
import org.kie.soup.project.datamodel.imports.Import;
import org.kie.soup.project.datamodel.oracle.ModuleDataModelOracle;
import org.kie.soup.project.datamodel.oracle.PackageDataModelOracle;
import org.kie.soup.project.datamodel.oracle.TypeSource;
import org.kie.workbench.common.services.backend.compiler.impl.classloader.CompilerClassloaderUtils;
import org.kie.workbench.common.services.backend.compiler.impl.classloader.MapClassLoader;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.service.AFCompilerService;
import org.kie.workbench.common.services.backend.compiler.service.executors.CompilerLogLevel;
import org.kie.workbench.common.services.backend.file.EnumerationsFileFilter;
import org.kie.workbench.common.services.backend.file.GlobalsFileFilter;
import org.kie.workbench.common.services.datamodel.backend.server.builder.packages.PackageDataModelOracleBuilder;
import org.kie.workbench.common.services.datamodel.backend.server.builder.projects.ModuleDataModelOracleBuilder;
import org.kie.workbench.common.services.datamodel.spi.DataModelExtension;
import org.kie.workbench.common.services.shared.project.KieModule;
import org.kie.workbench.common.services.shared.project.ProjectImportsService;
import org.kie.workbench.common.services.shared.whitelist.PackageNameWhiteListService;
import org.kie.workbench.common.services.shared.whitelist.WhiteList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.DirectoryStream;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.kie.workbench.common.services.backend.compiler.impl.classloader.CompilerClassloaderUtils.filterClassesByPackage;
import static org.kie.workbench.common.services.backend.compiler.impl.classloader.CompilerClassloaderUtils.filterPathClasses;
import static org.kie.workbench.common.services.backend.compiler.impl.classloader.CompilerClassloaderUtils.getMapClasses;
import static org.kie.workbench.common.services.backend.compiler.impl.utils.MavenOutputConverter.convertIntoBuildResults;
import static org.kie.workbench.common.services.datamodel.backend.server.builder.projects.ModuleDataModelOracleBuilder.newModuleOracleBuilder;
import static org.uberfire.backend.server.util.Paths.convert;

public class ProjectBuildDataImpl implements ProjectBuildData {

    private final ReentrantLock lock = new ReentrantLock();

    private static final MapClassLoader EMPTY_CLASSLOADER = new MapClassLoader(Collections.emptyMap(), new URLClassLoader(new URL[0], ClassLoader.getSystemClassLoader().getParent()));
    private static final ModuleDataModelOracle EMPTY_MODULE_ORACLE = newModuleOracleBuilder(new RawMVELEvaluator()).build();

    private static final Logger log = LoggerFactory.getLogger(ProjectBuildDataImpl.class);

    private static final DirectoryStream.Filter<Path> FILTER_ENUMERATIONS = new EnumerationsFileFilter();

    private static final DirectoryStream.Filter<Path> FILTER_GLOBALS = new GlobalsFileFilter();

    private final IOService ioService;
    private final ProjectImportsService importsService;
    private final PackageNameWhiteListService packageNameWhiteListService;
    private final FileDiscoveryService fileDiscoveryService;
    private final Instance<DataModelExtension> dataModelExtensionsProvider;
    private final MVELEvaluator evaluator;

    private final AFCompilerService compilerService;

    private final KieModule module;
    private final String mavenRepo;

    private KieCompilationResponse response;
    private ReleaseId releaseId;

    private KieModuleMetaData kieModuleMetaData;

    private final Map<String, byte[]> declaredTypes = new HashMap<>();
    private final Set<String> eventTypes = new HashSet<>();
    private Path artifact;
    private Path workingDir;

    private Set<String> targetArtifacts = new HashSet<>();

    private MapClassLoader classLoader;

    private ClassLoader dependenciesClassLoader;
    private Set<String> dependencies = new HashSet<>();

    private ModuleDataModelOracle moduleDataModelOracle;
    private Map<String, PackageDataModelOracle> packageDataModelOracle = new HashMap<>();
    private AtomicBoolean isReBuild = new AtomicBoolean(false);

    public ProjectBuildDataImpl(final AFCompilerService compilerService,
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
    public List<BuildMessage> validate(final Path resourcePath,
                                       final InputStream inputStream, CompilerLogLevel logLevel) {
        lock.lock();
        try {
            final KieCompilationResponse res = compilerService.build(convert(module.getRootPath()), mavenRepo, Collections.singletonMap(resourcePath, inputStream), logLevel).get();

            final BuildResults br = convertIntoBuildResults(res.getMavenOutput(),
                                                            convert(module.getRootPath()),
                                                            res.getWorkingDir().get().toString());

            return br.getMessages();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BuildResults build(CompilerLogLevel logLevel) {
        lock.lock();
        try {
            final KieCompilationResponse res = getCompilationResponse(logLevel);
            final BuildResults br = convertIntoBuildResults(res.getMavenOutput(),
                                                            convert(module.getRootPath()),
                                                            res.getWorkingDir().get().getParent().toString());

            return br;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BuildResults buildAndInstall(CompilerLogLevel logLevel) {
        lock.lock();
        try {
            final KieCompilationResponse res = compilerService.buildAndInstall(convert(module.getRootPath()), mavenRepo, logLevel).get();

            return convertIntoBuildResults(res.getMavenOutput(),
                                           convert(module.getRootPath()),
                                           res.getWorkingDir().get().getParent().toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isBuilt() {
        return response != null;
    }

    @Override
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            return doGetClassLoader();
        }
        return classLoader;
    }

    private ClassLoader doGetClassLoader() {
        lock.lock();
        try {
            if (classLoader == null || isReBuild.get()) {
                classLoader = buildClassLoader();
            }
            return classLoader;
        } finally {
            lock.unlock();
        }
    }

    private MapClassLoader buildClassLoader() {
        lock.lock();
        try {
            final KieCompilationResponse res = getCompilationResponse(CompilerLogLevel.STANDARD);

            if (res.isSuccessful()) {

                res.getKieModule().ifPresent(km -> {
                    kieModuleMetaData = new KieModuleMetaDataImpl((InternalKieModule) km,
                                                                  res.getDependenciesAsURI());
                    releaseId = km.getReleaseId();
                });

                /* absolute path on the fs */
                workingDir = res.getWorkingDir().get();
                artifact = res.getWorkingDir().map(w -> {
                    final Path result = w.resolve("target").resolve(module.getPom().getGav().getArtifactId() + "-" + module.getPom().getGav().getVersion() + ".jar");
                    if (result.toFile().exists()) {
                        return result;
                    }
                    return null;
                }).orElse(null);

                /* we collects all the thing produced in the target/classes folders */
                targetArtifacts = new HashSet<>(res.getTargetContent().stream().filter(s -> !s.endsWith(".jar")).collect(toSet()));
                final ClassLoader dependenciesClassLoader = getDependenciesClassLoader(res);
                final Map<String, byte[]> store = new HashMap<>();
                if (res.getProjectClassLoaderStore() != null) {
                    store.putAll(res.getProjectClassLoaderStore());
                    declaredTypes.putAll(store);
                }
                if (res.getEventTypeClasses() != null){
                    eventTypes.addAll(res.getEventTypeClasses());
                }
                /** The integration works with CompilerClassloaderUtils.getMapClasses
                 * This MapClassloader needs the .class from the target folders in a prj produced by the build, as a Map
                 * with a key like this "curriculumcourse/curriculumcourse/Curriculum.class" and the byte[] as a value */
                return new MapClassLoader(getMapClasses(workingDir.toString(), store), dependenciesClassLoader);
            }
            return EMPTY_CLASSLOADER;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ModuleDataModelOracle getModuleDataModelOracle() {
        if (moduleDataModelOracle == null) {
            return doGetModuleDataModelOracle();
        }
        return moduleDataModelOracle;
    }

    private ModuleDataModelOracle doGetModuleDataModelOracle() {
        lock.lock();
        try {
            if (moduleDataModelOracle == null || isReBuild.get()) {
                moduleDataModelOracle = buildModuleDataModelOracle();
            }
            return moduleDataModelOracle;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PackageDataModelOracle getPackageDataModelOracle(final Package pkg) {
        if (!packageDataModelOracle.containsKey(pkg.getPackageName())) {
            return doGetPackageDataModelOracle(pkg);
        }
        return packageDataModelOracle.get(pkg.getPackageName());
    }

    private PackageDataModelOracle doGetPackageDataModelOracle(final Package pkg) {
        lock.lock();
        try {
            if (!packageDataModelOracle.containsKey(pkg.getPackageName())) {
                packageDataModelOracle.put(pkg.getPackageName(), buildPackageDataModelOracle(pkg));
            }
            return packageDataModelOracle.get(pkg.getPackageName());
        } finally {
            lock.unlock();
        }
    }

    private PackageDataModelOracle buildPackageDataModelOracle(final Package pkg) {
        final String packageName = pkg.getPackageName();
        final PackageDataModelOracleBuilder dmoBuilder = PackageDataModelOracleBuilder.newPackageOracleBuilder(evaluator,
                                                                                                               packageName);
        final ModuleDataModelOracle modelOracle = doGetModuleDataModelOracle();
        dmoBuilder.setModuleOracle(modelOracle);

        if (modelOracle.equals(EMPTY_MODULE_ORACLE)) {
            return dmoBuilder.build();
        }

        //Add Guvnor enumerations
        loadEnumsForPackage(dmoBuilder, pkg);

        //Add DSLs
        loadExtensionsForPackage(dmoBuilder, pkg);

        //Add Globals
        loadGlobalsForPackage(dmoBuilder, pkg);

        return dmoBuilder.build();
    }

    private void loadEnumsForPackage(final PackageDataModelOracleBuilder dmoBuilder,
                                     final Package pkg) {
        final Path nioPackagePath = Paths.convert(pkg.getPackageMainResourcesPath());
        final Collection<Path> enumFiles = fileDiscoveryService.discoverFiles(nioPackagePath,
                                                                              FILTER_ENUMERATIONS);
        for (final Path path : enumFiles) {
            final String enumDefinition = ioService.readAllString(path);
            dmoBuilder.addEnum(enumDefinition, classLoader);
        }
    }

    private void loadExtensionsForPackage(final PackageDataModelOracleBuilder dmoBuilder,
                                          final Package pkg) {
        final Path nioPackagePath = Paths.convert(pkg.getPackageMainResourcesPath());
        final List<DataModelExtension> extensions = stream(dataModelExtensionsProvider.spliterator(), false).collect(toList());

        for (final DataModelExtension extension : extensions) {
            DirectoryStream.Filter<Path> filter = extension.getFilter();
            final Collection<Path> extensionFiles = fileDiscoveryService.discoverFiles(nioPackagePath,
                                                                                       filter);
            extensionFiles
                    .stream()
                    .map(file -> extension.getExtensions(file,
                                                         ioService.readAllString(file)))
                    .forEach(mappings -> mappings.forEach(mapping -> dmoBuilder.addExtension(mapping.getKind(),
                                                                                             mapping.getValues())));
        }
    }

    private void loadGlobalsForPackage(final PackageDataModelOracleBuilder dmoBuilder,
                                       final Package pkg) {
        final Path nioPackagePath = Paths.convert(pkg.getPackageMainResourcesPath());
        final Collection<Path> globalFiles = fileDiscoveryService.discoverFiles(nioPackagePath,
                                                                                FILTER_GLOBALS);
        for (final Path path : globalFiles) {
            final String definition = ioService.readAllString(path);
            dmoBuilder.addGlobals(definition);
        }
    }

    @Override
    public void reBuild(final Path changedPath) {
        reBuild(singletonList(changedPath));
    }

    @Override
    public void reBuild(final Collection<Path> changedPaths) {
        lock.lock();
        try {
            isReBuild.set(true);
            final List<String> packages = clearTargetAndListPackages(changedPaths);
            moduleDataModelOracle = buildModuleDataModelOracle();
            packages.parallelStream().forEach(p -> packageDataModelOracle.remove(p));
            isReBuild.set(false);
        } finally {
            lock.unlock();
        }
    }

    private List<String> clearTargetAndListPackages(final Collection<Path> changedPaths) {
        final List<String> packages = new ArrayList<>();
//        if (workingDir != null && workingDir.resolve("target/classes").toFile().exists()) {
//            final Path baseTargetPath = workingDir.resolve("target/classes");
        for (final Path path : changedPaths) {
            final Path basePath = path.subpath(convert(module.getRootPath()).getNameCount(), path.getNameCount());
            if (basePath.startsWith("src/main/java") || basePath.startsWith("src/main/resources")) {
                final Path relativeFilePath = path.subpath(convert(module.getRootPath()).getNameCount() + 3, path.getNameCount());
                final Path filePackage = relativeFilePath.subpath(0, relativeFilePath.getNameCount() - 1);
                packages.add(filePackage.toString().replace('/', '.'));
//                    final String fullFileName = path.getFileName().toString();
//                    final String fileName = fullFileName.substring(0, fullFileName.lastIndexOf('.'));
//                    Files.deleteIfExists(baseTargetPath.resolve(filePackage.resolve(fileName + ".class")));
//                }
            }
        }
        return packages;
    }

    private KieCompilationResponse getCompilationResponse(CompilerLogLevel logLevel) {
        if (response == null || isReBuild.get()) {
            try {
                response = compilerService.build(convert(module.getRootPath()), mavenRepo, logLevel).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return response;
    }

    private ClassLoader getDependenciesClassLoader(final KieCompilationResponse res) {
        if ((dependenciesClassLoader == null || dependencies == null || dependencies.isEmpty() ||
                (!res.getDependencies().isEmpty() && res.getDependencies().size() == dependencies.size() && res.getDependencies().containsAll(dependencies)))) {
            classLoader = null;
            dependencies = new HashSet<>(res.getDependencies());
            dependenciesClassLoader = new URLClassLoader(res.getDependenciesAsURL().toArray(new URL[res.getDependenciesAsURL().size()]));
        }
        return dependenciesClassLoader;
    }

    private ModuleDataModelOracle buildModuleDataModelOracle() {
        doGetClassLoader();
        if (kieModuleMetaData == null) {
            return EMPTY_MODULE_ORACLE;
        }
        return new InnerBuilder().build();
    }

    class InnerBuilder {

        private final ModuleDataModelOracleBuilder pdBuilder = newModuleOracleBuilder(evaluator);

        public ModuleDataModelOracle build() {
            lock.lock();
            try {
                final WhiteList whiteList = getFilteredPackageNames();
                for (final String packageName : whiteList) {
                    pdBuilder.addPackage(packageName);
                    addClasses(packageName,
                               kieModuleMetaData.getClasses(packageName));
                    addClasses(packageName,
                               filterClassesByPackage(targetArtifacts, packageName),
                               TypeSource.JAVA_PROJECT);
                }

                if (!declaredTypes.isEmpty()) {
                    for (final String packageName : whiteList) {
                        List<Class<?>> clazzes = getClazz(packageName);
                        if (!clazzes.isEmpty()) {
                            addClass(clazzes, TypeSource.DECLARED);
                        }
                    }
                }

                if (Files.exists(Paths.convert(module.getImportsPath()))) {
                    for (final Import item : getImports()) {
                        addClass(item);
                    }
                }

                return pdBuilder.build();
            } finally {
                lock.unlock();
            }
        }

        private boolean isEvent(final String className,
                                final Class<?> clazz) {
            if (!eventTypes.isEmpty()) {
                return eventTypes.contains(className);
            } else {
                if (clazz.isAnnotationPresent(Role.class)) {
                    Role.Type value = clazz.getAnnotation(Role.class).value();
                    return value.equals(Role.Type.EVENT);
                } else {
                    return false;
                }
            }
        }

        /**
         * @return A "white list" of package names that are available for authoring
         */
        private WhiteList getFilteredPackageNames() {
            final Collection<String> pkgs = kieModuleMetaData.getPackages();
            //@TODO change /global with guvnor repo
            pkgs.addAll(filterPathClasses(targetArtifacts, "global/"));
            return packageNameWhiteListService.filterPackageNames(module, pkgs);
        }

        private void addClasses(final String packageName,
                                final Collection<String> classes) {
            for (final String className : classes) {
                addClass(packageName, className);
            }
        }

        private void addClasses(final String packageName, final Collection<String> classes, TypeSource typeSource) {
            for (final String className : classes) {
                addClass(packageName, className, typeSource);
            }
        }

        private void addClass(final List<Class<?>> clazzes, TypeSource typeSource) {
            try {
                for (Class clazz : clazzes) {
                    pdBuilder.addClass(clazz, false, typeSource);
                }
            } catch (IOException ioe) {
                log.debug(ioe.getMessage());
            }
        }

        private void addClass(final Import item) {
            try {
                Class clazz = classLoader.loadClass(item.getType());
                pdBuilder.addClass(clazz, false, TypeSource.JAVA_DEPENDENCY);
            } catch (ClassNotFoundException | IOException cnfe) {
                //Class resolution would have happened in Builder and reported as warnings so log error here at debug level to avoid flooding logs
                log.debug(cnfe.getMessage());
            }
        }

        private void addClass(final String packageName, final String className) {
            try {
                if (classLoader != null) {
                    final Class clazz = CompilerClassloaderUtils.getClass(packageName,
                                                                          className,
                                                                          classLoader);
                    if (clazz != null) {
                        pdBuilder.addClass(clazz,
                                           kieModuleMetaData.getTypeMetaInfo(clazz).isEvent(),
                                           defineType(clazz));
                    }
                }
            } catch (Throwable e) {
                //Class resolution would have happened in Builder and reported as warnings so log error here at debug level to avoid flooding logs
                log.debug(e.getMessage());
            }
        }

        private void addClass(final String packageName, final String className, TypeSource typeSource) {
            try {
                if (classLoader != null) {

                    final Class clazz = CompilerClassloaderUtils.getClass(packageName,
                                                                          className,
                                                                          classLoader);

                    if (clazz != null) {
                        pdBuilder.addClass(clazz, isEvent(className, clazz), typeSource);
                    }
                }
            } catch (Throwable e) {
                //Class resolution would have happened in Builder and reported as warnings so log error here at debug level to avoid flooding logs
                log.debug(e.getMessage());
            }
        }

        private List<Import> getImports() {
            return importsService.load(module.getImportsPath()).getImports().getImports();
        }
    }

    private TypeSource defineType(Class clazz) {
        final String typeName = clazz.getCanonicalName();
        if (kieModuleMetaData.getTypeMetaInfo(clazz).isDeclaredType()) {
            return TypeSource.DECLARED;
        }
        if (targetArtifacts.contains(typeName.replace('.', '/') + ".class")) {
            return TypeSource.JAVA_PROJECT;
        }
        return TypeSource.JAVA_DEPENDENCY;
    }

    private List<Class<?>> getClazz(final String _packageName) {
        final String packageName = _packageName.replace('.', '/');
        if (classLoader != null) {
            if (!classLoader.getKeys().isEmpty()) {
                final List<Class<?>> clazzes = new ArrayList<>();
                for (String key : classLoader.getKeys()) {
                    if (key.startsWith(packageName) && declaredTypes.keySet().contains(key)) {
                        try {
                            Class clazz = classLoader.loadClass(key.substring(0, key.lastIndexOf(".")).replace("/", "."));
                            if (clazz != null) {
                                clazzes.add(clazz);
                            }
                        } catch (Exception e) {
                            //nothing to do
                        }
                    }
                }
                return clazzes;
            }
        }

        return Collections.emptyList();
    }

    @Override
    public Optional<KieContainer> getKieContainer() {
        lock.lock();
        try {
            final Optional<ReleaseId> releaseId = getReleaseId();
            if (!releaseId.isPresent()) {
                return Optional.empty();
            }

            final Optional<Path> results = getArtifact();
            return null;
//            return results.map(r -> {
//                final KieServices ks = KieServices.get();
//                KieModule km = KieServices.get().getRepository().getKieModule(releaseId.get());
//                if (km == null) {
//                    final Resource jarRes = ks.getResources().newFileSystemResource(r.toFile());
//                    km = ks.getRepository().addKieModule(jarRes);
//                }
//                return Optional.of(ks.newKieContainer(km.getReleaseId()));
//            }).orElse(Optional.empty());
        } finally {
            lock.unlock();
        }
    }

    private Optional<ReleaseId> getReleaseId() {
        if (releaseId == null) {
            buildClassLoader();
        }
        return Optional.of(releaseId);
    }

    private Optional<Path> getArtifact() {
        if (artifact == null) {
            buildClassLoader();
        }
        return Optional.of(artifact);
    }
}
