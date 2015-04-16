package org.genomebridge.boss.http.swagger;

import javax.validation.constraints.NotNull;


public class SwaggerConfiguration {
    
    @NotNull
    public String host;
    @NotNull
    public String apiVersion;
    @NotNull
    public String apiDocs;
    @NotNull
    public String baseUrl;
    @NotNull
    public String contact;
    @NotNull
    public String description;
    @NotNull
    public String title;
    @NotNull
    public String license;
    @NotNull
    public String licenseUrl;
    @NotNull
    public String termsOfServiceUrl;

}
