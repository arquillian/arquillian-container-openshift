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

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.openshift.express.archive.ArchiveUtil;
import org.jboss.arquillian.container.openshift.express.archive.AssetUtil;
import org.jboss.arquillian.container.openshift.express.util.IOUtils;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.application5.ApplicationDescriptor;
import org.jboss.shrinkwrap.descriptor.api.application5.ModuleType;
import org.jboss.shrinkwrap.descriptor.api.application5.WebType;
import org.jboss.shrinkwrap.descriptor.api.jbossweb60.JbossWebDescriptor;

/**
 * Metadata parser using OpenShift configuration and classpath scanning as no metadata are provided from from container itself
 *
 * <p>
 * Currently does not support web.xml scanning for servlet names and application.xml scanning for war context paths.
 * </p>
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 *
 */
public class ProtocolMetaDataParser {
    private static final Logger log = Logger.getLogger(ProtocolMetaDataParser.class.getName());

    private OpenShiftExpressConfiguration configuration;

    /**
     * Create parser
     *
     * @param configuration the configuration
     */
    public ProtocolMetaDataParser(OpenShiftExpressConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("OpenShift Express Configuration must be specified");
        }
        this.configuration = configuration;
    }

    /**
     * Extract metadata information from the deployment
     *
     * @param deployment the archive
     * @return Metadata information
     */
    public ProtocolMetaData parse(Archive<?> deployment) {
        ProtocolMetaData protocol = new ProtocolMetaData();
        HTTPContext context = new HTTPContext(configuration.getHostName(), 80);
        protocol.addContext(context);

        if (ArchiveUtil.isWarArchive(deployment)) {
            extractWebArchiveContexts(context, (WebArchive) deployment);
        } else if (ArchiveUtil.isEarArchive(deployment)) {
            extractEnterpriseArchiveContexts(context, (EnterpriseArchive) deployment);
        }

        return protocol;
    }

    private void extractEnterpriseArchiveContexts(HTTPContext context, EnterpriseArchive deployment) {

        for (WebArchive war : deployment.getAsType(WebArchive.class, AssetUtil.WAR_FILTER)) {
            extractEnterpriseWebArchiveContexts(context, deployment, war);
        }
    }

    private void extractWebArchiveContexts(HTTPContext context, WebArchive deployment) {
        extractWebContext(context, deployment.getName(), deployment);
    }

    private void extractEnterpriseWebArchiveContexts(HTTPContext context, EnterpriseArchive enterpriseDeployment,
            WebArchive webDeployment) {

        String deploymentName = webDeployment.getName();
        for (String servletName : ServletUtils.getServletNames(webDeployment)) {
            String contextRoot = getContextContextRootFromApplicationXml(enterpriseDeployment, deploymentName);
            if (contextRoot == null) {
                contextRoot = toContextRoot(deploymentName, webDeployment);
            }
            context.add(new Servlet(servletName, contextRoot));

            if (log.isLoggable(Level.FINE)) {
                log.fine("Context " + context.getHost() + " enriched with " + servletName + " at " + contextRoot);
            }
        }
    }

    private void extractWebContext(HTTPContext context, String deploymentName, WebArchive deployment) {

        for (String servletName : ServletUtils.getServletNames(deployment)) {
            String contextRoot = toContextRoot(deploymentName, deployment);
            context.add(new Servlet(servletName, contextRoot));

            if (log.isLoggable(Level.FINE)) {
                log.fine("Context " + context.getHost() + " enriched with " + servletName + " at " + contextRoot);
            }
        }

    }

    private String toContextRoot(String deploymentName, WebArchive deployment) {

        // check archive content for jboss-web.xml
        String contextRoot = getContextNameFromJbossWebXml(deployment);
        if (contextRoot != null) {
            return contextRoot;
        }

        // root.war is a kind of special
        if ("root.war".equals(deploymentName.toLowerCase())) {
            return "";
        }

        return removeTrailingSuffix(removeFirstSlash(deploymentName));
    }

    private String getContextNameFromJbossWebXml(WebArchive deployment) {

        Node node = deployment.get(ArchivePaths.create("WEB-INF/jboss-web.xml"));
        if (node == null) {
            return null;
        }

        String contextRoot = null;
        InputStream is = null;
        try {
            is = node.getAsset().openStream();
            JbossWebDescriptor descriptor = Descriptors.importAs(JbossWebDescriptor.class).fromStream(is);
            contextRoot = descriptor.getContextRoot();
        } catch (NullPointerException e) {
            // no asset was given, ignoring
        } finally {
            IOUtils.closeQuietly(is);
        }

        return removeFirstSlash(contextRoot);

    }

    private String getContextContextRootFromApplicationXml(EnterpriseArchive earDeployment, String deploymentName) {
        Node node = earDeployment.get(ArchivePaths.create("META-INF/application.xml"));
        if (node == null) {
            return null;
        }

        String contextRoot = null;
        InputStream is = null;
        try {
            is = node.getAsset().openStream();
            ApplicationDescriptor descriptor = Descriptors.importAs(ApplicationDescriptor.class).fromStream(is);
            List<ModuleType<ApplicationDescriptor>> modules = descriptor.getAllModule();
            // get all modules and find a web module which defines contextRoot for given WAR
            for (ModuleType<ApplicationDescriptor> module : modules) {
                WebType<ModuleType<ApplicationDescriptor>> webModule = module.getOrCreateWeb();
                if (deploymentName.equals(webModule.getWebUri())) {
                    contextRoot = removeFirstSlash(webModule.getContextRoot());
                }

            }
        } catch (NullPointerException e) {
            // no asset was given, ignoring
        } finally {
            IOUtils.closeQuietly(is);
        }

        return removeFirstSlash(contextRoot);

    }

    private String removeFirstSlash(String contextPath) {
        return contextPath != null && contextPath.startsWith("/") ? contextPath.substring(1) : contextPath;
    }

    private String removeTrailingSuffix(String contextPath) {
        return contextPath != null && contextPath.indexOf(".") != -1 ? contextPath.substring(0, contextPath.lastIndexOf("."))
                : contextPath;
    }
}
