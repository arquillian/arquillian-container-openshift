package org.jboss.arquillian.container.openshift.express;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;

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