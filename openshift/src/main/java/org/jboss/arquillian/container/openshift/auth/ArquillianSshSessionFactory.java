/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

import java.io.File;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;
import org.jboss.arquillian.container.openshift.OpenShiftContainerConfiguration;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ArquillianSshSessionFactory extends JschConfigSessionFactory {
    private static final Logger log = Logger.getLogger(ArquillianSshSessionFactory.class.getName());

    private JSch preconfiguredJSch;

    public ArquillianSshSessionFactory(OpenShiftContainerConfiguration configuration) {
        preconfigureJSch(configuration);
    }

    @Override
    protected JSch getJSch(Host hc, FS fs) throws JSchException {
        return preconfiguredJSch != null ? preconfiguredJSch : super.getJSch(hc, fs);
    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {
        return preconfiguredJSch != null ? preconfiguredJSch : super.createDefaultJSch(fs);
    }

    @Override
    protected void configure(Host hc, Session session) {
        // No additional configuration required
    }

    private void preconfigureJSch(OpenShiftContainerConfiguration configuration) {

        String identityFile = configuration.getIdentityFile();
        String passphrase = configuration.getPassphrase();
        boolean disableStrictHostChecking = configuration.isDisableStrictHostChecking();

        if (identityFile == null || identityFile.length() == 0) {
            return;
        }
        Validate.isReadable(identityFile,
                "Arquillian Openshift Container configuration \"privateKeyFile\" must represent a path to a readable file, but it was "
                        + identityFile);

        // either disable StrictHostChecking or load known machines from a standard location
        JSch jsch = new JSch();
        if (disableStrictHostChecking) {
            log.warning("StrictHostKeyChecking was disabled. Your tests vulnerable to man-in-the-middle attacks.");
            JSch.setConfig("StrictHostKeyChecking", "no");
        } else {
            setKnownHosts(jsch);
        }

        String prvkey = new File(identityFile).getAbsolutePath();

        try {
            if (passphrase == null || passphrase.length() == 0) {
                jsch.addIdentity(prvkey);
            } else {
                jsch.addIdentity(prvkey, passphrase);
            }
        } catch (JSchException e) {
            log.warning("Unable to add private key from " + prvkey
                    + ", to SSH configuration, ignoring Arquillian \"privateKeyFile\" property. Cause: \n" + e.getMessage());
        }

        // we have configured a special JSch
        this.preconfiguredJSch = jsch;

    }

    private void setKnownHosts(final JSch sch) {

        String userHomeDir = SecurityActions.getProperty("user.home");
        File userHome = new File(userHomeDir + File.separator + ".ssh" + File.separator + "known_hosts");
        if (userHome.exists() == false || userHome.canRead() == false) {
            return;
        }

        try {
            sch.setKnownHosts(userHome.getAbsolutePath());
        } catch (JSchException e) {
            log.warning("Unable to configure known hosts from SSH configuration, ignoring Arquillian \"privateKeyFile\" property."
                    + " If you want to force SSH to join to the host, set \"disableStrictHostChecking\" to true."
                    + " Cause: \n" + e.getMessage());
        }
    }

}
