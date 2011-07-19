package org.jboss.arquillian.container.openshift.express;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;

/**
 * Simple implementation of a classloader from ShrinkWrap archive
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
class ByteAssetClassLoader extends ClassLoader {

    private static final int ASSET_SIZE_LIMIT = 64 * 1024;

    private Archive<?> archive;

    private ArchiveType type;

    ByteAssetClassLoader(Archive<?> archive, ArchiveType type) {
        // set tctcl
        super(Thread.currentThread().getContextClassLoader());
        this.archive = archive;
        this.type = type;
    }

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

        byte[] bytes = new byte[ASSET_SIZE_LIMIT];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        is.close();

        return Arrays.copyOf(bytes, offset);
    }

}
