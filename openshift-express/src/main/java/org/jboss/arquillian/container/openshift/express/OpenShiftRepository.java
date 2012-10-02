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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.jboss.arquillian.container.openshift.express.auth.ArquillianSshSessionFactory;
import org.jboss.arquillian.container.openshift.express.util.GitUtil;
import org.jboss.arquillian.container.openshift.express.util.IOUtils;
import org.jboss.arquillian.container.openshift.express.util.MarkingUtil;
import org.jboss.arquillian.core.spi.Validate;

/**
 * Abstraction of a Git repository for OpenShift.
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class OpenShiftRepository {
    static Logger log = Logger.getLogger(OpenShiftRepository.class.getName());

    private static final String SOURCE_BUILD_IDENTIFIER_FILE = "pom.xml";

    private OpenShiftExpressConfiguration configuration;
    private CredentialsProvider credentialsProvider;
    private PersonIdent identification;

    private GitUtil git;
    private MarkingUtil markingUtil;

    private Set<String> deployments;

    private String lastSavedState;

    /**
     * Connects to remote repository and clones it to a temporary location on local file system. Determines deployments
     * directory based on cartridge type.
     *
     * @param configuration the configuration
     */
    public OpenShiftRepository(OpenShiftExpressConfiguration configuration, CredentialsProvider credentialsProvider) {

        this.configuration = configuration;
        this.credentialsProvider = credentialsProvider;
        this.deployments = new LinkedHashSet<String>();

        // override default SSH factory
        SshSessionFactory.setInstance(new ArquillianSshSessionFactory(configuration));

        try {
            initialize();
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize temporary Git repository", e);
        } catch (InvalidRemoteException e) {
            throw new RuntimeException("Unable to initialize temporary Git repository", e);
        } catch (TransportException e) {
            throw new RuntimeException("Unable to initialize temporary Git repository", e);
        } catch (GitAPIException e) {
            throw new RuntimeException("Unable to initialize temporary Git repository", e);
        }

    }

    /**
     * @param git the git to set
     */
    public void setGitUtil(GitUtil git) {
        this.git = git;
    }

    /**
     * @return the git
     */
    public GitUtil getGitUtil() {
        return git;
    }

    public boolean hasSourceBuild() {
        if (CartridgeType.JBOSSAS7 == configuration.getCartridgeType()) {
            return git.fileExists(SOURCE_BUILD_IDENTIFIER_FILE);
        }
        return false;
    }

    public OpenShiftRepository markArquillianLifeCycle() {
        markingUtil.markArquillianLifecycle();
        git.commit(identification, "Starting Arquillian lifecycle on OpenShift container");

        // no push, push will happen during first deployment

        return this;
    }

    public OpenShiftRepository unmarkArquillianLifeCycle() {
        markingUtil.unmarkArquillianLifecycle();
        git.commit(identification, "Stopping Arquillian lifecycle on OpenShift container");
        git.push(credentialsProvider);
        return this;
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
        add(path, content);
        push();

        return this;
    }

    /**
     * Adds and, commits under given path in the deployments directory which is scanned by OpenShift Express instance
     *
     *
     * @param path Path representing file name under deployments directory
     * @param content the context to be stored
     * @return Modified repository
     */
    public OpenShiftRepository add(String path, InputStream content) {
        // store file
        try {
            storeAsFileInRepository(path, content);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy context to the Git repository", e);
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Copied " + path + " to the local repository");
        }

        // add file to repository
        deployments.add(path);
        git.add(asFilePattern(path));
        markingUtil.mark(asFilePattern(path) + ".dodeploy");

        git.commit(identification, "Preparing " + path + " for OpenShift Express Deployment");

        if (log.isLoggable(Level.FINE)) {
            log.fine("Commited " + path + " to the repository");
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
        remove(path);
        push();
        return this;
    }

    /**
     * Removes, and commits under given path in deployments directory
     *
     * @param path Path representing file name under deployments directory
     * @return Modified repository
     */
    public OpenShiftRepository remove(String path) {
        deployments.remove(path);
        git.remove(asFilePattern(path));
        markingUtil.unmark(asFilePattern(path) + ".dodeploy");
        markingUtil.unmark(asFilePattern(path) + ".deployed");

        git.commit(identification, "Removing " + path + " Arquillian OpenShift Express Deployment");
        if (log.isLoggable(Level.FINE)) {
            log.fine("Commited " + path + " removal to the local repository");
        }

        return this;
    }

    public void push() {
        git.push(credentialsProvider);
        if (log.isLoggable(Level.INFO)) {
            log.info("Pushed to the remote repository " + configuration.getRemoteRepositoryUri());
        }
    }

    public String saveState() {
        String branch = UUID.randomUUID().toString();
        git.createBranch(branch.toString());
        lastSavedState = branch;
        return branch;
    }

    public void loadState(String branch) {
        Validate.notNull(branch, "The branch used to load state from can't be null.");
        git.restoreFromBranch(credentialsProvider, branch);
    }

    public String getLastSavedState() {
        return lastSavedState;
    }

    private void storeAsFileInRepository(String path, InputStream input) throws IOException {
        // create holder for the content
        File content = new File(asRepositoryPath(path));
        content.createNewFile();

        OutputStream output = new FileOutputStream(content);
        IOUtils.copy(input, output);

        IOUtils.closeQuietly(input);
        IOUtils.closeQuietly(output);
    }

    private void initialize() throws IOException, InvalidRemoteException, TransportException, GitAPIException {
        File repository = File.createTempFile("arq-openshift", "express");
        repository.delete();
        repository.mkdirs();
        repository.deleteOnExit();

        if (log.isLoggable(Level.FINE)) {
            log.fine("Preparing to clone " + configuration.getRemoteRepositoryUri() + " to " + repository.getAbsolutePath());
        }

        CloneCommand cloneCmd = Git.cloneRepository();
        cloneCmd.setDirectory(repository).setURI(configuration.getRemoteRepositoryUri());
        cloneCmd.setCredentialsProvider(credentialsProvider);

        this.git = new GitUtil(cloneCmd.call());
        this.markingUtil = new MarkingUtil(git);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Cloned remote repository from " + configuration.getRemoteRepositoryUri() + " to "
                    + repository.getAbsolutePath());
        }

        this.identification = new PersonIdent("Arquillian OpenShift Express", "arquillian@jboss.org");
    }

    private String asRepositoryPath(String path) {
        StringBuilder sb = new StringBuilder(git.getRepositoryDirectory().getAbsolutePath());
        return sb.append("/").append(asFilePattern(path)).toString();
    }

    private String asFilePattern(String path) {
        StringBuilder sb = new StringBuilder();

        return sb.append(configuration.getCartridgeType().getDeploymentDir()).append(path).toString();
    }

}
