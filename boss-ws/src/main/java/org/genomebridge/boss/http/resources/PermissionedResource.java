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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Arrays;

abstract public class PermissionedResource extends AbstractResource {

    public void checkUser( String user, String accessType, String[] usernameArray ) {
        if(user == null || usernameArray == null || Arrays.asList(usernameArray).indexOf(user) == -1) {
            String aclString = usernameArray == null ?
                    String.valueOf(usernameArray) : Arrays.asList(usernameArray).toString();
            String msg =  String.format("User \"%s\" is not allowed %s access to resource with ACL %s", user, accessType, aclString);
            logger().info(msg);
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                    .entity(msg)
                    .build());
        }
    }

    public void checkUserRead( HttpHeaders headers ) {
        checkUserRead(headers.getRequestHeaders().getFirst("REMOTE_USER"));
    }
    public void checkUserWrite( HttpHeaders headers ) {
        checkUserWrite(headers.getRequestHeaders().getFirst("REMOTE_USER"));
    }

    public abstract void checkUserRead( String user );
    public abstract void checkUserWrite( String user );
}
