package org.jboss.arquillian.container.openshift.express.mapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.arquillian.container.openshift.express.OpenShiftExpressConfiguration;
import org.jboss.arquillian.container.openshift.express.rest.Rest;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
public class OpenShiftTopology {

    private static final Logger log = Logger.getLogger(OpenShiftTopology.class.getName());

    private static OpenShiftTopology INSTANCE = new OpenShiftTopology();

    private OpenShiftTopology() {}

    private List<Cluster> clusters = new ArrayList<Cluster>();

    public static OpenShiftTopology instance() {
        return INSTANCE;
    }

    /**
     * Each time returns next node for a deployment
     */
    public URI pickNode(String clusterId, String deploymentName) {
        Cluster cluster = getCluster(clusterId);
        if (cluster != null) {
            return cluster.pick(deploymentName);
        }
        return null;
    }

    public void addNode(String clusterId, URI uri) {
        Cluster cluster = getCluster(clusterId);
        if (cluster == null) {
            cluster = new Cluster(clusterId);
            clusters.add(cluster);
        }
        cluster.add(uri);
    }

    public boolean isClusterParsed(String clusterId) {
        return getCluster(clusterId) != null;
    }

    /**
     * Reset indexes of returned nodes.
     */
    public void reset() {
        for (Cluster cluster : clusters) {
            cluster.reset();
        }
    }

    @Override
    public String toString() {
        return clusters.toString();
    }

    /**
     * Read cluster topology if not read yet
     */
    public void parseCluster(OpenShiftExpressConfiguration configuration) {
       if (!isClusterParsed(getClusterId(configuration))) {
          Rest rest = new Rest(configuration);
          List<URI> internalNodes = rest.readCluterTopology();
          for (URI internalNode : internalNodes)
          {
             addNode(getClusterId(configuration), internalNode);
          }
          log.info("OpenShift cluster: " + toString());
       }
    }

    private Cluster getCluster(String clusterId) {
        for (Cluster cluster : clusters) {
            if (cluster.getId().equals(clusterId)) {
                return cluster;
            }
        }
        return null;
    }

    public String getClusterId(OpenShiftExpressConfiguration configuration) {
        return configuration.getLibraDomain() + ":" + configuration.getNamespace() + ":" + configuration.getApplication();
    }

    public List<URI> getAllNodes() {
        List<URI> uris = new ArrayList<URI>();
        for (Cluster cluster : clusters) {
            uris.addAll(cluster.getURIs());
        }
        return uris;
    }

}
