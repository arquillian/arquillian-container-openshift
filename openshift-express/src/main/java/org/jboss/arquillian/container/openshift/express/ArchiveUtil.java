package org.jboss.arquillian.container.openshift.express;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

class ArchiveUtil {
    private static final Logger log = Logger.getLogger(ArchiveUtil.class.getName());

    static final <T> Collection<Class<T>> getDefinedClassesOf(Archive<?> archive, Class<T> needle) {

        Collection<String> classNames = new LinkedHashSet<String>();
        Collection<Class<T>> needleImpls = new LinkedHashSet<Class<T>>();
        getDefinedClasses(classNames, needleImpls, archive, needle);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Found " + needleImpls + " defined in the archive " + archive.getName() + " matching needle "
                    + needle.getName());
        }

        return needleImpls;
    }

    static final Collection<String> getDefinedClasses(Archive<?> archive) {

        Collection<String> classNames = new LinkedHashSet<String>();
        getDefinedClasses(classNames, null, archive, null);

        if (log.isLoggable(Level.FINE)) {
            log.info("Found " + classNames + " defined in the archive " + archive.getName());
        }

        return classNames;
    }

    private static final <T> void getDefinedClasses(Collection<String> classNames, Collection<Class<T>> needleImpls,
            Archive<?> archive, Class<T> needle) {

        if (isEarArchive(archive)) {
            for (JavaArchive jar : archive.getAsType(JavaArchive.class, AssetUtil.JAR_FILTER)) {
                getDefinedClasses(classNames, needleImpls, jar, needle);
            }
            for (WebArchive war : archive.getAsType(WebArchive.class, AssetUtil.WAR_FILTER)) {
                getDefinedClasses(classNames, needleImpls, war, needle);
            }
        } else if (isWarArchive(archive)) {

            ByteAssetClassLoader cl = new ByteAssetClassLoader(archive, ArchiveType.WAR);

            for (Entry<ArchivePath, Node> node : archive.getContent(AssetUtil.CLASS_FILTER).entrySet()) {
                getDefinedClasses(classNames, needleImpls, ArchiveType.WAR, node.getKey(), node.getValue(), needle, cl);
            }
            for (JavaArchive jar : archive.getAsType(JavaArchive.class, AssetUtil.JAR_FILTER)) {
                getDefinedClasses(classNames, needleImpls, jar, needle);
            }
        } else if (isJarArchive(archive)) {

            ByteAssetClassLoader cl = new ByteAssetClassLoader(archive, ArchiveType.JAR);

            for (Entry<ArchivePath, Node> node : archive.getContent(AssetUtil.CLASS_FILTER).entrySet()) {
                getDefinedClasses(classNames, needleImpls, ArchiveType.JAR, node.getKey(), node.getValue(), needle, cl);
            }
        }

    }

    @SuppressWarnings("unchecked")
    private static final <T> void getDefinedClasses(Collection<String> classNames, Collection<Class<T>> needleImpls,
            ArchiveType type, ArchivePath key, Node value, Class<T> needle, ClassLoader classLoader) {

        String name = type.asClassName(key);
        classNames.add(name);

        if (log.isLoggable(Level.FINER)) {
            log.finer("Processed path " + key + " for class: " + name);
        }

        // stop evaluating
        if (needle == null) {
            return;
        }

        // load class
        try {
            Class<?> clazz = Class.forName(name, false, classLoader);
            if (needle.isAssignableFrom(clazz)) {
                needleImpls.add((Class<T>) clazz);
                log.info("Class " + clazz.getName() + " passed check for " + needle.getName() + " implementation");
            }
        } catch (ByteAssetClassNotFoundException e) {
            throw new IllegalStateException("Unable to load class using ByteAssetClassLoader", e);
        } catch (ClassNotFoundException e) {
            log.warning("Unable to load class using ByteAssetClassLoader: " + e.getCause());
        } catch (NoClassDefFoundError e) {
            log.warning("Unable to load class using ByteAssetClassLoader: " + e.getCause());
        }

    }

    static final boolean isJarArchive(Archive<?> archive) {
        return archive instanceof JavaArchive;
    }

    static final boolean isWarArchive(Archive<?> archive) {
        return archive instanceof WebArchive;
    }

    static final boolean isEarArchive(Archive<?> archive) {
        return archive instanceof EnterpriseArchive;
    }
}
