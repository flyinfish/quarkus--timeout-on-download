package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient(configKey = "client-api")
interface TriggerClient {
    @GET
    @Path("files/{id}")
    @Produces(MediaType.WILDCARD)
    ActualFile download(@PathParam("id") String name);
}