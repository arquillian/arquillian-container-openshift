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
 *
 * Copyright (C) 2009, Constantine Plotnikov <constantine.plotnikov@gmail.com>
 * Copyright (C) 2008-2009, Google Inc.
 * Copyright (C) 2009, Google, Inc.
 * Copyright (C) 2009, JetBrains s.r.o.
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jboss.arquillian.container.openshift.express.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;
import org.jboss.arquillian.container.openshift.express.OpenShiftExpressConfiguration;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ArquillianSshSessionFactory extends JschConfigSessionFactory {
    private static final Logger log = Logger.getLogger(ArquillianSshSessionFactory.class.getName());

    private JSch preconfiguredJSch;

    public ArquillianSshSessionFactory(OpenShiftExpressConfiguration configuration) {
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

    private void preconfigureJSch(OpenShiftExpressConfiguration configuration) {

        String identityFile = configuration.getIdentityFile();
        String passphrase = configuration.getPassphrase();
        boolean disableStrictHostChecking = configuration.isDisableStrictHostChecking();

        if (identityFile == null || identityFile.length() == 0) {
            return;
        }
        Validate.isReadable(identityFile,
                "Arquillian Openshift Express configuration \"privateKeyFile\" must represent a path to a readable file, but it was "
                        + identityFile);

        // either disable StrictHostChecking or load known machines from a standard location
        JSch jsch = new JSch();
        if (disableStrictHostChecking) {
            log.warning("StrictHostKeyChecking was disabled. Your tests vulnerable to man-in-the-middle attacks.");
            JSch.setConfig("StrictHostKeyChecking", "no");
        } else {
            knownHosts(jsch);
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

    private static void knownHosts(final JSch sch) {

        final String userHomeDir = SecurityActions.getProperty("user.home");

        if (userHomeDir == null) {
            return;
        }

        final File home = new File(userHomeDir);

        final File known_hosts = new File(new File(home, ".ssh"), "known_hosts");
        try {
            final FileInputStream in = new FileInputStream(known_hosts);
            try {
                sch.setKnownHosts(in);
            } finally {
                in.close();
            }
        } catch (JSchException e) {
            log.warning("Unable to configure known hosts from SSH configuration, ignoring Arquillian \"privateKeyFile\" property."
                    + " If you want to force SSH to join to the host, set \"disableStrictHostChecking\" to true."
                    + " Cause: \n" + e.getMessage());
        } catch (FileNotFoundException none) {
            // Oh well. They don't have a known hosts in home.
        } catch (IOException err) {
            // Oh well. They don't have a known hosts in home.
        }
    }
}
