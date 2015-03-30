package org.genomebridge.boss.http.db;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ObjectRowMapper implements ResultSetMapper<ObjectRow> {
    public ObjectRow map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        ObjectRow rec = new ObjectRow();

        rec.objectId = r.getString("objectId");
        rec.objectName = r.getString("objectName");
        rec.ownerId = r.getString("ownerId");
        rec.sizeEstimateBytes = r.getLong("sizeEstimateBytes");
        rec.storagePlatform = r.getString("storagePlatform");
        rec.directoryPath = r.getString("location");
        rec.active = r.getString("active");
        rec.createdBy = r.getString("createdBy");
        rec.createDate = r.getTimestamp("createDate");
        rec.modifyDate = r.getTimestamp("modifyDate");
        rec.resolveDate = r.getTimestamp("resolveDate");
        rec.deleteDate = r.getTimestamp("deleteDate");

        return rec;
    }
}
