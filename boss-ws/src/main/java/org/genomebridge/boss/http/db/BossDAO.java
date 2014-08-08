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

import org.genomebridge.boss.http.resources.*;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper({ GroupResourceMapper.class, ObjectResourceMapper.class,
        FsGroupResourceMapper.class, FsObjectResourceMapper.class})
public interface BossDAO {

    @SqlQuery("select * from objects where objectId = :objectId and groupId = :group")
    public FsObjectResource findFsObjectById(@Bind("objectId") String objectId, @Bind("group") String group);

    @SqlQuery("select * from groups where groupId = :groupId")
    public FsGroupResource findFsGroupById(@Bind("groupId") String groupId);

    @SqlQuery("select * from objects where objectId = :objectId and groupId = :group")
    public ObjectResource findObjectById(@Bind("objectId") String objectId, @Bind("group") String group);

    @SqlQuery("select location from objects where objectId = :objectId and groupId = :groupId")
    public String findObjectLocation(@Bind("objectId") String objectId, @Bind("groupId") String groupId);

    @SqlQuery("select * from groups where groupId = :groupId")
    public GroupResource findGroupById(@Bind("groupId") String groupId);

    @SqlUpdate("insert into groups (groupId, ownerId, sizeEstimateBytes, typeHint, location) values " +
            "(:groupId, :ownerId, :sizeEstimate, :typeHint, :location)")
    public void insertGroup(@Bind("groupId") String groupId,
                            @Bind("ownerId") String ownerId,
                            @Bind("sizeEstimate") Long sizeEstimate,
                            @Bind("typeHint") String typeHint,
                            @Bind("location") String location);

    @SqlUpdate("insert into objects (objectId, groupId, ownerId, name, sizeEstimateBytes, location) values " +
            "(:objectId, :group, :ownerId, :name, :sizeEstimate, :location)")
    public void insertObject(@Bind("objectId") String objectId,
                             @Bind("group") String group,
                             @Bind("ownerId") String ownerId,
                             @Bind("sizeEstimate") Long sizeEstimate,
                             @Bind("name") String name,
                             @Bind("location") String location);

    @SqlUpdate("update groups set ownerId = :ownerId, sizeEstimateBytes = :sizeEstimate, typeHint = :typeHint " +
            "where groupId = :groupId")
    public void updateGroup(@Bind("groupId") String groupId,
                            @Bind("ownerId") String ownerId,
                            @Bind("sizeEstimate") Long sizeEstimate,
                            @Bind("typeHint") String typeHint);

    @SqlUpdate("update objects set ownerId = :ownerId, sizeEstimateBytes = :sizeEstimate, name = :name " +
            "where objectId = :objectId and groupId = :group")
    public void updateObject(@Bind("objectId") String objectId,
                             @Bind("group") String group,
                             @Bind("ownerId") String ownerId,
                             @Bind("sizeEstimate") Long sizeEstimate,
                             @Bind("name") String name);

    @SqlUpdate("update groups set active=false where groupId = :groupId")
    public void deleteGroup(@Bind("groupId") String groupId);

    @SqlUpdate("update objects set active=false where objectId = :objectId")
    public void deleteObject(@Bind("objectId") String objectId, @Bind("groupId") String groupId);


    @SqlQuery("select distinct(username) from readers where id = :id")
    public List<String> findReadersById(@Bind("id") String id);

    @SqlBatch("insert into readers (id, username) values (:id, :username)")
    public void insertReaders( @Bind("id") String id, @Bind("username") List<String> readers );

    @SqlBatch("delete from readers where id = :id and username = :username")
    public void deleteReaders( @Bind("id") String id, @Bind("username") List<String> readers );

    @SqlQuery("select distinct(username) from writers where id = :id")
    public List<String> findWritersById(@Bind("id") String id);

    @SqlBatch("insert into writers (id, username) values (:id, :username)")
    public void insertWriters( @Bind("id") String id, @Bind("username") List<String> readers );

    @SqlBatch("delete from writers where id = :id and username = :username")
    public void deleteWriters( @Bind("id") String id, @Bind("username") List<String> readers );


}
