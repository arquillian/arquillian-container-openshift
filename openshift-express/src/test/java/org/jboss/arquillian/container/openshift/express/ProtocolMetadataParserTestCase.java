/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

/**
 * Tests metadata creation
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class ProtocolMetadataParserTestCase {

    @Test
    public void testServletsInEAR() {

        OpenShiftExpressConfiguration configuration = new OpenShiftExpressConfiguration();
        ProtocolMetaDataParser parser = new ProtocolMetaDataParser(configuration);

        ProtocolMetaData data = parser.parse(sampleEAR());

        HTTPContext context = data.getContext(HTTPContext.class);

        Assert.assertNotNull(context.getServletByName("Servlet1"));

    }

    private EnterpriseArchive sampleEAR() {
        return ShrinkWrap.create(EnterpriseArchive.class)
                .addAsModule(ShrinkWrap.create(WebArchive.class).addClass(Servlet1.class))
                .addAsModule(ShrinkWrap.create(WebArchive.class).addClasses(Servlet2.class, Servlet3.class));
    }
}
