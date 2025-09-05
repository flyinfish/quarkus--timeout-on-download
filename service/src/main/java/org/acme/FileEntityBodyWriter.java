package org.acme;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.WILDCARD)
public class FileEntityBodyWriter implements jakarta.ws.rs.ext.MessageBodyWriter<FileEntity> {
    @Override
    public boolean isWriteable(Class<?> type, java.lang.reflect.Type genericType, java.lang.annotation.Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType) {
        return FileEntity.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(FileEntity fileEntity, Class<?> type, java.lang.reflect.Type genericType, java.lang.annotation.Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType, jakarta.ws.rs.core.MultivaluedMap<String, Object> httpHeaders, java.io.OutputStream entityStream) throws java.io.IOException, jakarta.ws.rs.WebApplicationException {
        if (fileEntity != null && fileEntity.content() != null) {
            httpHeaders.putSingle("Content-Type", fileEntity.type().toString());
            httpHeaders.putSingle(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''%s".formatted(fileEntity.name()));
            entityStream.write(fileEntity.content());
        } else {
            throw new jakarta.ws.rs.NotFoundException();
        }
    }

}
