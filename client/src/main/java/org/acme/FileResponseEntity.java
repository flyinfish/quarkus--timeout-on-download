package org.acme;

public record FileResponseEntity(String name, String type, int receivedBytes, String clientFailure) {
    public static FileResponseEntity from(FileEntity receivedEntity) {
        return new FileResponseEntity(receivedEntity.name(), receivedEntity.type().toString(), receivedEntity.content().length, null);
    }

    public static FileResponseEntity from(String name, Exception e) {
        return new FileResponseEntity(name, null, 0, e.toString());
    }
}