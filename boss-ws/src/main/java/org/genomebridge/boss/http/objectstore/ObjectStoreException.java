package org.genomebridge.boss.http.objectstore;

/**
 * Created by davidan on 9/3/14.
 */
public class ObjectStoreException extends RuntimeException {

    public ObjectStoreException(Throwable cause) {
        super(cause);
    }

    public ObjectStoreException( String message ) {
        super(message);
    }

    public ObjectStoreException(String message, Throwable cause) {
        super(message, cause);
    }

}
