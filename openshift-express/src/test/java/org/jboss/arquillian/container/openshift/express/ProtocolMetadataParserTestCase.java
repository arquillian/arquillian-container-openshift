package org.jboss.arquillian.container.openshift.express;

import junit.framework.Assert;

import org.jboss.arquillian.container.openshift.express.servlet.Servlet1;
import org.jboss.arquillian.container.openshift.express.servlet.Servlet2;
import org.jboss.arquillian.container.openshift.express.servlet.Servlet3;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

public class ProtocolMetadataParserTestCase {

    @Test
    public void testServletsInEAR() {

        OpenShiftExpressConfiguration configuration = new OpenShiftExpressConfiguration();
        ProtocolMetaDataParser parser = new ProtocolMetaDataParser(configuration);

        ProtocolMetaData data = parser.parse(sampleEAR());

        HTTPContext context = data.getContext(HTTPContext.class);

        System.out.println(context.getServlets());

        Assert.assertNotNull(context.getServletByName("Servlet1"));

    }

    private EnterpriseArchive sampleEAR() {
        return ShrinkWrap.create(EnterpriseArchive.class)
                .addAsModule(ShrinkWrap.create(WebArchive.class).addClass(Servlet1.class))
                .addAsModule(ShrinkWrap.create(WebArchive.class).addClasses(Servlet2.class, Servlet3.class));
    }
}
