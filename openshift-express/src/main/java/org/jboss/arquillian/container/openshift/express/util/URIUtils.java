package org.jboss.arquillian.container.openshift.express.util;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
public class URIUtils {

    public static Proxy parseProxy(String proxyUrl) throws MalformedURLException {
        String[] proxyHostPort = proxyUrl.split(":");
        if (proxyHostPort.length != 2) {
            throw new MalformedURLException("Invalid proxy configuration. Define proxy as host:port.");
        }
        String host = proxyHostPort[0];
        int port = Integer.parseInt(proxyHostPort[1]);
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }
}
