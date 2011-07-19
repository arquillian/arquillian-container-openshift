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

/**
 * Abstraction of a Git repository
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class OpenShiftRepository {
    private static Logger log = Logger.getLogger(OpenShiftRepository.class.getName());

    private OpenShiftExpressConfiguration configuration;
    private File repository;
    private Git git;
    private PersonIdent identification;

    public OpenShiftRepository(OpenShiftExpressConfiguration configuration) {

        this.configuration = configuration;

        try {
            initialize();
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize temporary Git repository", e);
        }

    }

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

    private OpenShiftRepository push() {
        PushCommand push = git.push();
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

        // type specific path
        if ("jbossas-7.0".equals(configuration.getType())) {
            sb.append("deployments/");
        } else {
            sb.append("/");
        }

        return sb.append(path).toString();
    }
}
