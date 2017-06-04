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

import org.apache.maven.artifact.Artifact;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.KieClassLoaderProvider;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOMavenCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NIOClassLoaderProviderImpl implements KieClassLoaderProvider {

    protected static final Logger logger = LoggerFactory.getLogger(NIOClassLoaderProviderImpl.class);

    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(String prjPath, String localRepo, ClassLoader parent) {
        List<String> poms = new ArrayList<>();
        NIOMavenUtils.searchPoms(Paths.get(prjPath), poms);
        List<URL> urls = getDependenciesURL(poms, localRepo);
        return buildResult(urls, parent);
    }


    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(String prjPath, String localRepo) {
        List<String> poms = new ArrayList<>();
        NIOMavenUtils.searchPoms(Paths.get(prjPath), poms);
        List<URL> urls = getDependenciesURL(poms, localRepo);
        return buildResult(urls);
    }


    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(List<String> poms, String localRepo) {
        List<URL> urls = loadCompilationOutputFiles(poms);
        return buildResult(urls);
    }

    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(List<String> poms, String localRepo, ClassLoader parent) {
        List<URL> urls = loadCompilationOutputFiles(poms);
        return buildResult(urls, parent);
    }


    @Override
    public Optional<ClassLoader> getClassloaderFromProjectTargets(List<String> targets, Boolean loadIntoClassloader) {
        List<URL> urls = loadIntoClassloader ? loadCompilationOutputFiles(targets) : loadCompilationFolders(targets);
        return buildResult(urls);
    }

    @Override
    public Optional<ClassLoader> getClassloaderFromProjectTargets(List<String> targets, Boolean loadIntoClassloader, ClassLoader parent) {
        List<URL> urls = loadIntoClassloader ? loadCompilationOutputFiles(targets) : loadCompilationFolders(targets);
        return buildResult(urls, parent);
    }

    private List<URL> getDependenciesURL(List<String> poms, String localRepo) {
        List<Artifact> artifacts = NIOMavenUtils.resolveDependenciesFromMultimodulePrj(poms);
        List<URL> urls = Collections.emptyList();
        try {
            urls = buildUrlsFromArtifacts(localRepo, artifacts);
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage());
        }
        return urls;
    }


    private List<URL> buildUrlsFromArtifacts(String localRepo, List<Artifact> artifacts) throws MalformedURLException {
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


    private Optional<ClassLoader> loadOnlyFolderNames(List<String> pomsPaths) {
        List<URL> urls = new ArrayList();
        try {
            for (String pomPath : pomsPaths) {
                Path path = Paths.get(pomPath);
                StringBuilder sb = new StringBuilder("file://")
                        .append(path.getParent().toAbsolutePath().toString())
                        .append("/target/classes/");
                urls.add(new URL(sb.toString()));
            }
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage());
        }
        return buildResult(urls);
    }


    private Optional<ClassLoader> loadFiles(List<String> pomsPaths) {
        List<URL> urls = new ArrayList();
        try {
            for (String pomPath : pomsPaths) {
                Path path = Paths.get(pomPath);
                StringBuilder sb = new StringBuilder("file://")
                        .append(path.getParent().toAbsolutePath().toString())
                        .append("/target/classes/");
                urls.add(new URL(sb.toString()));
            }
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage());
        }
        return buildResult(urls);
    }


    private List<URL> loadCompilationFolders(List<String> pomsPaths) {
        List<URL> urls = new ArrayList();
        try {
            for (String pomPath : pomsPaths) {
                Path path = Paths.get(pomPath);
                StringBuilder sb = new StringBuilder("file://")
                        .append(path.getParent().toAbsolutePath().toString())
                        .append("/target/classes/");
                urls.add(new URL(sb.toString()));
            }
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage());
        }
        return urls;
    }

    private List<URL> loadCompilationOutputFiles(List<String> pomsPaths) {
        List<URL> urls = new ArrayList();
        try {
            for (String pomPath : pomsPaths) {
                Path path = Paths.get(pomPath);
                StringBuilder sb = new StringBuilder("file://")
                        .append(path.getParent().toAbsolutePath().toString())
                        .append("/target/classes/");
                urls.add(new URL(sb.toString()));
            }
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage());
        }
        return urls;
    }


    private Optional<ClassLoader> buildResult(List<URL> urls) {
        if (urls.isEmpty()) {
            return Optional.empty();
        } else {
            URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
            return Optional.of(urlClassLoader);
        }
    }

    private Optional<ClassLoader> buildResult(List<URL> urls, ClassLoader parent) {
        if (urls.isEmpty()) {
            return Optional.empty();
        } else {
            URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
            return Optional.of(urlClassLoader);
        }
    }


    public Optional<ClassLoader> getClassloaderFromAllDependencies(String prjPath, String localRepo) {
        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(Paths.get(localRepo), Decorator.NONE);
        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(Paths.get(prjPath), compiler);
        String randomUUI = UUID.randomUUID().toString();
        Path tmpFolder;
        try {
            tmpFolder = Files.createTempDirectory("classpath");
        } catch (java.io.IOException ioe) {
            logger.error(ioe.getMessage());
            return Optional.empty();
        }
        StringBuilder cpFile =new StringBuilder(tmpFolder.toAbsolutePath().toString()).append("/").append(randomUUI).append(".cp");
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info, new String[]{MavenArgs.DEPS_BUILD_CLASSPATH, new StringBuilder("-Dmdep.outputFile=").append(cpFile.toString()).toString()}, new HashMap<>());
        CompilationResponse res = compiler.compileSync(req);
        if (res.isSuccessful()) {
            List<URL> urls = readFile(cpFile.toString(), tmpFolder);
            if (!urls.isEmpty()) {
                URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
                return Optional.of(urlClassLoader);
            }
        }
        return Optional.empty();

    }

    private List<URL> readFile(String filePath, Path tmpFolder) {

        BufferedReader br = null;
        FileReader fr = null;
        List<URL> urls = new ArrayList<>();
        try {

            fr = new FileReader(filePath);
            br = new BufferedReader(fr);
            String sCurrentLine;
            br = new BufferedReader(fr);

            while ((sCurrentLine = br.readLine()) != null) {
                StringTokenizer token = new StringTokenizer(sCurrentLine, ":");
                while (token.hasMoreTokens()) {
                    StringBuilder sb = new StringBuilder("file://").append(token.nextToken());
                    urls.add(new URL(sb.toString()));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();
                Files.delete(Paths.get(filePath));
                Files.delete(tmpFolder);
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }

        }
        return urls;
    }
}
