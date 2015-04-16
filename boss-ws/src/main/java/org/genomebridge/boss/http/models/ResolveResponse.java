package org.genomebridge.boss.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.net.URI;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("Resolve Response")
public class ResolveResponse {

    @ApiModelProperty(value="ObjectUrl is a pre-signed url generated to access the object for the desired operation.",dataType = "URI")
    public URI objectUrl;
    @ApiModelProperty(value = "Validity period seconds.",dataType = "Integer")
    public Integer validityPeriodSeconds;
    @ApiModelProperty(value = "Content Type.",dataType = "String")
    public String contentType;
    @ApiModelProperty(value = "Content MD5.",dataType = "String")
    public String contentMD5Hex;

}
