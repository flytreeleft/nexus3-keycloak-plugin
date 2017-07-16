package org.github.flytreeleft.nexus3.keycloak.plugin.internal.http;

public class HttpResponseException extends RuntimeException {
    private final int statusCode;
    private final String reasonPhrase;

    public HttpResponseException(String message, int statusCode, String reasonPhrase) {
        super(message);
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getReasonPhrase() {
        return this.reasonPhrase;
    }
}
