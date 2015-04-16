package org.genomebridge.boss.http.models;


import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "ObjectDesc")
public class ObjectDesc extends ObjectCore {

    public Boolean forceLocation;
    @ApiModelProperty(value = "The users with read access to this Object.",dataType = "String[]" )
    public String[] readers;
    @ApiModelProperty(value = "The users with write access to this Object.",dataType = "String[]" )
    public String[] writers;

}
