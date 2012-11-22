package org.jboss.arquillian.container.openshift.express.mapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
class Cluster {

    private String id;
    private List<URI> uris = new ArrayList<URI>();
    private int index = 0;

    public Cluster(String id) {
        this.id = id;
    }

    public void add(URI uri) {
        uris.add(uri);
    }

    public String getId() {
        return id;
    }

    public URI pick() {
        URI uri = uris.get(index);
        index++;
        return uri;
    }

    @Override
    public String toString() {
        return id + " " + uris.toString();
    }
}
