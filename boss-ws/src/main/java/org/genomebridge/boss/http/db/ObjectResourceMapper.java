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

import org.genomebridge.boss.http.resources.ObjectResource;
import org.genomebridge.boss.http.service.DeregisteredObjectException;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ObjectResourceMapper implements ResultSetMapper<ObjectResource> {
    public ObjectResource map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        ObjectResource rec = new ObjectResource();

        rec.objectId = r.getString("objectId");
        rec.objectName = r.getString("objectName");
        rec.ownerId = r.getString("ownerId");
        rec.sizeEstimateBytes = r.getLong("sizeEstimateBytes");
        rec.storagePlatform = r.getString("storagePlatform");

        return rec;
    }
}
