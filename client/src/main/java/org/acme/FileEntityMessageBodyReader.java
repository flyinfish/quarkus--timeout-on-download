package org.acme;

import jakarta.ws.rs.ext.Provider;

@Provider
public class FileEntityMessageBodyReader implements jakarta.ws.rs.ext.MessageBodyReader<FileEntity>    {
    @Override
    public boolean isReadable(Class<?> type, java.lang.reflect.Type genericType, java.lang.annotation.Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType) {
        return FileEntity.class.equals(type);
    }

    @Override
    public FileEntity readFrom(Class<FileEntity> type, java.lang.reflect.Type genericType, java.lang.annotation.Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType, jakarta.ws.rs.core.MultivaluedMap<String, String> httpHeaders, java.io.InputStream entityStream) throws java.io.IOException, jakarta.ws.rs.WebApplicationException {
        var contentDisposition = httpHeaders.getFirst("content-disposition");
        var name = contentDisposition.split("''")[1];
        var content = entityStream.readAllBytes();
        return new FileEntity(name, mediaType, content);
    }
    
}
