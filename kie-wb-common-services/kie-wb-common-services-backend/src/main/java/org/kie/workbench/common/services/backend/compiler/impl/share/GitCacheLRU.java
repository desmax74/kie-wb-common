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
package org.kie.workbench.common.services.backend.compiler.impl.share;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.eclipse.jgit.api.Git;
import org.guvnor.common.services.backend.cache.LRUCache;
import org.uberfire.java.nio.fs.jgit.JGitFileSystem;

@ApplicationScoped
@Named("LRUGitCache")
public class GitCacheLRU extends LRUCache<JGitFileSystem, Git> implements GitCache {

    public synchronized Git getGit(JGitFileSystem key) {
        return getEntry(key);
    }

    public synchronized void addGit(JGitFileSystem key, Git git) {
        setEntry(key, git);
    }

    public synchronized void removeGit(JGitFileSystem key) {
        invalidateCache(key);
    }

    public synchronized boolean containsGit(JGitFileSystem key) {
        return getKeys().contains(key);
    }

    public synchronized void clearGitCache() {
        invalidateCache();
        ;
    }
}