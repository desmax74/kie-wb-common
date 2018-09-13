/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.screens.datasource.management.util;

import java.io.File;
import java.net.URI;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.appformer.maven.integration.Aether;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class MavenArtifactResolverTest {

    private static RepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(MavenArtifactResolver.getGlobalRepoPath());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session,
                                                                           localRepo));

        return session;
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class,
                           BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class,
                           FileTransporterFactory.class);
        locator.addService(TransporterFactory.class,
                           HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    @After
    public void tearDown() throws Exception {
        deleteArtifactIFPresent();
    }

    private void deleteArtifactIFPresent() throws ArtifactResolutionException {
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(getArtifact());
        ArtifactResult result = Aether.getAether().getSystem().resolveArtifact(newSession(newRepositorySystem()),
                                                                               artifactRequest);
        if (!result.isMissing()) {
            File artifactFile = result.getArtifact().getFile();
            assertThat(artifactFile.delete()).isTrue();
        }
    }

    private Artifact getArtifact() {
        Artifact jarArtifact = new DefaultArtifact("org.uberfire",
                                                   "uberfire-m2repo-editor-backend",
                                                   "jar",
                                                   "100-SNAPSHOT");
        return jarArtifact;
    }

    @Test
    public void resolveArtifact() throws Exception {
        RepositorySystemSession session = newSession(newRepositorySystem());
        assertThat(checksIfArtifactIsPresent(session)).isFalse();

        File file = new File("target/test-classes/uberfire-m2repo-editor-backend-100-SNAPSHOT.jar");
        assertThat(file).exists();

        Artifact artifact = getArtifact();
        artifact = artifact.setFile(file);
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);

        ArtifactResult result;
        try {
            Aether.getAether().getSystem().resolveArtifact(session,
                                                           artifactRequest);
        } catch (ArtifactResolutionException ex) {
            assertThat(ex).isNotNull();
        }

        deployTestJar(artifact,
                      session);

        MavenArtifactResolver resolver = new MavenArtifactResolver();
        URI uri = resolver.resolve(artifact.getGroupId(),
                                   artifact.getArtifactId(),
                                   artifact.getVersion());
        assertThat(uri).isNotNull();
        assertThat(uri.getPath()).endsWith("repositories" + File.separator + "kie" + File.separator + "global" + File.separator + "org" + File.separator + "uberfire" + File.separator + "uberfire-m2repo-editor-backend" + File.separator + "100-SNAPSHOT" + File.separator + "uberfire-m2repo-editor-backend-100-SNAPSHOT.jar");
        result = Aether.getAether().getSystem().resolveArtifact(session,
                                                                artifactRequest);
        assertThat(result.isMissing()).isFalse();
        assertThat(result.isResolved()).isTrue();
    }

    private boolean checksIfArtifactIsPresent(RepositorySystemSession session) {
        try {
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(getArtifact());
            Aether.getAether().getSystem().resolveArtifact(session,
                                                           artifactRequest);
            return true;
        } catch (ArtifactResolutionException e) {
            return false;
        }
    }

    private boolean deployTestJar(Artifact jarArtifact,
                                  RepositorySystemSession session) throws InstallationException {
        final InstallRequest installRequest = new InstallRequest();
        installRequest.addArtifact(jarArtifact);
        InstallResult result = Aether.getAether().getSystem().install(session,
                                                                      installRequest);
        return result.getArtifacts().size() == 1;
    }
}
