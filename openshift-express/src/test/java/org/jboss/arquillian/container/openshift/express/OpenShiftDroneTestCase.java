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

import java.net.URL;

import org.jboss.arquillian.ajocado.Ajocado;
import org.jboss.arquillian.ajocado.framework.AjaxSelenium;
import org.jboss.arquillian.ajocado.locator.IdLocator;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBossEmbeddedIntegrationTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
@Ignore
public class OpenShiftDroneTestCase {

    @Drone
    AjaxSelenium browser;

    private static final IdLocator ARQUILLIAN = Ajocado.id("arquillian.info");

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "ROOT.war").addAsDirectories("images")
                .addAsWebResource("test.war/health.jsp", "health.jsp").addAsWebResource("test.war/index.html", "index.html")
                .addAsWebResource("test.war/snoop.jsp", "snoop.jsp").setWebXML("test.war/WEB-INF/web.xml");
    }

    @Test
    public void testApplicationIsDeployed(@ArquillianResource URL contextPath) throws Exception {
        browser.open(contextPath);
        browser.waitForPageToLoad();

        Assert.assertTrue(browser.isElementPresent(ARQUILLIAN));
    }
}
