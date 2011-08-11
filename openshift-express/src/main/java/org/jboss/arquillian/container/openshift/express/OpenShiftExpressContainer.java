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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * OpenShift Express container. Deploys application or descriptor to an existing OpenShift instance. This instance must be
 * created before the test itself.
 *
 * <p>
 * See {@link OpenShiftExpressConfiguration} for required configuration
 * </p>
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class OpenShiftExpressContainer implements DeployableContainer<OpenShiftExpressConfiguration> {
    private static final Logger log = Logger.getLogger(OpenShiftExpressContainer.class.getName());

    @Inject
    @ContainerScoped
    private InstanceProducer<OpenShiftRepository> repository;

    @Inject
    @ContainerScoped
    private InstanceProducer<OpenShiftExpressConfiguration> configuration;

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    private CredentialsProvider credentialsProvider;

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public Class<OpenShiftExpressConfiguration> getConfigurationClass() {
        return OpenShiftExpressConfiguration.class;
    }

    @Override
    public void setup(OpenShiftExpressConfiguration configuration) {
        this.configuration.set(configuration);
        this.credentialsProvider = getCredentialsProvider();
    }

    @Override
    public void start() throws LifecycleException {

        // initialize repository
        long beforeInit = System.currentTimeMillis();
        OpenShiftExpressConfiguration conf = configuration.get();

        log.info("Preparing Arquillian OpenShift Express container at " + conf.getRootContextUrl());

        this.repository.set(new OpenShiftRepository(conf, credentialsProvider));

        if (log.isLoggable(Level.FINE)) {
            log.fine("Git repository initialization took " + (System.currentTimeMillis() - beforeInit) + "ms");
        }

        OpenShiftRepository repo = repository.get();
        repo.markArquillianLifeCycle();
    }

    @Override
    public void stop() throws LifecycleException {
        OpenShiftExpressConfiguration conf = configuration.get();
        OpenShiftRepository repo = repository.get();

        log.info("Shutting down Arquillian OpenShift Express container at " + conf.getRootContextUrl());

        repo.unmarkArquillianLifeCycle();
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {

        long beforeDeploy = System.currentTimeMillis();

        OpenShiftRepository repo = repository.get();
        InputStream is = new ByteArrayInputStream(descriptor.getDescriptorName().getBytes(Charset.defaultCharset()));
        repo.addAndPush(descriptor.getDescriptorName(), is);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Deployment of " + descriptor.getDescriptorName() + " took " + (System.currentTimeMillis() - beforeDeploy)
                    + "ms");
        }
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {

        long beforeUnDeploy = System.currentTimeMillis();

        OpenShiftRepository repo = repository.get();
        repo.removeAndPush(descriptor.getDescriptorName());

        if (log.isLoggable(Level.FINE)) {
            log.fine("Undeployment of " + descriptor.getDescriptorName() + " took "
                    + (System.currentTimeMillis() - beforeUnDeploy) + "ms");
        }
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {

        long beforeDeploy = System.currentTimeMillis();

        OpenShiftRepository repo = repository.get();
        InputStream is = archive.as(ZipExporter.class).exportAsInputStream();
        repo.addAndPush(archive.getName(), is);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Deployment of " + archive.getName() + " took " + (System.currentTimeMillis() - beforeDeploy) + "ms");
        }

        OpenShiftExpressConfiguration conf = configuration.get();
        ProtocolMetaDataParser parser = new ProtocolMetaDataParser(conf);
        return parser.parse(archive);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {

        long beforeUnDeploy = System.currentTimeMillis();

        OpenShiftRepository repo = repository.get();
        repo.removeAndPush(archive.getName());

        if (log.isLoggable(Level.FINE)) {
            log.fine("Undeployment of " + archive.getName() + " took " + (System.currentTimeMillis() - beforeUnDeploy) + "ms");
        }
    }

    /**
     * Returns a credentials provider for OpenShift Express. If no implementation is found, it returns a configuration based
     * one.
     *
     * @return the credentials provider
     */
    private CredentialsProvider getCredentialsProvider() {
        // get the credentials provider
        ServiceLoader service = serviceLoader.get();
        return service.onlyOne(CredentialsProvider.class);
    }
}
