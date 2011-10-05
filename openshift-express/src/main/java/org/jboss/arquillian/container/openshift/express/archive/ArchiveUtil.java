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
package org.jboss.arquillian.container.openshift.express.archive;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * An utility to provide classpath scanning based on ShrinkWrap archives.
 *
 * It is used to determine servlet implementation in the archive. Supports JAR, WAR and EAR archives.
 *
 * <p>
 * For class path scanning, following formula is used
 * </p>
 * <ul>
 * <li>EAR - scan all JARs and then WARs</li>
 * <li>WAR - scan all classes in the WEB-INF/classes and then JARs</li>
 * <li>JAR - scan all classes</li>
 * </ul>
 *
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="http://community.jboss.org/people/jharting">Jozef Hartinger</a>
 *
 */
public class ArchiveUtil {
    private static final Logger log = Logger.getLogger(ArchiveUtil.class.getName());

    /**
     * Checks if archive is of type JAR
     *
     * @param archive the archive
     * @return {@code true} if archive is JAR, {@code false} otherwise
     */
    public static final boolean isJarArchive(Archive<?> archive) {
        return archive instanceof JavaArchive;
    }

    /**
     * Checks if archive is of type WAR
     *
     * @param archive the archive
     * @return {@code true} if archive is WAR, {@code false} otherwise
     */
    public static final boolean isWarArchive(Archive<?> archive) {
        return archive instanceof WebArchive;
    }

    /**
     * Checks if archive is of type EAR
     *
     * @param archive the archive
     * @return {@code true} if archive is EAR, {@code false} otherwise
     */
    public static final boolean isEarArchive(Archive<?> archive) {
        return archive instanceof EnterpriseArchive;
    }

    /**
     * Gets all classes from the archive which implement interface or are the subclass of given needle
     *
     * @param <T> Type of objects to be found
     * @param archive Archive to be searched
     * @param needle Class representing superclass of searched objects
     * @return Unique collection of classes in the archive
     */
    public static final <T> Collection<Class<T>> getDefinedClassesOf(Archive<?> archive, Class<T> needle) {

        long beforeScanning = System.currentTimeMillis();

        Collection<String> classNames = new LinkedHashSet<String>();
        Collection<Class<T>> needleImpls = new LinkedHashSet<Class<T>>();
        getDefinedClasses(classNames, needleImpls, archive, needle);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Found " + needleImpls + " defined in the archive " + archive.getName() + " matching needle "
                    + needle.getName());
            log.fine("Scanning classpath took " + (System.currentTimeMillis() - beforeScanning) + "ms");
        }

        return needleImpls;
    }

    /**
     * Gets all fully qualified names of classes in the archive
     *
     * @param archive Archive to be searched
     * @return Unique collection of class names in the archive
     */
    public static final Collection<String> getDefinedClasses(Archive<?> archive) {

        Collection<String> classNames = new LinkedHashSet<String>();
        getDefinedClasses(classNames, null, archive, null);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Found " + classNames + " defined in the archive " + archive.getName());
        }

        return classNames;
    }

    // recursive descent
    private static final <T> void getDefinedClasses(Collection<String> classNames, Collection<Class<T>> needleImpls,
            Archive<?> archive, Class<T> needle) {

        if (isEarArchive(archive)) {
            for (JavaArchive jar : getScannableNestedArchives(archive, JavaArchive.class, AssetUtil.JAR_FILTER)) {
                getDefinedClasses(classNames, needleImpls, jar, needle);
            }
            for (WebArchive war : getScannableNestedArchives(archive, WebArchive.class, AssetUtil.WAR_FILTER)) {
                getDefinedClasses(classNames, needleImpls, war, needle);
            }
        } else if (isWarArchive(archive)) {

            ByteAssetClassLoader cl = new ByteAssetClassLoader(archive, ArchiveType.WAR);

            for (Entry<ArchivePath, Node> node : archive.getContent(AssetUtil.CLASS_FILTER).entrySet()) {
                getDefinedClasses(classNames, needleImpls, ArchiveType.WAR, node.getKey(), node.getValue(), needle, cl);
            }
            for (JavaArchive jar : getScannableNestedArchives(archive, JavaArchive.class, AssetUtil.JAR_FILTER)) {
                getDefinedClasses(classNames, needleImpls, jar, needle);
            }
        } else if (isJarArchive(archive)) {

            ByteAssetClassLoader cl = new ByteAssetClassLoader(archive, ArchiveType.JAR);

            for (Entry<ArchivePath, Node> node : archive.getContent(AssetUtil.CLASS_FILTER).entrySet()) {
                getDefinedClasses(classNames, needleImpls, ArchiveType.JAR, node.getKey(), node.getValue(), needle, cl);
            }
        }

    }
    
    /**
     * Does the same as {@link Archive#getAsType(Class, Filter)} but filters out nodes which cannot be scanned for classes
     * (cannot be converted to <X>)
     */
    private static <X extends Archive<X>> Collection<X> getScannableNestedArchives(Archive<?> archive, Class<X> type,
            Filter<ArchivePath> filter) {
        Collection<X> nestedArchives = new HashSet<X>();
        for (Map.Entry<ArchivePath, Node> entry : archive.getContent(filter).entrySet()) {
            try {
                X nestedArchive = archive.getAsType(type, entry.getKey());
                nestedArchives.add(nestedArchive);
            } catch (IllegalArgumentException ignored) {
                // ignored, we are not able to convert this type to X so we won't need it anyway
            }
        }
        return nestedArchives;
    }

    // worker method
    @SuppressWarnings("unchecked")
    private static final <T> void getDefinedClasses(Collection<String> classNames, Collection<Class<T>> needleImpls,
            ArchiveType type, ArchivePath key, Node value, Class<T> needle, ClassLoader classLoader) {

        String name = type.asClassName(key);

        if (log.isLoggable(Level.FINER)) {
            log.finer("Processing path " + key + " for class: " + name);
        }

        // construct class names
        if (needle == null) {
            classNames.add(name);
        }
        // load classes and check their type
        else {
            // load class
            try {
                Class<?> clazz = Class.forName(name, false, classLoader);
                if (needle.isAssignableFrom(clazz)) {
                    needleImpls.add((Class<T>) clazz);

                    if (log.isLoggable(Level.FINE)) {
                        log.fine("Class " + clazz.getName() + " passed check for " + needle.getName() + " implementation");
                    }
                }
            } catch (ByteAssetClassNotFoundException e) {
                throw new IllegalStateException("Unable to load class using ByteAssetClassLoader", e);
            } catch (ClassNotFoundException e) {
                log.warning("Unable to load class using ByteAssetClassLoader: " + e.getCause());
            } catch (NoClassDefFoundError e) {
                log.warning("Unable to load class using ByteAssetClassLoader: " + e.getCause());
            }
        }

    }

}
