package org.genomebridge.boss.http.db;

import java.sql.Timestamp;

import org.genomebridge.boss.http.models.ObjectCore;

public class ObjectRow extends ObjectCore {
    public String active;
    public String createdBy;
    public Timestamp createDate;
    public Timestamp modifyDate;
    public Timestamp resolveDate;
    public Timestamp deleteDate;
}
