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

package org.kie.workbench.common.services.backend.builder.compiler.uberfire.impl;

import org.apache.maven.artifact.Artifact;
import org.guvnor.common.services.backend.file.DotFileFilter;
import org.kie.workbench.common.services.backend.builder.compiler.KieClassLoaderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.DirectoryStream;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UberfireClassLoaderProviderImpl implements KieClassLoaderProvider {

    protected static final Logger logger = LoggerFactory.getLogger(UberfireClassLoaderProviderImpl.class);
    private final DirectoryStream.Filter<Path> dotFileFilter = new DotFileFilter();

    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(String prjPath, String localRepo) {
        List<String> poms = new ArrayList<>();
        UberfireMavenUtils.searchPoms(Paths.get(prjPath), poms);
        List<URL> urls = getDependenciesURL(poms, localRepo);
        return buildResult(urls);
    }

    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(String prjPath, String localRepo, ClassLoader parent) {
        List<String> poms = new ArrayList<>();
        UberfireMavenUtils.searchPoms(Paths.get(prjPath), poms);
        List<URL> urls = getDependenciesURL(poms, localRepo);
        return buildResult(urls, parent);
    }


    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(List<String> poms, String localRepo) {
        List<URL> urls = getDependenciesURL(poms, localRepo);
        return buildResult(urls);
    }

    @Override
    public Optional<ClassLoader> loadDependenciesClassloaderFromProject(List<String> poms, String localRepo, ClassLoader parent) {
        List<URL> urls = getDependenciesURL(poms, localRepo);
        return buildResult(urls, parent);
    }

    @Override
    public Optional<ClassLoader> getClassloaderFromProjectTargets(List<String> pomsPaths, Boolean loadIntoClassloader) {
        List<URL> urls = loadIntoClassloader ? loadFiles(pomsPaths) : loadOnlyFolderNames(pomsPaths);
        return buildResult(urls);
    }

    @Override
    public Optional<ClassLoader> getClassloaderFromProjectTargets(List<String> pomsPaths, Boolean loadIntoClassloader, ClassLoader parent) {
        List<URL> urls = loadIntoClassloader ? loadFiles(pomsPaths) : loadOnlyFolderNames(pomsPaths);
        return buildResult(urls, parent);
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


    private List<URL> getDependenciesURL(List<String> poms, String localRepo) {
        List<Artifact> artifacts = UberfireMavenUtils.resolveDependenciesFromMultimodulePrj(poms);
        List<URL> urls = Collections.emptyList();
        try {
            urls =
                    buildUrlsFromArtifacts(localRepo, artifacts);
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

    private List<URL> loadFiles(List<String> pomsPaths, ClassLoader parent) {
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

    private Optional<ClassLoader> buildResult(List<URL> urls, ClassLoader parent) {
        if (urls.isEmpty()) {
            return Optional.empty();
        } else {
            URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
            return Optional.of(urlClassLoader);
        }
    }
}
