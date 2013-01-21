package org.jboss.arquillian.protocol.proxied_servlet.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.protocol.servlet.ServletMethodExecutor;

/**
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 */
@WebServlet(name="ArquillianServletProxy", urlPatterns = {"/ArquillianServletProxy"})
public class RemoteProxyServlet extends HttpServlet {

    public static final String SERVLET_NAME = "ArquillianServletProxy";
    public static final String SERVLET_MAPPING = "/ArquillianServletProxy";
    private static Logger log = Logger.getLogger(RemoteProxyServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        doPost(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){

        BufferedInputStream webToProxyBuf = null;
        BufferedOutputStream proxyToClientBuf = null;
        HttpURLConnection con = null;

        try{
            int statusCode;
            int oneByte;
            String methodName;
            String headerText;

            URL url = getNodeUrl(request);

            log.info("Fetching >"+url.toString());

            con =(HttpURLConnection) url.openConnection();

            methodName = request.getMethod();
            con.setRequestMethod(methodName);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setFollowRedirects(false);
            con.setUseCaches(true);

            for( Enumeration e = request.getHeaderNames() ; e.hasMoreElements();){
                String headerName = e.nextElement().toString();
                con.setRequestProperty(headerName, request.getHeader(headerName));
            }

            con.connect();

            if(methodName.equals("POST")){
                BufferedInputStream clientToProxyBuf = new BufferedInputStream(request.getInputStream());
                BufferedOutputStream proxyToWebBuf     = new BufferedOutputStream(con.getOutputStream());

                while ((oneByte = clientToProxyBuf.read()) != -1)
                    proxyToWebBuf.write(oneByte);

                proxyToWebBuf.flush();
                proxyToWebBuf.close();
                clientToProxyBuf.close();
            }

            statusCode = con.getResponseCode();
            response.setStatus(statusCode);

//            for( Iterator i = con.getHeaderFields().entrySet().iterator() ; i.hasNext() ;){
//                Map.Entry mapEntry = (Map.Entry)i.next();
//                if(mapEntry.getKey()!=null)
//                    response.setHeader(mapEntry.getKey().toString(), ((List)mapEntry.getValue()).get(0).toString());
//            }

            InputStream is = null;
            try {
                is = con.getInputStream();
            } catch (FileNotFoundException e) {
                log.log(Level.FINE, "Not found:", e);
            }
            if (is != null) {
                webToProxyBuf = new BufferedInputStream(is);
                proxyToClientBuf = new BufferedOutputStream(response.getOutputStream());

                while ((oneByte = webToProxyBuf.read()) != -1)
                    proxyToClientBuf.write(oneByte);
            }

        }catch(Exception e){
            log.log(Level.SEVERE, "", e);
        }
        finally {
            try {
                if (proxyToClientBuf != null) {
                    proxyToClientBuf.flush();
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Cannot flush:", e);
            }

            try {
                if (proxyToClientBuf != null) {
                    proxyToClientBuf.close();
                }

                if (webToProxyBuf != null) {
                    webToProxyBuf.close();
                }

                if (con != null) {
                    con.disconnect();
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Cannot close conection", e);
            }

        }
    }

    private URL getNodeUrl(HttpServletRequest request) throws MalformedURLException {
        StringBuilder sb = new StringBuilder("http://");
        String host = request.getParameter("internalHost");
        String port = request.getParameter("internalPort");


        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append(getTestUrl(request));

        return new URL(sb.toString());
    }

    private StringBuilder getTestUrl(HttpServletRequest request) {
        String pingArchName = request.getParameter("pingArchiveName");

        StringBuilder urlSb = new StringBuilder();
        if (pingArchName != null && !pingArchName.equals("")) {
            urlSb.append("/");
            urlSb.append(pingArchName);
            urlSb.append("/deploy?name=");
            urlSb.append(request.getParameter("deploymentName"));
        } else {
            urlSb.append(request.getContextPath());
            urlSb.append(ServletMethodExecutor.ARQUILLIAN_SERVLET_MAPPING);
            String qs = request.getQueryString();
            if (qs != null) {
                urlSb.append("?");
                urlSb.append(qs);
            }
        }
        return urlSb;
    }

}
