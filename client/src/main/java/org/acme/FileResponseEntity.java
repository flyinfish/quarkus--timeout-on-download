package org.acme;

public record FileResponseEntity(String name, String type, int receivedBytes ) {
    public static FileResponseEntity from(FileEntity receivedEntity) {
        return new FileResponseEntity(receivedEntity.name(), receivedEntity.type().toString(), receivedEntity.content().length );
    }
}