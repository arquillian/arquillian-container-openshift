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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.arquillian.container.openshift.express.archive.ArchiveUtil;
import org.jboss.arquillian.protocol.servlet.ServletMethodExecutor;
import org.jboss.arquillian.protocol.servlet.runner.ServletTestRunner;
import org.jboss.shrinkwrap.api.Archive;

/**
 * This utility provided possibility to work with javax.servlet without having them explicitly on compiler classpath.
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
class ServletUtils {

    private static final String SERVLET_CLASS_NAME = "javax.servlet.Servlet";
    private static final String WEB_SERVLET_ANNOTATION_CLASS_NAME = "javax.servlet.annotation.WebServlet";

    /**
     * Gets servlet names for all classes in the archive. Class must inherit from javax.servlet.Servlet in order to be marked as
     * Servlet. Classes are scanned for a presence of WebServlet annotation as well. If no such annotation is found, simple name
     * of the class is returned.
     *
     * <p>
     * If no javax.servlet.Servlet is found on the class path the only name returned is Arquillian Runner.
     * </p>
     * <p>
     * Arquillian Runner Servlet has special treatment.
     * </p>
     *
     * @param deployment the deployment to be scanned
     * @return List of servlet names
     */
    public static Collection<String> getServletNames(Archive<?> deployment) {

        // Arquillian Runner server hook
        if (!isServletOnClasspath()) {
            return Collections.singletonList(ServletMethodExecutor.ARQUILLIAN_SERVLET_NAME);
        }

        // this is nasty but we can't determine this dynamically
        @SuppressWarnings("rawtypes")
        Class javaxServlet = forName(SERVLET_CLASS_NAME);
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Collection servlets = ArchiveUtil.getDefinedClassesOf(deployment, javaxServlet);

        List<String> names = new ArrayList<String>(servlets.size());
        for (Object servletClass : servlets) {
            names.add(getServletName((Class<?>) servletClass));
        }

        // jsp/default servlet for WAR archives
        if (ArchiveUtil.isWarArchive(deployment)) {
            names.add("default");
        }

        return names;

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static String getServletName(Class<?> clazz) {

        // FIXME scan for web.xml

        // dynamically scan for annotation
        if (!isWebServletAnnotationOnClasspath()) {
            return applyArquillianHook(clazz.getSimpleName());
        }

        Class webServlet = forName(WEB_SERVLET_ANNOTATION_CLASS_NAME);
        if (clazz.isAnnotationPresent(webServlet)) {
            Object webServletAnnon = clazz.getAnnotation(webServlet);

            Method nameMethod = SecurityActions.getMethod(webServlet, "name");
            if (nameMethod != null) {
                String servletName = null;
                try {
                    servletName = (String) nameMethod.invoke(webServletAnnon);
                } catch (IllegalArgumentException e) {
                    // ignore
                } catch (IllegalAccessException e) {
                    // ignore
                } catch (InvocationTargetException e) {
                    // ignore
                }

                if (servletName != null && servletName.length() != 0) {
                    return servletName;
                }
            }

        }

        // name was not found in annotation, revert to simple class name
        return applyArquillianHook(clazz.getSimpleName());
    }

    private static final String applyArquillianHook(String name) {
        if (ServletTestRunner.class.getSimpleName().equals(name)) {
            return ServletMethodExecutor.ARQUILLIAN_SERVLET_NAME;
        }

        return name;
    }

    private static final boolean isServletOnClasspath() {
        return classExists(SERVLET_CLASS_NAME);
    }

    private static final boolean isWebServletAnnotationOnClasspath() {
        return classExists(WEB_SERVLET_ANNOTATION_CLASS_NAME);
    }

    private static final boolean classExists(String className) {
        {
            try {
                Class.forName(className);
                return true;
            } catch (ClassNotFoundException exception) {
                return false;
            }
        }
    }

    private static final Class<?> forName(String className) {
        {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException exception) {
                return null;
            }
        }
    }
}