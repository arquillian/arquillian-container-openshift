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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.container.spi.client.deployment.Validate;

/**
 * A {@link org.jboss.arquillian.container.spi.client.container.ContainerConfiguration} implementation for the OpenShift Express
 * container.
 *
 * <p>
 * Following configuration properties are required in order to run:
 * </p>
 * <ul>
 * <li>namespace - a namespace created by rhc-create-domain tool, e.g. bar</li>
 * <li>application - an application name created by rhc-create-app tool, e.g. foo</li>
 * <li>login - a Red Hat login (RHN with OpenShift Express access, e.g. bar@redhat.com</li>
 * <li>sshUserName - an user name generated when an application is created by rhc-create-app tool, e.g.
 * a7b1daad5c624157bdeea60b26cf8eba
 * </ul>
 *
 * <p>
 * Following configuration properties have sensible defaults, but can be modified:
 * </p>
 * <ul>
 * <li>type - cartridge type, e.g. jbossas-7.0</li>
 * <li>libraDomain - domain where OpenShift server instance is running, e.g. rhcloud.com</li>
 * </ul>
 *
 * <p>
 * Following configuration properties are optional
 * </p>
 * <ul>
 * <li>passphrase - the passphrase to SSH key, can be set via SSH_PASSPHRASE environment variable</li>
 * </ul>
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @version $Revision: $
 */
public class OpenShiftExpressConfiguration implements ContainerConfiguration {

    private String type = "jbossas-7.0";

    private String namespace;

    private String application;

    private String login;

    private String libraDomain = "rhcloud.com";

    private String sshUserName;

    private CartridgeType cartridgeType;

    private String passphrase = System.getenv("SSH_PASSPHRASE");

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.arquillian.spi.client.container.ContainerConfiguration#validate()
     */
    public void validate() throws ConfigurationException {
        Validate.notNullOrEmpty(namespace,
                "OpenShift Express namespace must be specified, please fill in \"namespace\" property in Arquillian configuration");
        Validate.notNullOrEmpty(application,
                "Application name must be specified, please fill in \"application\" property in Arquillian configuration");
        Validate.notNullOrEmpty(login,
                "OpenShift Express login must be specified, please fill in \"login\" property in Arquillian configuration");
        Validate.notNullOrEmpty(sshUserName,
                "OpenShift Express SSH username must not be empty, please fill in \"sshUserName\" property in Arquillian configuration");
        Validate.notNullOrEmpty(libraDomain,
                "OpenShift Express Libra Domain must not be empty, please fill in \"libraDomain\" property in Arquillian configuration");
        Validate.notNullOrEmpty(type,
                "OpenShift Express Cartridge Type must be specified, please fill in \"libraDomain\" property in Arquillian configuration");

        this.cartridgeType = CartridgeType.typeOf(type);

        // construct compound values and validate them
        getRemoteRepositoryUri();
        getRootContextUrl();
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
        this.setCartridgeType(CartridgeType.typeOf(type));
    }

    /**
     * @param cartridgeType the cartridgeType to set
     */
    public void setCartridgeType(CartridgeType cartridgeType) {
        this.cartridgeType = cartridgeType;
    }

    /**
     * @return the cartridgeType
     */
    public CartridgeType getCartridgeType() {
        return cartridgeType;
    }

    /**
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the application
     */
    public String getApplication() {
        return application;
    }

    /**
     * @param application the application to set
     */
    public void setApplication(String application) {
        this.application = application;
    }

    /**
     * @return the login
     */
    public String getLogin() {
        return login;
    }

    /**
     * @param login the login to set
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * @return the libraDomain
     */
    public String getLibraDomain() {
        return libraDomain;
    }

    /**
     * @param libraDomain the libraDomain to set
     */
    public void setLibraDomain(String libraDomain) {
        this.libraDomain = libraDomain;
    }

    /**
     * @return the sshUserName
     */
    public String getSshUserName() {
        return sshUserName;
    }

    /**
     * @param sshUserName the sshUserName to set
     */
    public void setSshUserName(String sshUserName) {
        this.sshUserName = sshUserName;
    }

    /**
     * @return the rootContextUrl
     */
    public String getRootContextUrl() {
        try {
            return constructRootContext().toURI().toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Application name, namespace and Libra Domain does not represent a valid URL", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Application name, namespace and Libra Domain does not represent a valid URL", e);
        }
    }

    /**
     * @return the remoteRepositoryUri
     */
    public String getRemoteRepositoryUri() {
        try {
            return constructRemoteRepositoryURI().toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Application name, namespace, Libra Domain and SSH User Name does not represent a valid Git URL", e);
        }
    }

    public String getHostName() {
        StringBuilder sb = new StringBuilder();
        sb.append(application).append("-").append(namespace).append(".").append(libraDomain);
        return sb.toString();
    }

    /**
     * @param passphrase the passphrase to set
     */
    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    /**
     * @return the passphrase
     */
    public String getPassphrase() {
        return passphrase;
    }

    private URL constructRootContext() throws MalformedURLException {

        StringBuilder sb = new StringBuilder("http://");
        sb.append(getHostName());

        return new URL(sb.toString());
    }

    private URI constructRemoteRepositoryURI() throws URISyntaxException {
        StringBuilder sb = new StringBuilder("ssh://");
        sb.append(sshUserName).append("@").append(getHostName()).append("/~/git/").append(application).append(".git/");

        return new URI(sb.toString());
    }
}