package org.jboss.arquillian.container.openshift.express;

import java.net.URI;
import java.util.List;

import org.jboss.arquillian.container.openshift.express.rest.Rest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Matej Lazar
 */
public class RestTest {

    protected String restApiUrl = "https://localhost:8081/broker/rest/";
    protected String nameSpace = "mlazar054";
    protected String application = "jbossas054";
    protected String login = "mlazar054";
    protected String passphrase = "abc";

    @Test
    public void parseClusterTopology() {
        OpenShiftExpressConfiguration configuration = mockConfiguration();
        Rest rest = new Rest(configuration);
        List<URI> uris = rest.readCluterTopology();
        Assert.assertEquals(2, uris.size());

    }


    private OpenShiftExpressConfiguration mockConfiguration() {
        OpenShiftExpressConfiguration configuration = new OpenShiftExpressConfiguration() {

            @Override
            public String getRestApiUrl() {
                return restApiUrl;
            }

            @Override
            public String getNamespace() {
                return nameSpace;
            }

            @Override
            public String getApplication() {
                return application;
            }

            @Override
            public String getLogin() {
                return login;
            }

            @Override
            public String getPassphrase() {
                return passphrase;
            }
        };
        return configuration;
    }
}
