package org.genomebridge.boss.http.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;


@ApiModel(value = "An Object in a Storage Platform")
public class ObjectCore {

    public void copy( ObjectCore that ) {
        this.objectId = that.objectId;
        this.objectName = that.objectName;
        this.storagePlatform = that.storagePlatform;
        this.directoryPath = that.directoryPath;
        this.sizeEstimateBytes = that.sizeEstimateBytes;
        this.ownerId = that.ownerId;
    }

    @ApiModelProperty(value = "The Boss ID for this Object.")
    public String objectId;
    @ApiModelProperty(value = "The Name for this Object.")
    public String objectName;
    @ApiModelProperty(value = "The Storage Platform for this Object.")
    public String storagePlatform;
    @ApiModelProperty(value = "The URI for this Object.")
    public String directoryPath;
    @ApiModelProperty(value = "The estimated size for this Object.")
    public Long sizeEstimateBytes;
    @ApiModelProperty(value = "The owner for this Object.")
    public String ownerId;
}
