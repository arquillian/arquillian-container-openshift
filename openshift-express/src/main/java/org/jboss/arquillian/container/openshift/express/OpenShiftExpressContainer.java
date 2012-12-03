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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.jboss.arquillian.container.openshift.express.ping.AS7PingArchive;
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
public class OpenShiftExpressContainer implements DeployableContainer<OpenShiftExpressConfiguration>
{
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
   public ProtocolDescription getDefaultProtocol()
   {
      return new ProtocolDescription("Servlet 3.0");
   }

   @Override
   public Class<OpenShiftExpressConfiguration> getConfigurationClass()
   {
      return OpenShiftExpressConfiguration.class;
   }

   @Override
   public void setup(OpenShiftExpressConfiguration configuration)
   {
      this.configuration.set(configuration);
      this.credentialsProvider = getCredentialsProvider();
   }

   @Override
   public void start() throws LifecycleException
   {

      // initialize repository
      long beforeInit = System.currentTimeMillis();
      OpenShiftExpressConfiguration conf = configuration.get();

      log.info("Preparing Arquillian OpenShift Express container at " + conf.getRootContextUrl());

      this.repository.set(new OpenShiftRepository(conf, credentialsProvider));

      if (log.isLoggable(Level.FINE))
      {
         log.fine("Git repository initialization took " + (System.currentTimeMillis() - beforeInit) + "ms");
      }

      OpenShiftRepository repo = repository.get();
      if (conf.isDiscardHistory()) {
         String state = repo.saveState();
         log.info("State of the repository has been saved to the branch <" + state + ">.");
      }
      if(repo.hasSourceBuild()) {
         repo.markArquillianLifeCycle();
      }
   }

   @Override
   public void stop() throws LifecycleException
   {
      OpenShiftExpressConfiguration conf = configuration.get();
      OpenShiftRepository repo = repository.get();

      log.info("Shutting down Arquillian OpenShift Express container at " + conf.getRootContextUrl());

      if(repo.hasSourceBuild()) {
         repo.unmarkArquillianLifeCycle();
      }
      if (conf.isDiscardHistory()) {
         String state = repo.getLastSavedState();
         repo.loadState(state);
         log.info("State of the repository has been loaded from the branch <" + state + ">.");
      }
   }

   @Override
   public void deploy(Descriptor descriptor) throws DeploymentException
   {

      long beforeDeploy = System.currentTimeMillis();

      OpenShiftRepository repo = repository.get();
      InputStream is = new ByteArrayInputStream(descriptor.getDescriptorName().getBytes(Charset.defaultCharset()));
      repo.addAndPush(descriptor.getDescriptorName(), is);

      if (log.isLoggable(Level.FINE))
      {
         log.fine("Deployment of " + descriptor.getDescriptorName() + " took "
               + (System.currentTimeMillis() - beforeDeploy) + "ms");
      }
   }

   @Override
   public void undeploy(Descriptor descriptor) throws DeploymentException
   {

      long beforeUnDeploy = System.currentTimeMillis();

      OpenShiftRepository repo = repository.get();
      repo.removeAndPush(descriptor.getDescriptorName());

      if (log.isLoggable(Level.FINE))
      {
         log.fine("Undeployment of " + descriptor.getDescriptorName() + " took "
               + (System.currentTimeMillis() - beforeUnDeploy) + "ms");
      }
   }

   @Override
   public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException
   {
      OpenShiftExpressConfiguration conf = configuration.get();

      long beforeDeploy = System.currentTimeMillis();

      OpenShiftRepository repo = repository.get();

      // we need to add a special deployment we can ping to check if we're done deploying
      if (CartridgeType.JBOSSAS7 == conf.getCartridgeType())
      {
         repo.add(AS7PingArchive.ARCHIVE_NAME, AS7PingArchive.create().as(ZipExporter.class).exportAsInputStream());
      }

      InputStream is = archive.as(ZipExporter.class).exportAsInputStream();
      repo.addAndPush(archive.getName(), is);

      if (log.isLoggable(Level.FINE))
      {
         log.fine("Deployment of " + archive.getName() + " took " + (System.currentTimeMillis() - beforeDeploy) + "ms");
      }

      if (CartridgeType.JBOSSAS7 == conf.getCartridgeType())
      {
         waitUntilDeployed(AS7PingArchive.ARCHIVE_NAME, archive.getName());
      }
      ProtocolMetaDataParser parser = new ProtocolMetaDataParser(conf);
      return parser.parse(archive);
   }

   @Override
   public void undeploy(Archive<?> archive) throws DeploymentException
   {
      OpenShiftExpressConfiguration conf = configuration.get();

      long beforeUnDeploy = System.currentTimeMillis();

      OpenShiftRepository repo = repository.get();
      if (CartridgeType.JBOSSAS7 == conf.getCartridgeType())
      {
         repo.remove(AS7PingArchive.ARCHIVE_NAME);
      }

      repo.removeAndPush(archive.getName());

      if (log.isLoggable(Level.FINE))
      {
         log.fine("Undeployment of " + archive.getName() + " took " + (System.currentTimeMillis() - beforeUnDeploy)
               + "ms");
      }
   }

   /**
    * Returns a credentials provider for OpenShift Express. If no implementation is found, it returns a configuration based
    * one.
    *
    * @return the credentials provider
    */
   private CredentialsProvider getCredentialsProvider()
   {
      // get the credentials provider
      ServiceLoader service = serviceLoader.get();
      return service.onlyOne(CredentialsProvider.class);
   }

   private void waitUntilDeployed(String pingArchiveName, String deploymentName) throws DeploymentException
   {
      StringBuilder url = new StringBuilder().append("http://").append(configuration.get().getHostName())
            .append(":80/").append(createDeploymentName(pingArchiveName)).append("/deploy?name=")
            .append(deploymentName);

      log.fine("Checking if deployment is deployed: " + url);

      long timeout = System.currentTimeMillis() + configuration.get().getDeploymentTimeoutInSeconds() * 1000;

      UrlChecker checker = new UrlChecker(timeout, url.toString());
      if (!checker.checkUrlWithRetry())
      {
         throw new DeploymentException("Following path were not reachable within " + configuration.get().getDeploymentTimeoutInSeconds() + " seconds after git push. "
               + "Check if following archives are constructed properly:\n" + deploymentName);
      }
   }

   private String createDeploymentName(String archiveName)
   {
      String correctedName = archiveName;
      if (correctedName.startsWith("/"))
      {
         correctedName = correctedName.substring(1);
      }
      if (correctedName.indexOf(".") != -1)
      {
         correctedName = correctedName.substring(0, correctedName.lastIndexOf("."));
      }
      return correctedName;
   }

   //in an interval, it checks whether an url returns HTTP 20x response
   class UrlChecker
   {
      final long timeout;

      final String url;

      UrlChecker(long timeout, String url)
      {
         this.timeout = timeout;
         this.url = url;
      }

      public boolean checkUrlWithRetry()
      {
         boolean interrupted = false;
         while (timeout > System.currentTimeMillis())
         {
            if (isUrlPresent(url) == true)
            {
               return true;
            }
            try
            {
               Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
               interrupted = true;
            }
         }
         if (interrupted)
         {
            Thread.currentThread().interrupt();
         }

         return false;
      }

      private boolean isUrlPresent(String url)
      {

         HttpURLConnection httpConnection = null;

         try
         {
            URLConnection connection = new URL(url).openConnection();

            if (!(connection instanceof HttpURLConnection))
            {
               throw new IllegalStateException("Not an http connection! " + connection);
            }

            httpConnection = (HttpURLConnection) connection;
            httpConnection.setUseCaches(false);
            httpConnection.setDefaultUseCaches(false);
            httpConnection.setDoInput(true);
            httpConnection.setRequestMethod("GET");
            httpConnection.setDoOutput(false);

            httpConnection.connect();
            httpConnection.getResponseCode();

            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
               return true;
            }

            return false;
         }
         catch (IOException e)
         {
            e.printStackTrace();
            return false;
         }
         finally
         {
            if (httpConnection != null)
            {
               httpConnection.disconnect();
            }
         }
      }

   }
}