package org.jboss.arquillian.container.openshift.express.util;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
public class TrustAllSecurityManager {

    private static final Logger log = Logger.getLogger(TrustAllSecurityManager.class.getName());

    private static SSLSocketFactory instance = null;
    private static SSLContext sslContext = null;

    public static SSLContext getSslContextInstance() {
        if (sslContext == null) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, new TrustManager[]{new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        log.fine("getAcceptedIssuers");
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs,
                                                   String authType) {
                        log.fine("checkClientTrusted =============");
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs,
                                                   String authType) {
                        log.fine("checkServerTrusted =============");
                    }
                }}, new SecureRandom());
                return sc;
            } catch (Exception e) {
                log.warning("could not create ssl context" + e);
            }
        }
        return sslContext;
    }

    public static SSLSocketFactory instance() {
        if (instance == null) {

            // Install the all-trusting trust manager
            try {

                instance = getSslContextInstance().getSocketFactory();

                //HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                return null;
            }
        }
        return instance;

    }

    public static void setAllTrustingValidators(HttpsURLConnection ssl) {
        ssl.setSSLSocketFactory(instance());
        ssl.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

    }
}
