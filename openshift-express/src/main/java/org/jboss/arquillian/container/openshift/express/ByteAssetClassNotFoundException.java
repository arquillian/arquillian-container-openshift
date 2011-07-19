package org.jboss.arquillian.container.openshift.express;

public class ByteAssetClassNotFoundException extends ClassNotFoundException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ByteAssetClassNotFoundException(String message) {
        super(message);
    }

    public ByteAssetClassNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
