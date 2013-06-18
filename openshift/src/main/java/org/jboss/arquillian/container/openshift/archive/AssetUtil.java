/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.arquillian.container.openshift.archive;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;

/**
 * An utility providing basic Archive filtering
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class AssetUtil {

    /**
     * Filter for getting EAR archives from an archive
     */
    public static final Filter<ArchivePath> EAR_FILTER = new Filter<ArchivePath>() {

        @Override
        public boolean include(ArchivePath object) {
            return object.get().toLowerCase().endsWith(".ear");
        }
    };

    /**
     * Filter for getting WAR archives from an archive
     */
    public static final Filter<ArchivePath> WAR_FILTER = new Filter<ArchivePath>() {

        @Override
        public boolean include(ArchivePath object) {
            return object.get().toLowerCase().endsWith(".war");
        }
    };

    /**
     * Filter for getting JAR archives from an archive
     */
    public static final Filter<ArchivePath> JAR_FILTER = new Filter<ArchivePath>() {

        @Override
        public boolean include(ArchivePath object) {
            return object.get().toLowerCase().endsWith(".jar");
        }
    };

    /**
     * Filter for getting compiled classes from an archive
     */
    static final Filter<ArchivePath> CLASS_FILTER = new Filter<ArchivePath>() {

        @Override
        public boolean include(ArchivePath object) {
            return object.get().toLowerCase().endsWith(".class");
        }

    };
}
