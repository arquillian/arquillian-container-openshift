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

import java.util.Collection;

import javax.servlet.Servlet;

import junit.framework.Assert;

import org.jboss.arquillian.container.openshift.express.servlet.Servlet1;
import org.jboss.arquillian.container.openshift.express.servlet.Servlet2;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

/**
 * Tests archive processing
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class ArchiveUtilTestCase {

    @Test
    public void testWarContent() {
        Collection<String> classes = ArchiveUtil.getDefinedClasses(createWarDeployment1());

        Assert.assertEquals("There is one class in the archive", 1, classes.size());

        Assert.assertEquals("Transformation from path to className",
                "org.jboss.arquillian.container.openshift.express.servlet.Servlet1", classes.iterator().next());

        classes = ArchiveUtil.getDefinedClasses(createWarDeployment2());
        Assert.assertEquals("There are two classes in the archive", 2, classes.size());
    }

    @Test
    public void testWarClassLoader() {
        Collection<Class<Servlet>> servlets = ArchiveUtil.getDefinedClassesOf(createWarDeployment2(), Servlet.class);

        Assert.assertEquals("There is one Servlet implementation in the archive", 1, servlets.size());

        Assert.assertEquals("Transformation from path to className",
                "org.jboss.arquillian.container.openshift.express.servlet.Servlet1", servlets.iterator().next().getName());
    }

    @Test
    public void testEarContent() {
        Collection<String> classes = ArchiveUtil.getDefinedClasses(createEarDeployment());

        Assert.assertEquals("There are three classes in the archive", 3, classes.size());
    }

    @Test
    public void testEarClassLoader() {
        Collection<Class<Servlet>> servlets = ArchiveUtil.getDefinedClassesOf(createEarDeployment(), Servlet.class);

        Assert.assertEquals("There is two Servlet implementation in the archive", 2, servlets.size());
    }

    private EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class)
                .addAsModule(ShrinkWrap.create(WebArchive.class).addClass(Servlet1.class))
                .addAsModule(ShrinkWrap.create(WebArchive.class).addClass(Servlet2.class))
                .addAsModule(ShrinkWrap.create(JavaArchive.class).addClass(Object.class));
    }

    private WebArchive createWarDeployment1() {
        return ShrinkWrap.create(WebArchive.class).addClass(Servlet1.class);
    }

    private WebArchive createWarDeployment2() {
        return ShrinkWrap.create(WebArchive.class).addClasses(Servlet1.class, Object.class);
    }

}
