package org.jboss.arquillian.protocol.proxied_servlet;

import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
public class ProxiedServletProtocolExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(Protocol.class, ProxiedServletProtocol.class);
    }

}
