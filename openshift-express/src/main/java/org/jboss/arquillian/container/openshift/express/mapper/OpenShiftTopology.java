package org.jboss.arquillian.container.openshift.express.mapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
public class OpenShiftTopology {

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

    @Override
    public String toString() {
        return clusters.toString();
    }

    private Cluster getCluster(String clusterId) {
        for (Cluster cluster : clusters) {
            if (cluster.getId().equals(clusterId)) {
                return cluster;
            }
        }
        return null;
    }

}
