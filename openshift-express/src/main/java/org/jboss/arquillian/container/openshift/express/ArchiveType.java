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
package org.jboss.arquillian.container.openshift.express;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;

/**
 * Representation of archive type with mapping between class and file names
 *
 * <p>
 * Implementation note: There is no EAR type, as classes cannot be stored directly in an EAR archive
 * </p>
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
enum ArchiveType {
    JAR {
        @Override
        public String asClassName(ArchivePath path) {
            String name = path.get();
            return name.replaceFirst("/", "").replaceAll("\\.class", "").replaceAll("/", "\\.");
        }

        @Override
        public ArchivePath asArchivePath(String className) {
            String slashedClassName = className.replaceAll("\\.", "/").concat(".class");
            return ArchivePaths.create("/WEB-INF/classes/", slashedClassName);
        }
    },
    WAR {
        @Override
        public String asClassName(ArchivePath path) {
            String name = path.get();
            return name.replaceFirst("/WEB-INF/classes/", "").replaceAll("\\.class", "").replaceAll("/", "\\.");
        }

        @Override
        public ArchivePath asArchivePath(String className) {
            String slashedClassName = className.replaceAll("\\.", "/").concat(".class");
            return ArchivePaths.create("/WEB-INF/classes/", slashedClassName);
        }
    };

    public abstract String asClassName(ArchivePath path);

    public abstract ArchivePath asArchivePath(String className);
}