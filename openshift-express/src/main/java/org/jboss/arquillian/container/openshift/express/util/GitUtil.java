/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.openshift.express.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;

/**
 * Provides a high level Git API
 *
 * @author <a href="mailto:kpiwko@redhat.com>Karel Piwko</a>
 *
 */
public class GitUtil {
    private static final Logger log = Logger.getLogger(GitUtil.class.getName());

    private Git git;

    /**
     * Creates a git utility based on Git repository abstraction
     *
     * @param git
     */
    public GitUtil(Git git) {
        this.git = git;
    }

    /**
     * Adds a file pattern
     *
     * @param filePattern the file pattern
     */
    public void add(String filePattern) {

        AddCommand add = git.add();
        DirCache cache;
        try {
            cache = add.addFilepattern(filePattern).call();
            updateCache(cache);
        } catch (NoFilepatternException e) {
            throw new IllegalStateException("Unable to add file to the Git cache", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to add file to the Git cache", e);
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Stored " + filePattern + " to the local repository at " + filePattern);
        }
    }

    /**
     * Removes a file pattern
     *
     * @param filePattern the file pattern
     */
    public void remove(String filePattern) {
        RmCommand remove = git.rm();

        DirCache cache;
        try {
            cache = remove.addFilepattern(filePattern).call();
            updateCache(cache);
        } catch (NoFilepatternException e) {
            throw new IllegalStateException("Unable to remove file from the Git cache", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to remove file from the Git cache", e);
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Removed " + filePattern + " from the local repository");
        }
    }

    /**
     * Commits changes to a local repository
     *
     * @param identification The person identification
     * @param message the commit message
     */
    public void commit(PersonIdent identification, String message) {
        CommitCommand commit = git.commit();
        commit.setAuthor(identification);
        commit.setCommitter(identification);
        commit.setMessage(message);
        try {
            commit.call();
        } catch (NoHeadException e) {
            throw new IllegalStateException("Unable to commit into Git repository", e);
        } catch (NoMessageException e) {
            throw new IllegalStateException("Unable to commit into Git repository", e);
        } catch (UnmergedPathException e) {
            throw new IllegalStateException("Unable to commit into Git repository", e);
        } catch (ConcurrentRefUpdateException e) {
            throw new IllegalStateException("Unable to commit into Git repository", e);
        } catch (JGitInternalException e) {
            throw new IllegalStateException("Unable to commit into Git repository", e);
        } catch (WrongRepositoryStateException e) {
            throw new IllegalStateException("Unable to commit into Git repository", e);
        }
    }

    /**
     * Pushes local repository upstream
     *
     * @param credentialsProvider the credentials provider to get SSH pass phrase
     */
    public void push(CredentialsProvider credentialsProvider) {
        PushCommand push = git.push();
        push.setCredentialsProvider(credentialsProvider);
        try {
            push.call();
        } catch (JGitInternalException e) {
            throw new IllegalStateException("Unable to push into remote Git repository", e);
        } catch (InvalidRemoteException e) {
            throw new IllegalStateException("Unable to push into remote Git repository", e);
        }
    }

    /**
     * Gets project directory
     *
     * @return the repository directory on local file system
     */
    public File getRepositoryDirectory() {
        return git.getRepository().getWorkTree();// .getRepository().getDirectory();
    }

    public boolean fileExists(String fileName) {
       return new File(getRepositoryDirectory(), fileName).exists();
    }
    
    // update cache after changes
    private void updateCache(DirCache cache) throws IOException {
        if (!cache.lock()) {
            throw new IllegalStateException("Unable to lock Git repository cache");
        }
        cache.write();
        if (!cache.commit()) {
            throw new IllegalStateException("Unable to commit Git repository cache");
        }

    }

}
