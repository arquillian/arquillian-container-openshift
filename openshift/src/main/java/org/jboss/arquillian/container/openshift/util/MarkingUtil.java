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
package org.jboss.arquillian.container.openshift.util;

import java.io.File;
import java.io.IOException;

/**
 * An utility to add marking file to the git repository
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class MarkingUtil {

    // Arquillian lifecycle marker
    private static final String SKIP_MAVEN_BUILD = ".openshift/markers/skip_maven_build";

    private GitUtil git;

    /**
     * Creates a marking utility based on Git utility
     *
     * @param git
     */
    public MarkingUtil(GitUtil git) {
        this.git = git;
    }

    /**
     * Marks Arquillian life cycle start in the local repository
     */
    public void markArquillianLifecycle() {
        markSkipMavenBuild();
    }

    /**
     * Marks Arquillian life cycle stop in the local repository
     */
    public void unmarkArquillianLifecycle() {
        unmarkSkipMavenBuild();
    }

    /**
     * Removes arbitrary marker in the local repository
     *
     * @param markerName the name of the marker
     */
    public void unmark(String markerName) {
        git.remove(markerName);
    }

    /**
     * Adds arbitrary marker in the local repository
     *
     * @param markerName the name of the marker
     */
    public void mark(String markerName) {

        StringBuilder sb = new StringBuilder(git.getRepositoryDirectory().getAbsolutePath());
        sb.append("/").append(markerName);
        File file = new File(sb.toString());
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create a marker file at " + markerName + " e.g. " + sb.toString(), e);
        }

        git.add(markerName);
    }

    private void markSkipMavenBuild() {
        mark(SKIP_MAVEN_BUILD);
    }

    private void unmarkSkipMavenBuild() {
        unmark(SKIP_MAVEN_BUILD);
    }

}