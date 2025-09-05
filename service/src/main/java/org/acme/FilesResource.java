package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

@ApplicationScoped
@Path("/files")
public class FilesResource {
    @ConfigProperty(name = "files.dir", defaultValue = "../files")
    String filesDir;

    Properties filesRegistry;

    @PostConstruct
    void loadFilesRegistry() throws IOException {
        filesRegistry = new Properties();
        try (InputStream is = Files.newInputStream(java.nio.file.Path.of(filesDir, "files.properties"))) {
            filesRegistry.load(is);
        }
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.WILDCARD)
    public FileEntity getFileContent(@PathParam("name") String name) throws IOException {
        var typeAndLength = filesRegistry.getProperty(name);
        return typeAndLength == null                             
            ? null 
            : new FileEntity(name, MediaType.valueOf(typeAndLength.split(";")[0]), readFileContent(name));
    }

    byte[] readFileContent(String name) throws IOException {                
        return Files.exists(java.nio.file.Path.of(filesDir, name)) ? 
            Files.readAllBytes(java.nio.file.Path.of(filesDir, name)) : null;
    }
}
