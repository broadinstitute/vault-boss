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

import org.genomebridge.boss.http.resources.FsObjectResource;
import org.genomebridge.boss.http.service.DeregisteredObjectException;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FsObjectResourceMapper implements ResultSetMapper<FsObjectResource> {
    public FsObjectResource map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        FsObjectResource rec = new FsObjectResource();

        rec.objectId = r.getString("objectId");
        if(!r.getBoolean("active")) { throw new DeregisteredObjectException("fs-" + rec.group + "/" + rec.objectId); }

        rec.group = r.getString("groupId");
        rec.ownerId = r.getString("ownerId");
        rec.sizeEstimateBytes = r.getLong("sizeEstimateBytes");
        rec.name = r.getString("name");

        return rec;
    }
}
