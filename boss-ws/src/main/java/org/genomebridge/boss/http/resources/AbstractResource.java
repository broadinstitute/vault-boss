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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Arrays;

public abstract class AbstractResource {

    public static <T> T setFrom(T original, T newValue) {
        if(newValue != null) {
            return newValue;
        } else {
            return original;
        }
    }

    private static RuntimeException generateException(String fieldName, String expected, String given) {
        return new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                .entity(String.format("%s was different than previously set. Expected: %s; given: %s",
                        fieldName,
                        expected,
                        given))
                .build());
    }

    /**
     * Throw a WebApplicationException (Status: BAD_REQUEST) if a particular value is
     * set to something other than the original value.
     *
     * @param original  The original value
     * @param newValue  The new value
     * @param fieldName The name of the field that is being tested
     * @param <T> The type of the values.
     *
     * @return The original value.
     */
    public static <T> T errorIfSet(T original, T newValue, String fieldName) {
        if(original == null) {
            if (newValue != null) {
                throw generateException(fieldName, "null", newValue.toString());
            }
        } else {
            if(newValue != null && !original.equals(newValue)) {
                throw generateException(fieldName, original.toString(), newValue.toString());
            }
        }

        return original;
    }

    public static <T> boolean eq(T mine, T theirs) {
        if(mine == null) {
            return theirs == null;
        } else {
            return mine.equals(theirs);
        }
    }

    public static <T> boolean arrayEq(T[] mine, T[] theirs) {
        if(mine == null) {
            return theirs == null;
        } else {
            return Arrays.equals(mine, theirs);
        }
    }

    abstract public Logger logger();

}
