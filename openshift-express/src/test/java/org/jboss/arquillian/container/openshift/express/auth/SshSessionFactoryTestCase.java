/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.arquillian.container.openshift.express.auth;

import org.jboss.arquillian.container.openshift.express.OpenShiftExpressConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests SSH session factory
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SshSessionFactoryTestCase {

    @Mock
    OpenShiftExpressConfiguration configuration;

    @Test
    public void createWithNoKeyFile() {
        Mockito.when(configuration.getIdentityFile()).thenReturn(null);
        new ArquillianSshSessionFactory(configuration);
    }

    @Test
    public void createWithEmptyFile() {
        Mockito.when(configuration.getIdentityFile()).thenReturn("");
        new ArquillianSshSessionFactory(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWithInvalidFile() {
        Mockito.when(configuration.getIdentityFile()).thenReturn("foo-bar-invalid");
        new ArquillianSshSessionFactory(configuration);
    }

}
