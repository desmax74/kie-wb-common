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
import org.kie.workbench.common.services.backend.builder.compiler.KieClassLoaderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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



    /*private List<URL> getURLDependenciesFromProject(String prjPath, String localRepo){
        List<String> poms = new ArrayList<>();
        NIOMavenUtils.searchPoms(Paths.get(prjPath), poms);
        return getDependenciesURL(poms,localRepo);
    }*/

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
}
