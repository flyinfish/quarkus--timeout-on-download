package org.acme;

public record TriggerParams(int requests, int minDelayMs, int maxDelayMs, int usingConcurrencyOf) {
    
}
