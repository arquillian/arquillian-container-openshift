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

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

/**
 * SecurityActions
 *
 * A set of privileged actions that are not to leak out of this package
 *
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 *
 * @version $Revision: $
 */
final class SecurityActions {

    // -------------------------------------------------------------------------------||
    // Constructor
    // ------------------------------------------------------------------||
    // -------------------------------------------------------------------------------||

    /**
     * No instantiation
     */
    private SecurityActions() {
        throw new UnsupportedOperationException("No instantiation");
    }

    // -------------------------------------------------------------------------------||
    // Utility Methods
    // --------------------------------------------------------------||
    // -------------------------------------------------------------------------------||

    static List<Method> getMethods(final Class<?> source) {
        List<Method> declaredAccessableMethods = AccessController.doPrivileged(new PrivilegedAction<List<Method>>() {
            public List<Method> run() {
                return Arrays.asList(source.getDeclaredMethods());
            }
        });
        return declaredAccessableMethods;
    }

    static Method getMethod(final Class<?> source, final String name, final Class<?>... parameterTypes) {
        Method declaredAccessableMethod = AccessController.doPrivileged(new PrivilegedAction<Method>() {
            public Method run() {
                try {
                    return source.getDeclaredMethod(name, parameterTypes);
                } catch (SecurityException e) {

                } catch (NoSuchMethodException e) {

                }
                return null;
            }
        });
        return declaredAccessableMethod;
    }
}
