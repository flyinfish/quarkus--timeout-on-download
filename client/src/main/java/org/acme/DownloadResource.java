package org.acme;

import java.io.IOException;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/files")
public class DownloadResource {
    @RestClient
    ServiceClient serviceClient;

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public FileResponseEntity getFileContent(@PathParam("name") String name) throws IOException {
        var receivedEntity = serviceClient.download(name);
        return FileResponseEntity.from(receivedEntity);
    }
}
