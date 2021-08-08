package org.github.flytreeleft.nexus3.keycloak.plugin.internal.http;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

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
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    mapper.setVisibility(VisibilityChecker.Std.defaultInstance()
                                                              .withFieldVisibility(JsonAutoDetect.Visibility.ANY));

                    try {
                        return mapper.readValue(inputStream, responseType);
                    } catch (IOException e) {
                        throw new RuntimeException("Error parsing JSON response for type " + responseType.getType(), e);
                    }
                });
            }
        };
    }
}
