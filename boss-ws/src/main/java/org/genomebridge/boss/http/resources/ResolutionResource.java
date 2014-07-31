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
package org.genomebridge.boss.http.resources;

import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URL;

public class ResolutionResource extends AbstractResource {

    public URI url;
    public URI objectUrl;
    public Integer validityPeriodSeconds;

    public ResolutionResource() {}

    public ResolutionResource(URI url, URI objectUrl, Integer validityPeriodSeconds) {
        this.url = url;
        this.objectUrl = objectUrl;
        this.validityPeriodSeconds = validityPeriodSeconds;
    }

    public Logger logger() { return Logger.getLogger(ResolutionResource.class); }
}
