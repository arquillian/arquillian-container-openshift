package org.jboss.arquillian.protocol.proxied_servlet;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
public class ProxyAwareHTTPContext extends HTTPContext {

    private String arquillianProxyServletHost;
    private int arquillianProxyServletPort;

    public ProxyAwareHTTPContext(String name, String host, int port) {
        super(name, host, port);
    }

    public String getArquillianProxyServletHost()
    {
       return arquillianProxyServletHost;
    }

    public void setArquillianProxyServletHost(String arquillianProxyServletHost)
    {
       this.arquillianProxyServletHost = arquillianProxyServletHost;
    }

    public int getArquillianProxyServletPort()
    {
       return arquillianProxyServletPort;
    }

    public void setArquillianProxyServletPort(int arquillianProxyServletPort)
    {
       this.arquillianProxyServletPort = arquillianProxyServletPort;
    }

}
