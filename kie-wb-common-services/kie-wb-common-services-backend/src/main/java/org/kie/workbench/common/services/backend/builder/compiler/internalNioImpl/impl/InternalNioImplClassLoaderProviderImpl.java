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

package org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.guvnor.common.services.backend.file.DotFileFilter;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.KieClassLoaderProvider;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.InternalNioImplCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.InternalNioImplMavenCompiler;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOMavenCompiler;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIODefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOMavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOWorkspaceCompilationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.DirectoryStream;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class InternalNioImplClassLoaderProviderImpl implements KieClassLoaderProvider {

    protected static final Logger logger = LoggerFactory.getLogger(InternalNioImplClassLoaderProviderImpl.class);
    private static String CLASSPATH_FILENAME = "module";
    private static String CLASSPATH_EXT = ".cpath";
    private static String MAVEN_DEP_PLUGING_OUTPUT_FILE = "-Dmdep.outputFile=";
    private final DirectoryStream.Filter<Path> dotFileFilter = new DotFileFilter();

    public static void searchCPFiles(Path file,
                                     List<String> classPathFiles) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(file.toAbsolutePath())) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    searchCPFiles(p,
                                  classPathFiles);
                } else if (p.toString().endsWith(CLASSPATH_EXT)) {
                    classPathFiles.add(p.toAbsolutePath().toString());
                }
            }
        }
    }

    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(String prjPath,
                                                                        String localRepo) {
        List<String> poms = new ArrayList<>();
        InternalNioImplMavenUtils.searchPoms(Paths.get(prjPath),
                                             poms);
        List<URL> urls = getDependenciesURL(poms,
                                            localRepo);
        return buildResult(urls);
    }

    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(String prjPath,
                                                                        String localRepo,
                                                                        ClassLoader parent) {
        List<String> poms = new ArrayList<>();
        InternalNioImplMavenUtils.searchPoms(Paths.get(prjPath),
                                             poms);
        List<URL> urls = getDependenciesURL(poms,
                                            localRepo);
        return buildResult(urls,
                           parent);
    }

    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(List<String> poms,
                                                                        String localRepo) {
        List<URL> urls = getDependenciesURL(poms,
                                            localRepo);
        return buildResult(urls);
    }

    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(List<String> poms,
                                                                        String localRepo,
                                                                        ClassLoader parent) {
        List<URL> urls = getDependenciesURL(poms,
                                            localRepo);
        return buildResult(urls,
                           parent);
    }

    @Override
    public Optional<ClassLoader> getClassloaderFromProjectTargets(List<String> pomsPaths,
                                                                  Boolean loadIntoClassloader) {
        List<URL> urls = loadIntoClassloader ? loadFiles(pomsPaths) : loadOnlyFolderNames(pomsPaths);
        return buildResult(urls);
    }

    @Override
    public Optional<ClassLoader> getClassloaderFromProjectTargets(List<String> pomsPaths,
                                                                  Boolean loadIntoClassloader,
                                                                  ClassLoader parent) {
        List<URL> urls = loadIntoClassloader ? loadFiles(pomsPaths) : loadOnlyFolderNames(pomsPaths);
        return buildResult(urls,
                           parent);
    }

    private List<URL> buildUrlsFromArtifacts(String localRepo,
                                             List<Artifact> artifacts) throws MalformedURLException {
        List<URL> urls = new ArrayList<>(artifacts.size());
        for (Artifact artifact : artifacts) {
            StringBuilder sb = new StringBuilder("file://");
            sb.append(localRepo).append("/").append(artifact.getGroupId()).
                    append("/").append(artifact.getVersion()).append("/").append(artifact.getArtifactId()).
                    append("-").append(artifact.getVersion()).append(".").append(artifact.getType());
            URL url = new URL(sb.toString());
            urls.add(url);
        }
        return urls;
    }

    private List<URL> getDependenciesURL(List<String> poms,
                                         String localRepo) {
        List<Artifact> artifacts = InternalNioImplMavenUtils.resolveDependenciesFromMultimodulePrj(poms);
        List<URL> urls = Collections.emptyList();
        try {
            urls =
                    buildUrlsFromArtifacts(localRepo,
                                           artifacts);
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage());
        }
        return urls;
    }

    private List<URL> loadFiles(List<String> pomsPaths) {
        List<URL> targetModulesUrls = getTargetModulesURL(pomsPaths);
        if (!targetModulesUrls.isEmpty()) {
            List<URL> targetFiles = addFIlesURL(targetModulesUrls);
            return targetFiles;
        }
        return Collections.emptyList();
    }

    private List<URL> loadFiles(List<String> pomsPaths,
                                ClassLoader parent) {
        List<URL> targetModulesUrls = getTargetModulesURL(pomsPaths);
        if (!targetModulesUrls.isEmpty()) {
            List<URL> targetFiles = addFIlesURL(targetModulesUrls);
            if (!targetFiles.isEmpty()) {
                return targetFiles;
            }
        }
        return Collections.emptyList();
    }

    private List<URL> addFIlesURL(List<URL> targetModulesUrls) {
        List<URL> targetFiles = new ArrayList<>(targetModulesUrls.size());
        for (URL url : targetModulesUrls) {
            try {
                targetFiles.addAll(visitFolders(Files.newDirectoryStream(Paths.get(url.toURI()))));
            } catch (URISyntaxException ex) {
                logger.error(ex.getMessage());
            }
        }
        return targetFiles;
    }

    private List<URL> getTargetModulesURL(List<String> pomsPaths) {
        if (pomsPaths != null && pomsPaths.size() > 0) {
            List<URL> targetModulesUrls = new ArrayList(pomsPaths.size());
            try {
                for (String pomPath : pomsPaths) {
                    Path path = Paths.get(pomPath);
                    StringBuilder sb = new StringBuilder("file://")
                            .append(path.getParent().toAbsolutePath().toString())
                            .append("/target/classes/");
                    targetModulesUrls.add(new URL(sb.toString()));
                }
            } catch (MalformedURLException ex) {
                logger.error(ex.getMessage());
            }
            return targetModulesUrls;
        } else {
            return Collections.emptyList();
        }
    }

    private List<URL> visitFolders(final DirectoryStream<Path> directoryStream) {
        List<URL> urls = new ArrayList<>();
        for (final org.uberfire.java.nio.file.Path path : directoryStream) {
            if (Files.isDirectory(path)) {
                visitFolders(Files.newDirectoryStream(path));
            } else {
                //Don't process dotFiles
                if (!dotFileFilter.accept(path)) {
                    try {
                        urls.add(path.toUri().toURL());
                    } catch (MalformedURLException ex) {
                        logger.error(ex.getMessage());
                    }
                }
            }
        }
        return urls;
    }

    private List<URL> loadOnlyFolderNames(List<String> pomsPaths) {
        return getTargetModulesURL(pomsPaths);
    }

    private Optional<ClassLoader> buildResult(List<URL> urls) {
        if (urls.isEmpty()) {
            return Optional.empty();
        } else {
            URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
            return Optional.of(urlClassLoader);
        }
    }

    private Optional<ClassLoader> buildResult(List<URL> urls,
                                              ClassLoader parent) {
        if (urls.isEmpty()) {
            return Optional.empty();
        } else {
            URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
                                                               parent);
            return Optional.of(urlClassLoader);
        }
    }

    public Optional<ClassLoader> getClassloaderFromAllDependencies(String prjPath,
                                                                   String localRepo) {
        InternalNioImplMavenCompiler compiler = InternalNioImplMavenCompilerFactory.getCompiler(Paths.get(localRepo),
                                                                                                Decorator.NONE);
        InternalNioImplWorkspaceCompilationInfo info = new InternalNioImplWorkspaceCompilationInfo(Paths.get(prjPath),
                                                                                                   compiler);
        StringBuilder sb = new StringBuilder(MAVEN_DEP_PLUGING_OUTPUT_FILE).append(CLASSPATH_FILENAME).append(CLASSPATH_EXT);
        InternalNioImplCompilationRequest req = new InternalNioImplDefaultCompilationRequest(info,
                                                                                             new String[]{MavenArgs.DEPS_BUILD_CLASSPATH, sb.toString()},
                                                                                             new HashMap<>(),
                                                                                             Optional.empty());
        CompilationResponse res = compiler.compileSync(req);
        if (res.isSuccessful()) {
            /** Maven dependency plugin is not able to append the modules classpath using an absolute path in -Dmdep.outputFile,
             it override each time and at the end only the last writted is present in  the file,
             for this reason we use a relative path and then we read each file present in each module to build a unique classpath file
             * */
            List<String> classPathFiles = new ArrayList<>();
            searchCPFiles(Paths.get(prjPath),
                          classPathFiles);
            if (!classPathFiles.isEmpty()) {
                List<URL> deps = new ArrayList<>();
                for (String file : classPathFiles) {
                    deps.addAll(readFile(file));
                }
                if (!deps.isEmpty()) {
                    URLClassLoader urlClassLoader = new URLClassLoader(deps.toArray(new URL[deps.size()]));
                    return Optional.of(urlClassLoader);
                }
            }
        }
        return Optional.empty();
    }

    private List<URL> readFile(String filePath) {

        BufferedReader br = null;
        FileReader fr = null;
        List<URL> urls = new ArrayList<>();
        try {

            fr = new FileReader(filePath);
            br = new BufferedReader(fr);
            String sCurrentLine;
            br = new BufferedReader(fr);

            while ((sCurrentLine = br.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(sCurrentLine,
                                                            ":");
                while (token.hasMoreTokens()) {
                    StringBuilder sb = new StringBuilder("file://").append(token.nextToken());
                    urls.add(new URL(sb.toString()));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }

                if (fr != null) {
                    fr.close();
                }
                Files.delete(Paths.get(filePath));
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
        return urls;
    }
/*
    private List<URL> readFile(String filePath,
                               Path tmpFolder) {

        BufferedReader br = null;
        FileReader fr = null;
        List<URL> urls = new ArrayList<>();
        try {

            fr = new FileReader(filePath);
            br = new BufferedReader(fr);
            String sCurrentLine;
            br = new BufferedReader(fr);

            while ((sCurrentLine = br.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(sCurrentLine,
                                                            ":");
                while (token.hasMoreTokens()) {
                    StringBuilder sb = new StringBuilder("file://").append(token.nextToken());
                    urls.add(new URL(sb.toString()));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }

                if (fr != null) {
                    fr.close();
                }
                Files.delete(Paths.get(filePath));
                Files.delete(tmpFolder);
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
        return urls;
    }*/

    public Optional<List<URI>> getURISFromAllDependencies(String prjPath,
                                                          String localRepo) {
        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(java.nio.file.Paths.get(localRepo),
                                                                        Decorator.NONE);
        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(java.nio.file.Paths.get(prjPath),
                                                                           compiler);
        StringBuilder sb = new StringBuilder(MAVEN_DEP_PLUGING_OUTPUT_FILE).append(CLASSPATH_FILENAME).append(CLASSPATH_EXT);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info,
                                                                     new String[]{MavenArgs.DEPS_BUILD_CLASSPATH, sb.toString()},
                                                                     new HashMap<>(),
                                                                     Optional.empty());
        CompilationResponse res = compiler.compileSync(req);
        if (res.isSuccessful()) {
            /** Maven dependency plugin is not able to append the modules' classpath using an absolute path in -Dmdep.outputFile,
             it override each time and at the end only the last writted is present in  the file,
             for this reason we use a relative path and then we read each file present in each module to build a unique classpath file
             * */
            List<String> classPathFiles = new ArrayList<>();
            searchCPFiles(Paths.get(prjPath),
                          classPathFiles);
            if (!classPathFiles.isEmpty()) {
                List<URI> deps = new ArrayList<>();
                for (String file : classPathFiles) {
                    deps.addAll(readFileAsURI(file));
                }
                if (!deps.isEmpty()) {
                    return Optional.of(deps);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<List<URI>> getURISFromAllDependencies(String prjPath,
                                                          String localRepo,
                                                          NIOMavenCompiler compiler,
                                                          NIOWorkspaceCompilationInfo info) {

        StringBuilder sb = new StringBuilder(MAVEN_DEP_PLUGING_OUTPUT_FILE).append(CLASSPATH_FILENAME).append(CLASSPATH_EXT);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info,
                                                                     new String[]{MavenArgs.DEPS_BUILD_CLASSPATH, sb.toString()},
                                                                     new HashMap<>(),
                                                                     Optional.empty());
        CompilationResponse res = compiler.compileSync(req);
        if (res.isSuccessful()) {
            /** Maven dependency plugin is not able to append the modules' classpath using an absolute path in -Dmdep.outputFile,
             it override each time and at the end only the last writted is present in  the file,
             for this reason we use a relative path and then we read each file present in each module to build a unique classpath file
             * */
            List<String> classPathFiles = new ArrayList<>();
            searchCPFiles(Paths.get(prjPath),
                          classPathFiles);
            if (!classPathFiles.isEmpty()) {
                List<URI> deps = new ArrayList<>();
                for (String file : classPathFiles) {
                    deps.addAll(readFileAsURI(file));
                }
                if (!deps.isEmpty()) {
                    return Optional.of(deps);
                }
            }
        }
        return Optional.empty();
    }

    private List<URI> readFileAsURI(String filePath) {

        BufferedReader br = null;
        FileReader fr = null;
        List<URI> urls = new ArrayList<>();
        try {

            fr = new FileReader(filePath);
            br = new BufferedReader(fr);
            String sCurrentLine;
            br = new BufferedReader(fr);

            while ((sCurrentLine = br.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(sCurrentLine,
                                                            ":");
                while (token.hasMoreTokens()) {
                    StringBuilder sb = new StringBuilder("file://").append(token.nextToken());
                    urls.add(new URI(sb.toString()));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }

                if (fr != null) {
                    fr.close();
                }
                Files.delete(Paths.get(filePath));
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
        return urls;
    }

    @Override
    public Optional<List<URI>> getURISFromAllDependencies(String prjPath) {
        List<URI> deps = readAllCpFilesAsUris(prjPath);
        if (deps.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(deps);
        }
    }

    private List<URI> readAllCpFilesAsUris(String prjPath) {
        List<String> classPathFiles = new ArrayList<>();
        searchCPFiles(Paths.get(prjPath),
                      classPathFiles);
        if (!classPathFiles.isEmpty()) {
            List<URI> deps = new ArrayList<>();
            for (String file : classPathFiles) {
                deps.addAll(readFileAsURI(file));
            }
            if (!deps.isEmpty()) {

                return deps;
            }
        }
        return Collections.emptyList();
    }

    private List<URL> readAllCpFilesAsUrls(String prjPath) {
        List<String> classPathFiles = new ArrayList<>();
        searchCPFiles(Paths.get(prjPath),
                      classPathFiles);
        if (!classPathFiles.isEmpty()) {
            List<URL> deps = new ArrayList<>();
            for (String file : classPathFiles) {
                deps.addAll(readFileAsURL(file));
            }
            if (!deps.isEmpty()) {

                return deps;
            }
        }
        return Collections.emptyList();
    }

    private List<URL> readFileAsURL(String filePath) {

        BufferedReader br = null;
        FileReader fr = null;
        List<URL> urls = new ArrayList<>();
        try {

            fr = new FileReader(filePath);
            br = new BufferedReader(fr);
            String sCurrentLine;
            br = new BufferedReader(fr);

            while ((sCurrentLine = br.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(sCurrentLine,
                                                            ":");
                while (token.hasMoreTokens()) {
                    StringBuilder sb = new StringBuilder("file://").append(token.nextToken());
                    urls.add(new URL(sb.toString()));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }

                if (fr != null) {
                    fr.close();
                }
                Files.delete(Paths.get(filePath));
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
        return urls;
    }
}
