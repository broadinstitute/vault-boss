/*
 * Copyright 2015 Broad Institute
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
