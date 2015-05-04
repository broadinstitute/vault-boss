package org.genomebridge.boss.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.net.URI;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("Resolve Response")
public class ResolveResponse {

    @ApiModelProperty(value="ObjectUrl is a pre-signed url generated to access the object for the desired operation.")
    public URI objectUrl;
    @ApiModelProperty(value = "Validity period seconds.")
    public Integer validityPeriodSeconds;
    @ApiModelProperty(value = "Content Type.")
    public String contentType;
    @ApiModelProperty(value = "Content MD5.")
    public String contentMD5Hex;

}
