package org.jboss.arquillian.container.openshift.express;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;

class AssetUtil {

    static final Filter<ArchivePath> EAR_FILTER = new Filter<ArchivePath>() {

        @Override
        public boolean include(ArchivePath object) {
            return object.get().toLowerCase().endsWith(".ear");
        }
    };

    static final Filter<ArchivePath> WAR_FILTER = new Filter<ArchivePath>() {

        @Override
        public boolean include(ArchivePath object) {
            return object.get().toLowerCase().endsWith(".war");
        }
    };

    static final Filter<ArchivePath> JAR_FILTER = new Filter<ArchivePath>() {

        @Override
        public boolean include(ArchivePath object) {
            return object.get().toLowerCase().endsWith(".jar");
        }
    };

    static final Filter<ArchivePath> CLASS_FILTER = new Filter<ArchivePath>() {

        @Override
        public boolean include(ArchivePath object) {
            return object.get().toLowerCase().endsWith(".class");
        }

    };
}
