package org.jboss.arquillian.container.openshift.express;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.protocol.servlet.ServletMethodExecutor;
import org.jboss.arquillian.protocol.servlet.runner.ServletTestRunner;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class ProtocolMetaDataParser {
    private static final Logger log = Logger.getLogger(ProtocolMetaDataParser.class.getName());

    private OpenShiftExpressConfiguration configuration;

    public ProtocolMetaDataParser(OpenShiftExpressConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("OpenShift Express Configuration must be specified");
        }
        this.configuration = configuration;
    }

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

        // FIXME include application.xml as well
        extractWebContext(context, webDeployment.getName(), webDeployment);
    }

    private void extractWebContext(HTTPContext context, String deploymentName, WebArchive deployment) {

        Collection<Class<javax.servlet.Servlet>> servlets = ArchiveUtil.getDefinedClassesOf(deployment,
                javax.servlet.Servlet.class);

        for (Class<javax.servlet.Servlet> servlet : servlets) {
            context.add(new Servlet(getServletName(servlet), toContextName(deploymentName)));

            if (log.isLoggable(Level.FINE)) {
                log.fine("Context " + context.getHost() + " enriched with " + getServletName(servlet) + " at "
                        + toContextName(deploymentName));
            }
        }

    }

    private String getServletName(Class<javax.servlet.Servlet> clazz) {
        // FIXME scan for annotation
        String className = clazz.getSimpleName();

        // Arquillian Servlet Name hook
        if (ServletTestRunner.class.getSimpleName().equals(className)) {
            return ServletMethodExecutor.ARQUILLIAN_SERVLET_NAME;
        }

        return className;
    }

    private String toContextName(String deploymentName) {

        // root.war is a kind of special
        if ("root.war".equals(deploymentName.toLowerCase())) {
            return "";
        }

        String correctedName = deploymentName;
        if (correctedName.startsWith("/")) {
            correctedName = correctedName.substring(1);
        }
        if (correctedName.indexOf(".") != -1) {
            correctedName = correctedName.substring(0, correctedName.lastIndexOf("."));
        }
        return correctedName;
    }

}
