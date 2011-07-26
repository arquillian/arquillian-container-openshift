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
package org.jboss.arquillian.container.openshift.express;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
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
 * Abstraction of a Git repository for OpenShift.
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class OpenShiftRepository {
    private static Logger log = Logger.getLogger(OpenShiftRepository.class.getName());

    private OpenShiftExpressConfiguration configuration;
    private CredentialsProvider credentialsProvider;
    private File repository;
    private Git git;
    private PersonIdent identification;

    /**
     * Connects to remote repository and clones it to a temporary location on local file system. Determines deployments
     * directory based on cartridge type.
     *
     * @param configuration the configuration
     */
    public OpenShiftRepository(OpenShiftExpressConfiguration configuration, CredentialsProvider credentialsProvider) {

        this.configuration = configuration;
        this.credentialsProvider = credentialsProvider;

        try {
            initialize();
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize temporary Git repository", e);
        }

    }

    /**
     * Adds, commits and pushes upstream context under given path in the deployments directory which is scanned by OpenShift
     * Express instance
     *
     *
     * @param path Path representing file name under deployments directory
     * @param content the context to be stored
     * @return Modified repository
     */
    public OpenShiftRepository addAndPush(String path, InputStream content) {

        try {
            storeAsFileInRepository(path, content);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy context to the Git repository", e);
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Copied " + path + " to the local repository");
        }

        AddCommand add = git.add();
        DirCache cache;
        try {
            cache = add.addFilepattern(asFilePattern(path)).call();
            updateCache(cache);
        } catch (NoFilepatternException e) {
            throw new IllegalStateException("Unable to add file to the Git cache", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to add file to the Git cache", e);
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Stored " + path + " to the local repository at " + asFilePattern(path));
        }

        commit("Preparing " + path + " for OpenShift Express Deployment");

        if (log.isLoggable(Level.FINE)) {
            log.fine("Commited " + path + " to the repository");
        }

        push();

        if (log.isLoggable(Level.INFO)) {
            log.info("Pushed " + path + " to the remote repository " + configuration.getRemoteRepositoryUri());
        }

        return this;
    }

    /**
     * Removes, commits and pushes upstream under given path in deployments directory
     *
     * @param path Path representing file name under deployments directory
     * @return Modified repository
     */
    public OpenShiftRepository removeAndPush(String path) {
        RmCommand remove = git.rm();

        DirCache cache;
        try {
            cache = remove.addFilepattern(asFilePattern(path)).call();
            updateCache(cache);
        } catch (NoFilepatternException e) {
            throw new IllegalStateException("Unable to remove file from the Git cache", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to remove file from the Git cache", e);
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Removed " + path + " from local the repository");
        }

        commit("Removing " + path + " Arquillian OpenShift Express Deployment");
        if (log.isLoggable(Level.FINE)) {
            log.fine("Commited " + path + " removal to the local repository");
        }

        push();
        if (log.isLoggable(Level.INFO)) {
            log.info("Pushed removal of " + path + " to the remote repository " + configuration.getRemoteRepositoryUri());
        }

        return this;
    }

    // commit
    private OpenShiftRepository commit(String message) {
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

        return this;
    }

    // push
    private OpenShiftRepository push() {
        PushCommand push = git.push();
        push.setCredentialsProvider(credentialsProvider);
        try {
            push.call();
        } catch (JGitInternalException e) {
            throw new IllegalStateException("Unable to push into remote Git repository", e);
        } catch (InvalidRemoteException e) {
            throw new IllegalStateException("Unable to push into remote Git repository", e);
        }

        return this;
    }

    private void updateCache(DirCache cache) throws IOException {
        if (!cache.lock()) {
            throw new IllegalStateException("Unable to lock Git repository cache");
        }
        cache.write();
        if (!cache.commit()) {
            throw new IllegalStateException("Unable to commit Git repository cache");
        }

    }

    private void storeAsFileInRepository(String path, InputStream input) throws IOException {
        // create holder for the content
        File content = new File(asRepositoryPath(path));
        content.createNewFile();

        OutputStream output = new FileOutputStream(content);
        IOUtils.copy(input, output);

        try {
            input.close();
        } catch (IOException e) {
            log.warning("Could not close input for " + path + ", cause: " + e.getMessage());
        }
        try {
            output.close();
        } catch (IOException e) {
            log.warning("Could not close input for " + path + ", cause: " + e.getMessage());
        }
    }

    private void initialize() throws IOException {
        this.repository = File.createTempFile("arq-openshift", "express");
        repository.delete();
        repository.mkdirs();
        repository.deleteOnExit();

        if (log.isLoggable(Level.FINE)) {
            log.fine("Preparing to clone " + configuration.getRemoteRepositoryUri() + " to " + repository.getAbsolutePath());
        }

        CloneCommand cloneCmd = Git.cloneRepository();
        cloneCmd.setDirectory(repository).setURI(configuration.getRemoteRepositoryUri());
        cloneCmd.setCredentialsProvider(credentialsProvider);

        this.git = cloneCmd.call();

        if (log.isLoggable(Level.FINE)) {
            log.fine("Cloned remote repository from " + configuration.getRemoteRepositoryUri() + " to "
                    + repository.getAbsolutePath());
        }

        this.identification = new PersonIdent("Arquillian OpenShift Express", "arquillian@jboss.org");

    }

    private String asRepositoryPath(String path) {
        StringBuilder sb = new StringBuilder(repository.getAbsolutePath());
        return sb.append("/").append(asFilePattern(path)).toString();
    }

    private String asFilePattern(String path) {
        StringBuilder sb = new StringBuilder();

        return sb.append(configuration.getCartridgeType().getDeploymentDir()).append(path).toString();
    }
}
