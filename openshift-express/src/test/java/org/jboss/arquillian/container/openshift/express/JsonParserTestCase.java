package org.jboss.arquillian.container.openshift.express;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.jboss.arquillian.container.openshift.express.rest.JSonParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Matej Lazar
 */
public class JsonParserTestCase {
    //TODO remove concrete URL
    private final String jsonGears = "{\"data\":[{\"components\":[{\"name\":\"jbossas-7\",\"proxy_host\":\"jboss051a-mlazar051.dev.rhcloud.com\",\"proxy_port\":35536,\"internal_port\":8080},{\"name\":\"haproxy-1.4\",\"proxy_host\":null,\"proxy_port\":null,\"internal_port\":null}],\"uuid\":\"b8e550988a0940b08648e3334b8a459b\"},{\"components\":[{\"name\":\"jbossas-7\",\"proxy_host\":\"3e64f72b38-mlazar051.dev.rhcloud.com\",\"proxy_port\":35541,\"internal_port\":8080}],\"uuid\":\"3e64f72b38724d7c84bbb49f1e81d63d\"}],\"messages\":[],\"status\":\"ok\",\"supported_api_versions\":[1.0,1.1,1.2,1.3],\"type\":\"gears\",\"version\":\"1.3\"}";

    @Test
    public void getListOfGears() throws URISyntaxException {
        JSonParser parser = new JSonParser();
        List<URI> gears = parser.parseGears(jsonGears);

        Assert.assertEquals(2, gears.size());
    }
}
