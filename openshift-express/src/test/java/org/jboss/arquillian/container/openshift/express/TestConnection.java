package org.jboss.arquillian.container.openshift.express;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.arquillian.test.spi.TestResult;

/**
 * @author Matej Lazar
 */
public class TestConnection {

    /**
     * run with -Dhttp.proxyHost=file.rdu.redhat.com -Dhttp.proxyPort=3128
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("file.rdu.redhat.com", 3128));
        Proxy proxy = null;

        URL url = new URL("http://jbossas055-mlazar055.dev.rhcloud.com:80/cluster-tests/ArquillianServletProxy?outputMode=serializedObject&className=org.jboss.test.workshop.arquillian.cluster.SimpleClusteredTestCase&methodName=testCacheOnDepA&internalHost=jbossas055-mlazar055.dev.rhcloud.com&internalPort=35536");
        URLConnection connection;
        if (proxy == null) {
            connection = url.openConnection();
        } else {
            connection = url.openConnection(proxy);
        }
        if (!(connection instanceof HttpURLConnection))
        {
           throw new IllegalStateException("Not an http connection! " + connection);
        }
        HttpURLConnection httpConnection = (HttpURLConnection) connection;
        httpConnection.setUseCaches(false);
        httpConnection.setDefaultUseCaches(false);
        httpConnection.setDoInput(true);
        try
        {
           if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
           {
              ObjectInputStream ois = new ObjectInputStream(httpConnection.getInputStream());
              Object o;
              try
              {
                 o = ois.readObject();
              }
              finally
              {
                 ois.close();
              }
              TestResult result = (TestResult)o;
              System.out.println(result.toString());
           }
        }
        finally
        {
           httpConnection.disconnect();
        }
    }
}
