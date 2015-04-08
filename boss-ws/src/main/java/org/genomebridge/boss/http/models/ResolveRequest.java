package org.genomebridge.boss.http.models;


import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("Resolve Request")
public class ResolveRequest {

    @ApiModelProperty(value = "The number of seconds that this request will be valid for. This value must be positive.",dataType = "Integer")
    public Integer validityPeriodSeconds;
    @ApiModelProperty(value="The 'http' method to use for accessing this object. Must be one of the following: GET, PUT, HEAD.",dataType = "String")
    public String httpMethod;
    @ApiModelProperty(value = "The content type value. Must be a valid mime type",dataType = "String")
    public String contentType;
    @ApiModelProperty(value = "The content MD5. Must be a valid 32 character hexadecimal string.",dataType = "String")
    public String contentMD5Hex;

}
