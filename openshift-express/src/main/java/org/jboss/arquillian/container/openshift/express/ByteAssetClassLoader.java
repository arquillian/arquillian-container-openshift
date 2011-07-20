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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;

/**
 * Simple implementation of a class loader from ShrinkWrap archive
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
class ByteAssetClassLoader extends ClassLoader {

    private Archive<?> archive;

    private ArchiveType type;

    /**
     * Constructs a class loader with an archive as a pool
     *
     * @param archive the archive
     * @param type the type of archive
     */
    ByteAssetClassLoader(Archive<?> archive, ArchiveType type) {
        // set tctcl
        super(Thread.currentThread().getContextClassLoader());
        this.archive = archive;
        this.type = type;
    }

    /**
     * Overridden method signalizes a problem with archive class loading by throwing a ClassNotFoundException subclass.
     * ClassNotFoundExceptions are expected to be ignored
     *
     * @see {@link ByteAssetClassNotFoundException}
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        ArchivePath path = type.asArchivePath(name);

        if (!archive.contains(path)) {
            throw new ByteAssetClassNotFoundException("Unable to find class " + name + " in archive " + archive.getName());
        }

        Node node = archive.get(path);
        byte[] b;
        try {
            b = getBytesFromAsset(node.getAsset());
        } catch (IOException e) {
            throw new ByteAssetClassNotFoundException("Unable to load bytes for the Asset", e);
        }

        return defineClass(name, b, 0, b.length);
    }

    private byte[] getBytesFromAsset(Asset asset) throws IOException {
        InputStream is = asset.openStream();
        byte[] bytes = IOUtils.toByteArray(is);
        is.close();
        return bytes;
    }

}
