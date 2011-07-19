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

/**
 * Enumeration of supported cartridges in OpenShift Express
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
enum CartridgeType {
    JBOSSAS7("jbossas-7.0") {
        @Override
        public String getDeploymentDir() {
            return "deployments/";
        }
    },
    PHP53("php-5.3") {
        @Override
        public String getDeploymentDir() {
            return "/";
        }
    },
    WSGI32("wsgi-3.2") {
        @Override
        public String getDeploymentDir() {
            return "/";
        }
    },
    PERL510("perl-5.10") {
        @Override
        public String getDeploymentDir() {
            return "/";
        }
    },
    RACK11("rack-1.1") {
        @Override
        public String getDeploymentDir() {
            return "/";
        }
    };

    private String name;

    CartridgeType(String name) {
        this.name = name;
    }

    /**
     * Gets a Git subdirectory where deployments should go
     *
     * @return the Git repository path
     */
    public abstract String getDeploymentDir();

    /**
     * Constructs cartridge type using cartridge name
     *
     * @param type the type
     * @return the cartridge
     * @throws IllegalArgumentException if no cartridge is defined for given type
     */
    public static CartridgeType typeOf(String type) {
        for (CartridgeType cartridge : CartridgeType.values()) {
            if (cartridge.name.equals(type)) {
                return cartridge;
            }
        }

        throw new IllegalArgumentException("Unknown OpenShift Express cartridge type " + type
                + ", please contact container developer or file a JIRA");
    }
}
