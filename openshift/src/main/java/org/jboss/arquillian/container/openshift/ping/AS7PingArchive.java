/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.arquillian.container.openshift.ping;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * AS7PingArchive
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class AS7PingArchive
{
   public static String ARCHIVE_NAME = "arq-verify-deployed.war";

   public static Archive<?> create() {
      return ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME)
               .addPackage(AS7PingArchive.class.getPackage())
               .addAsResource(
                     new StringAsset(
                           "org.jboss.arquillian.container.openshift.ping.OpenShiftActivator"), 
                           "META-INF/services/org.jboss.msc.service.ServiceActivator")
               .setManifest(new StringAsset(
                    new StringBuilder()
                        .append("Dependencies: org.jboss.as.controller,org.jboss.as.server").append('\n')
                        .toString()));
   }
}
