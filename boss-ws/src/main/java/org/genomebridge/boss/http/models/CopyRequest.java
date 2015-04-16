package org.genomebridge.boss.http.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;


@ApiModel("Copy Request")
public  class CopyRequest {

    @ApiModelProperty(value="Validity Period Seconds.",dataType = "Integer")
    public Integer validityPeriodSeconds;
    @ApiModelProperty(value="Location to copy.",dataType = "String")
    public String locationToCopy; // expecting something of the form "/bucket/key"

}
