/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.protocol.proxied_servlet;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.command.Command;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;
import org.jboss.arquillian.protocol.proxied_servlet.proxy.RemoteProxyServlet;
import org.jboss.arquillian.protocol.servlet.ServletMethodExecutor;
import org.jboss.arquillian.protocol.servlet.ServletProtocolConfiguration;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;

/**
 * ServletMethodExecutor
 *
 * @author <a href="mailto:mlazar@redhat.com">Matej Lazar</a>
 * @version $Revision: $
 */
//TODO extends ServletMethodExecutor, change super class methods from private to protected
public class ProxiedServletMethodExecutor extends ServletMethodExecutor implements ContainerMethodExecutor {

   private static final Logger log = Logger.getLogger(ProxiedServletMethodExecutor.class.getName());

   public ProxiedServletMethodExecutor(ServletProtocolConfiguration config, Collection<HTTPContext> contexts, final CommandCallback callback) {
       if(config == null)
       {
          throw new IllegalArgumentException("ServletProtocolConfiguration must be specified");
       }
       if (contexts == null || contexts.size() == 0)
       {
          throw new IllegalArgumentException("HTTPContext must be specified");
       }
       if (callback == null)
       {
          throw new IllegalArgumentException("Callback must be specified");
       }
       this.uriHandler = new ProxiedServletURIHandler(config, contexts);
       this.callback = callback;
   }

   @Override
   public TestResult invoke(final TestMethodExecutor testMethodExecutor)
   {
      if (testMethodExecutor == null)
      {
         throw new IllegalArgumentException("TestMethodExecutor must be specified");
      }

      Method method = testMethodExecutor.getMethod();

      String servletMapping = ARQUILLIAN_SERVLET_MAPPING;
      String internalHostParams = "";

      URI targetBaseURI;
      URI servletURI = uriHandler.locateTestServlet(method);

      URI arqProxy = ((ProxiedServletURIHandler)uriHandler).getArquillianProxyServlet(method, RemoteProxyServlet.SERVLET_NAME);

      if (arqProxy != null) {
          internalHostParams =
                  "&internalHost=" + servletURI.getHost() +
                  "&internalPort=" + servletURI.getPort();
          servletMapping = RemoteProxyServlet.SERVLET_MAPPING;
          targetBaseURI = arqProxy;
      } else {
          log.warning("Container does not provide a proxy cofiguration. Continue without a proxy.");
          targetBaseURI = servletURI;
      }

      Class<?> testClass = testMethodExecutor.getInstance().getClass();
      final String url = targetBaseURI.toASCIIString() + servletMapping
            + "?outputMode=serializedObject&className=" + testClass.getName() + "&methodName="
            + testMethodExecutor.getMethod().getName()
            + internalHostParams;

      final String eventUrl = targetBaseURI.toASCIIString() + servletMapping
            + "?outputMode=serializedObject&className=" + testClass.getName() + "&methodName="
            + testMethodExecutor.getMethod().getName() + "&cmd=event"
            + internalHostParams;

      log.info("Invocation url: " + url + ".");

      Timer eventTimer = null;
      try
      {
         eventTimer = new Timer();
         eventTimer.schedule(new TimerTask()
         {
            @Override
            public void run()
            {
               try
               {
                  Object o = execute(eventUrl, Object.class, null);
                  if (o != null)
                  {
                     if (o instanceof Command)
                     {
                        Command<?> command = (Command<?>) o;
                        callback.fired(command);
                        execute(eventUrl, Object.class, command);
                     }
                     else
                     {
                        throw new RuntimeException("Recived a non " + Command.class.getName()
                              + " object on event channel");
                     }
                  }
               }
               catch (Exception e)
               {
                  e.printStackTrace();
               }
            }
         }, 0, 100);

         return executeWithRetry(url, TestResult.class);
      }
      catch (Exception e)
      {
         throw new IllegalStateException("Error launching test " + testClass.getName() + " "
               + testMethodExecutor.getMethod(), e);
      }
      finally
      {
         if (eventTimer != null)
         {
            eventTimer.cancel();
         }
      }
   }


}