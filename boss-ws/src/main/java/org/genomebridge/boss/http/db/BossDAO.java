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
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.sql.Timestamp;
import java.util.List;

@RegisterMapper({ ObjectResourceMapper.class })
public interface BossDAO extends Transactional<BossDAO> {

    /*
    Object API
     */

    @SqlQuery("select * from objects where objectId = :objectId")
    public ObjectResource findObjectById(@Bind("objectId") String objectId);

    @SqlQuery("select location from objects where objectId = :objectId and active='Y'")
    public String findObjectLocation(@Bind("objectId") String objectId);

    @SqlQuery("select o.* from objects o inner join readers r on o.objectId = r.objectId " +
              "where o.objectName = :objectName and o.active='Y' and r.username = :username")
    public List<ObjectResource> findObjectsByName(@Bind("username") String username, @Bind("objectName") String objectName);

    @SqlUpdate("insert into objects " +
            "(objectId, objectName, ownerId, sizeEstimateBytes, location, storagePlatform, createdBy, active, createDate ) values " +
            "(:objectId, :objectName, :ownerId, :sizeEstimate, :location, :storagePlatform, :createdBy, 'Y', :now)")
    public void insertObject(@Bind("objectId") String objectId,
                             @Bind("objectName") String objectName,
                             @Bind("ownerId") String ownerId,
                             @Bind("sizeEstimate") Long sizeEstimate,
                             @Bind("location") String location,
                             @Bind("storagePlatform") String storagePlatform,
                             @Bind("createdBy") String createdBy,
                             @Bind("now") Timestamp now);

    @SqlUpdate("update objects set ownerId = :ownerId, sizeEstimateBytes = :sizeEstimate, " +
            "objectName = :objectName, modifyDate = :now where objectId = :objectId and active='Y'")
    public void updateObject(@Bind("objectId") String objectId,
                             @Bind("objectName") String objectName,
                             @Bind("ownerId") String ownerId,
                             @Bind("sizeEstimate") Long sizeEstimate,
                             @Bind("now") Timestamp now);

    @SqlUpdate("update objects set active='N', deleteDate = :now where objectId = :objectId")
    public void deleteObject(@Bind("objectId") String objectId, @Bind("now") Timestamp now);

    @SqlUpdate("update objects set resolveDate = :now where objectId = :objectId")
    public void updateResolveDate(@Bind("objectId") String objectId, @Bind("now") Timestamp now);

    /*
    Readers/Writers API
     */

    @SqlQuery("select distinct(username) from readers where objectId = :objectId")
    public List<String> findReadersById(@Bind("objectId") String objectId);

    @SqlBatch("insert into readers (objectId, username) values (:objectId, :username)")
    public void insertReaders( @Bind("objectId") String objectId, @Bind("username") List<String> readers );

    @SqlBatch("delete from readers where objectId = :objectId and username = :username")
    public void deleteReaders( @Bind("objectId") String objectId, @Bind("username") List<String> readers );

    @SqlQuery("select distinct(username) from writers where objectId = :objectId")
    public List<String> findWritersById(@Bind("objectId") String objectId);

    @SqlBatch("insert into writers (objectId, username) values (:objectId, :username)")
    public void insertWriters( @Bind("objectId") String objectId, @Bind("username") List<String> readers );

    @SqlBatch("delete from writers where objectId = :objectId and username = :username")
    public void deleteWriters( @Bind("objectId") String objectId, @Bind("username") List<String> readers );


}
