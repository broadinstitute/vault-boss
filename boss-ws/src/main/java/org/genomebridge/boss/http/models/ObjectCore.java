package org.genomebridge.boss.http.models;

public class ObjectCore {

    public void copy( ObjectCore that ) {
        this.objectId = that.objectId;
        this.objectName = that.objectName;
        this.storagePlatform = that.storagePlatform;
        this.directoryPath = that.directoryPath;
        this.sizeEstimateBytes = that.sizeEstimateBytes;
        this.ownerId = that.ownerId;
    }

    public String objectId;
    public String objectName;
    public String storagePlatform;
    public String directoryPath;
    public Long sizeEstimateBytes;
    public String ownerId;
}
