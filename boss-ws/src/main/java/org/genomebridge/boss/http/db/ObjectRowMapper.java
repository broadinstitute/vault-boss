/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
