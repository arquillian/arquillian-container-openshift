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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * OpenShift Express container
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class OpenShiftExpressContainer implements DeployableContainer<OpenShiftExpressConfiguration> {
    private static Logger log = Logger.getLogger(OpenShiftExpressContainer.class.getName());

    private OpenShiftExpressConfiguration configuration;

    private final List<String> failedUndeployments = new ArrayList<String>();

    @Inject
    @ContainerScoped
    private InstanceProducer<OpenShiftRepository> repository;

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Local");
    }

    @Override
    public Class<OpenShiftExpressConfiguration> getConfigurationClass() {
        return OpenShiftExpressConfiguration.class;
    }

    @Override
    public void setup(OpenShiftExpressConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        // initialize repository
        this.repository.set(new OpenShiftRepository(configuration));
    }

    @Override
    public void stop() throws LifecycleException {
        // no-op
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        OpenShiftRepository repo = repository.get();

        InputStream is = new ByteArrayInputStream(descriptor.getDescriptorName().getBytes(Charset.defaultCharset()));
        repo.addAndPush(descriptor.getDescriptorName(), is);
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        OpenShiftRepository repo = repository.get();

        repo.removeAndPush(descriptor.getDescriptorName());
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        OpenShiftRepository repo = repository.get();

        InputStream is = archive.as(ZipExporter.class).exportAsInputStream();
        repo.addAndPush(archive.getName(), is);

        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        OpenShiftRepository repo = repository.get();

        repo.removeAndPush(archive.getName());
    }

}
