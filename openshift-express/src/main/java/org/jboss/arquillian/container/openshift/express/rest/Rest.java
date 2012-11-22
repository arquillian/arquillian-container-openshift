package org.jboss.arquillian.container.openshift.express.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import org.jboss.arquillian.container.openshift.express.OpenShiftExpressConfiguration;
import org.jboss.arquillian.container.openshift.express.util.IOUtils;
import org.jboss.arquillian.container.openshift.express.util.TrustAllSecurityManager;
import org.jboss.util.Base64;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
public class Rest {

    private static final Logger log = Logger.getLogger(Rest.class.getName());

    private final OpenShiftExpressConfiguration configuration;

    public Rest(OpenShiftExpressConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<URI> readCluterTopology() {

        StringBuilder restUrlSB = new StringBuilder();
        //build url: https://[domain]/broker/rest/domains/[Domain_ID]/applications/[App_Name]/gears
        restUrlSB.append(configuration.getRestApiUrl());
        restUrlSB.append("domains/");
        restUrlSB.append(configuration.getNamespace());
        restUrlSB.append("/applications/");
        restUrlSB.append(configuration.getApplication());
        restUrlSB.append("/gears");

        URL restUrl;
        URLConnection connection = null;
        try {
            restUrl = new URL(restUrlSB.toString());
            connection = restUrl.openConnection();
        } catch (MalformedURLException e) {
            log.log(Level.WARNING, "Cannot read OpenShift topology.", e);
        } catch (IOException e) {
            log.log(Level.WARNING, "Cannot read OpenShift topology.", e);
        }

        if (connection != null) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            try {
                //restConnection.addRequestProperty("User-Agent", "Java - Arquillian test suite.");
                httpConnection.addRequestProperty("Accept", "application/json");
                addBasicAuthPass(httpConnection);
                if (configuration.getTrustAllSslStores()) {
                    truestSelfsignedCert(httpConnection);
                }

                InputStream is = (InputStream) httpConnection.getContent();
                char[] content = IOUtils.toCharArray(is);

                JSonParser parser = new JSonParser();
                return parser.parseGears(new String(content));
            } catch (Exception e) {
                log.log(Level.WARNING, "Cannot read OpenShift topology.", e);
            } finally {
                httpConnection.disconnect();
            }
        }
        return new ArrayList<URI>();
    }

    private void truestSelfsignedCert(URLConnection restConnection) {
        if (restConnection instanceof HttpsURLConnection) {
            log.info("setting non validating ssl socket factory");
            HttpsURLConnection ssl = (HttpsURLConnection) restConnection;
            TrustAllSecurityManager.setAllTrustingValidators(ssl);
        }
    }

    private void addBasicAuthPass(URLConnection restConnection) {
        String encoded = Base64.encodeBytes((configuration.getLogin() + ":" + configuration.getPassphrase()).getBytes());
        restConnection.setRequestProperty("Authorization", "Basic " + encoded);
    }


}
