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
package org.jboss.arquillian.container.openshift;

import junit.framework.Assert;

import org.jboss.arquillian.container.openshift.OpenShiftContainerConfiguration;
import org.jboss.arquillian.container.openshift.ProtocolMetaDataParser;
import org.jboss.arquillian.container.openshift.servlet.Servlet1;
import org.jboss.arquillian.container.openshift.servlet.Servlet2;
import org.jboss.arquillian.container.openshift.servlet.Servlet3;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.application5.ApplicationDescriptor;
import org.jboss.shrinkwrap.descriptor.api.jbossweb60.JbossWebDescriptor;
import org.junit.Test;

/**
 * Tests metadata creation
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="http://community.jboss.org/people/jharting">Jozef Hartinger</a>
 *
 */
public class ProtocolMetadataParserTestCase {

    @Test
    public void testServletsInEar() {

        OpenShiftContainerConfiguration configuration = new OpenShiftContainerConfiguration();
        ProtocolMetaDataParser parser = new ProtocolMetaDataParser(configuration);

        ProtocolMetaData data = parser.parse(sampleEar());

        HTTPContext context = data.getContext(HTTPContext.class);
        Assert.assertNotNull(context.getServletByName("Servlet1"));

        String contextRoot = context.getServletByName("Servlet1").getContextRoot();
        Assert.assertEquals("Context root of arquillian1.war is set correctly", "/arquillian1", contextRoot);
    }

    @Test
    public void testServletsInWar() {

        OpenShiftContainerConfiguration configuration = new OpenShiftContainerConfiguration();
        ProtocolMetaDataParser parser = new ProtocolMetaDataParser(configuration);

        ProtocolMetaData data = parser.parse(sampleWar());

        HTTPContext context = data.getContext(HTTPContext.class);

        Assert.assertNotNull(context.getServletByName("Servlet1"));

        String contextRoot = context.getServletByName("Servlet1").getContextRoot();
        Assert.assertEquals("Context root of ROOT.war is set correctly", "", contextRoot);
    }

    @Test
    public void testNoServlets() {
        OpenShiftContainerConfiguration configuration = new OpenShiftContainerConfiguration();
        ProtocolMetaDataParser parser = new ProtocolMetaDataParser(configuration);

        ProtocolMetaData data = parser.parse(sampleWarNoServlets());

        HTTPContext context = data.getContext(HTTPContext.class);

        Assert.assertNotNull(context.getServletByName("default"));
        String contextRoot = context.getServletByName("default").getContextRoot();
        Assert.assertEquals("Context root of arquillian.war is set correctly", "/arquillian", contextRoot);
    }

    @Test
    public void testNoServletsJBossWebXml() {
        OpenShiftContainerConfiguration configuration = new OpenShiftContainerConfiguration();
        ProtocolMetaDataParser parser = new ProtocolMetaDataParser(configuration);

        ProtocolMetaData data = parser.parse(sampleWarJBossWebXml());

        HTTPContext context = data.getContext(HTTPContext.class);

        Assert.assertNotNull(context.getServletByName("default"));
        String contextRoot = context.getServletByName("default").getContextRoot();
        Assert.assertEquals("Context root of arquillian.war is set correctly", "/the-context-root", contextRoot);
    }

    /**
     * Verifies that parsing does not fail on a package imported using ZipImporter.
     *
     * @see ARQ-621
     */
    @Test
    public void testImportedWar() {
        OpenShiftContainerConfiguration configuration = new OpenShiftContainerConfiguration();
        ProtocolMetaDataParser parser = new ProtocolMetaDataParser(configuration);

        ProtocolMetaData data = parser.parse(sampleImportedWar());

        HTTPContext context = data.getContext(HTTPContext.class);

        Assert.assertNotNull(context.getServletByName("default"));
        String contextRoot = context.getServletByName("default").getContextRoot();
        Assert.assertEquals("Context root of arquillian.war is set correctly", "/test", contextRoot);
    }

    @Test
    public void testEarWithApplicationXml() {
        OpenShiftContainerConfiguration configuration = new OpenShiftContainerConfiguration();
        ProtocolMetaDataParser parser = new ProtocolMetaDataParser(configuration);

        ProtocolMetaData data = parser.parse(sampleEarWithApplicationXml());

        HTTPContext context = data.getContext(HTTPContext.class);
        Assert.assertNotNull(context.getServletByName("Servlet1"));

        String contextRoot = context.getServletByName("Servlet1").getContextRoot();
        Assert.assertEquals("Context root of arquillian1.war is set correctly", "/arquillian-ear-1", contextRoot);

        Assert.assertNotNull(context.getServletByName("Servlet2"));
        contextRoot = context.getServletByName("Servlet2").getContextRoot();

        Assert.assertEquals("Context root of arquillian1.war is set correctly", "/arquillian-ear-2", contextRoot);

    }

    private EnterpriseArchive sampleEar() {
        return ShrinkWrap.create(EnterpriseArchive.class)
                .addAsModule(ShrinkWrap.create(WebArchive.class, "arquillian1.war").addClass(Servlet1.class))
                .addAsModule(ShrinkWrap.create(WebArchive.class, "arquillian2.war").addClasses(Servlet2.class, Servlet3.class));
    }

    private EnterpriseArchive sampleEarWithApplicationXml() {
        return ShrinkWrap
                .create(EnterpriseArchive.class)
                .addAsModule(ShrinkWrap.create(WebArchive.class, "arquillian1.war").addClass(Servlet1.class))
                .addAsModule(ShrinkWrap.create(WebArchive.class, "arquillian2.war").addClasses(Servlet2.class, Servlet3.class))
                .setApplicationXML(
                        new StringAsset(Descriptors.create(ApplicationDescriptor.class).createModule().getOrCreateWeb()
                                .webUri("arquillian1.war").contextRoot("arquillian-ear-1").up().up().createModule()
                                .getOrCreateWeb().webUri("arquillian2.war").contextRoot("arquillian-ear-2").up().up()
                                .exportAsString()));
    }

    private WebArchive sampleWar() {
        return ShrinkWrap.create(WebArchive.class, "ROOT.war").addClass(Servlet1.class);
    }

    private WebArchive sampleWarNoServlets() {
        return ShrinkWrap.create(WebArchive.class, "arquillian.war").addClass(Object.class);
    }

    private WebArchive sampleWarJBossWebXml() {
        return ShrinkWrap
                .create(WebArchive.class, "arquillian.war")
                .addClass(Object.class)
                .add(new StringAsset(Descriptors.create(JbossWebDescriptor.class).contextRoot("the-context-root")
                        .exportAsString()), ArchivePaths.create("WEB-INF/jboss-web.xml"));
    }

    private WebArchive sampleImportedWar() {

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war").addClass(Object.class)
                .addAsLibrary(ShrinkWrap.create(JavaArchive.class, "test.jar").addClass(String.class));

        return ShrinkWrap.create(ZipImporter.class, "test.war").importFrom(archive.as(ZipExporter.class).exportAsInputStream())
                .as(WebArchive.class);
    }
}
