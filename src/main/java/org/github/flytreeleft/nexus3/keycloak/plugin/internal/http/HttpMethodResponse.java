package org.github.flytreeleft.nexus3.keycloak.plugin.internal.http;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class HttpMethodResponse<R> {
    private final HttpMethod<R> method;

    public HttpMethodResponse(HttpMethod<R> method) {
        this.method = method;
    }

    public R execute() {
        return this.method.execute((InputStream inputStream) -> null);
    }

    public HttpMethodResponse<R> json(final Class<R> responseType) {
        return json(new TypeReference<R>() {
            @Override
            public Type getType() {
                return responseType;
            }
        });
    }

    public HttpMethodResponse<R> json(final TypeReference<R> responseType) {
        return new HttpMethodResponse<R>(this.method) {
            @Override
            public R execute() {
                return method.execute((InputStream inputStream) -> {
                    try {
                        JsonSerialization.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                        JsonSerialization.mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
                        return JsonSerialization.readValue(inputStream, responseType);
                    } catch (IOException e) {
                        throw new RuntimeException("Error parsing JSON response.", e);
                    }
                });
            }
        };
    }
}
