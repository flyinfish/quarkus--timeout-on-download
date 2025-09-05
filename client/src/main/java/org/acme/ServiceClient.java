package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient(configKey = "service-api")
interface ServiceClient {
    @GET
    @Path("files/{id}")
    @Produces(MediaType.WILDCARD)
    FileEntity download(@PathParam("id") String name);
}