package org.genomebridge.boss.http.models;

/**
 * Created by davidan on 9/3/14.
 */
public enum StoragePlatform {
    OBJECTSTORE ("objectstore"),
    FILESYSTEM ("filesystem");

    private final String value;
    public String getValue() {return value;}

    StoragePlatform(String value) {
        this.value = value;
    }


}
