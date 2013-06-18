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
package org.jboss.arquillian.container.openshift.auth;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.jboss.arquillian.container.openshift.OpenShiftContainerConfiguration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

/**
 * Implements credentials provider for JGit based on arquillian.xml configuration.
 *
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class ArquillianConfigurationCredentialsProvider extends CredentialsProvider {

    private static final Logger log = Logger.getLogger(ArquillianConfigurationCredentialsProvider.class.getName());

    @Inject
    private Instance<OpenShiftContainerConfiguration> configuration;

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        for (CredentialItem i : items) {
            if (i instanceof CredentialItem.StringType) {
                continue;
            } else if (i instanceof CredentialItem.CharArrayType) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {

        if (configuration.get() == null) {
            throw new IllegalStateException("OpenShift Container configuration was not properly propagated");
        }

        if (items.length == 0) {
            return true;
        }

        for (CredentialItem item : items) {

            if (item instanceof CredentialItem.StringType) {
                CredentialItem.StringType i = (CredentialItem.StringType) item;
                i.setValue(getConfigurationValueByPrompt(i.getPromptText(), i.isValueSecure()));

            } else if (item instanceof CredentialItem.CharArrayType) {

                CredentialItem.CharArrayType i = (CredentialItem.CharArrayType) item;
                i.setValueNoCopy(getConfigurationValueByPrompt(i.getPromptText(), i.isValueSecure()).toCharArray());
            } else {
                throw new UnsupportedCredentialItem(uri, item.getPromptText());
            }
        }

        return true;
    }

    private String getConfigurationValueByPrompt(String prompt, boolean isSecured) {

        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException("Unable to determine configuration property, the JGit prompt must not be empty");
        }

        if (log.isLoggable(Level.FINER)) {
            log.finer("JGit requests: " + prompt);
        }

        OpenShiftContainerConfiguration conf = configuration.get();
        String retVal = "";
        if (prompt.startsWith("Passphrase")) {
            retVal = conf.getPassphrase();
        }

        if (log.isLoggable(Level.FINER)) {
            log.finer("ArquillianConfiguration returns: " + (isSecured ? "*masked" : retVal));
        }

        return retVal;
    }
}