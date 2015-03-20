package org.genomebridge.boss.http.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.genomebridge.boss.http.service.BossAPI.ErrorDesc;

public abstract class AbstractResource {

    public static void throwWAE( ErrorDesc err )
    {
        throw new WebApplicationException(Response.status(err.mStatus)
                                                    .type(MediaType.TEXT_PLAIN)
                                                    .entity(err.mMessage)
                                                    .build());
    }
}
