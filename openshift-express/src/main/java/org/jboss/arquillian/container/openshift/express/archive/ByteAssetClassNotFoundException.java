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

/**
 * A special exception to signalize that {@link ClassNotFoundException} was thrown due to problem in archive processing
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class ByteAssetClassNotFoundException extends ClassNotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     *
     * @param message the message
     */
    public ByteAssetClassNotFoundException(String message) {
        super(message);
    }

    /**
     *
     * @param message the message
     * @param cause the cause
     */
    public ByteAssetClassNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
