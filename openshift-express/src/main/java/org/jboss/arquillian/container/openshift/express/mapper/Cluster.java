package org.jboss.arquillian.container.openshift.express.mapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
class Cluster {

    private String id;
    private List<URI> uris = new ArrayList<URI>();
    private Map<String, Integer> indexes = new HashMap<String, Integer>();

    public Cluster(String id) {
        this.id = id;
    }

    public void add(URI uri) {
        uris.add(uri);
    }

    public String getId() {
        return id;
    }

    /**
     * Get next URI for a group.
     */
    public URI pick(String group) {
        try {
            return uris.get(nextIndex(group));
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("No remaining uris for group " + group + ".");
        }
    }

    /**
     * Resets index of picked URIs.
     */
    public void reset() {
        indexes.clear();
    }

    private int nextIndex(String group) {
        Integer index = indexes.get(group);
        if (index == null) {
            index = 0;
        }
        indexes.put(group, index + 1);
        return index;
    }

    @Override
    public String toString() {
        return id + " " + uris.toString();
    }
}
