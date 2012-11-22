package org.jboss.arquillian.container.openshift.express.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
public class JSonParser {

    public List<URI> parseGears(String jsonString) throws URISyntaxException {

        List<URI> internalNodes = new ArrayList<URI>();

        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray componentsArray = json.getJSONArray("data");
        for (Object components : componentsArray) {
            for (Object componentObj : ((JSONObject)components).getJSONArray("components")) {
                JSONObject component = (JSONObject) componentObj;
                String name = component.getString("name");
                //TODO make cofigurable use configuration.cartridgeType
                if ("jbossas-7".equals(name)) {
                    String host = component.getString("proxy_host");
                    String port = component.getString("proxy_port");
                    URI internalNodeURI = new URI("http://" + host + ":" + port);
                    internalNodes.add(internalNodeURI);
                }
            }
        }
        return internalNodes;
    }

}
