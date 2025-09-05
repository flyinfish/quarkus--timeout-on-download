package org.acme;

import jakarta.ws.rs.core.MediaType;

public record FileEntity(String name, MediaType type, byte[] content) {
}