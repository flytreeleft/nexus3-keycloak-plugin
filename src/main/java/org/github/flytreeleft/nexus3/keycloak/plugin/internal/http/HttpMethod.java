package org.github.flytreeleft.nexus3.keycloak.plugin.internal.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpMethod<R> {
    private static final Logger logger = LoggerFactory.getLogger(HttpMethod.class);

    private final HttpClient httpClient;
    private final ClientAuthenticator authenticator;
    private final RequestBuilder builder;
    private final HashMap<String, String> params;

    public HttpMethod(HttpClient httpClient, ClientAuthenticator authenticator, RequestBuilder builder) {
        this(httpClient, authenticator, builder, new HashMap<>());
    }

    public HttpMethod(
            HttpClient httpClient, ClientAuthenticator authenticator, RequestBuilder builder,
            HashMap<String, String> params
    ) {
        this.httpClient = httpClient;
        this.authenticator = authenticator;
        this.builder = builder;
        this.params = params != null ? new HashMap<>(params) : new HashMap<>();
    }

    public void execute() {
        execute((InputStream inputStream) -> null);
    }

    public R execute(HttpResponseProcessor<R> responseProcessor) {
        HttpUriRequest request = null;
        InputStream inputStream = null;

        try {
            preExecute(this.builder);
            request = this.builder.build();

            HttpResponse response = this.httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                inputStream = entity.getContent();
            }

            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            logger.debug("{} - {}", request.getURI(), statusCode);
            if (statusCode == 404) {
                return null;
            } else if (statusCode < 200 || statusCode >= 300) {
                throw new HttpResponseException(String.format("Unexpected response for url %s: %d / %s",
                                                              request.getURI(),
                                                              statusCode,
                                                              statusLine.getReasonPhrase()),
                                                statusCode,
                                                statusLine.getReasonPhrase());
            }

            if (inputStream == null) {
                return null;
            } else {
                return responseProcessor.process(inputStream);
            }
        } catch (HttpResponseException e) {
            throw e;
        } catch (Exception e) {
            if (request != null) {
                logger.error("Error executing http method for url {}", request.getURI(), e);
            }
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected void preExecute(RequestBuilder builder) {
        for (Map.Entry<String, String> param : this.params.entrySet()) {
            builder.addParameter(param.getKey(), param.getValue());
        }
    }

    public HttpMethod<R> authorizationBearer(String bearer) {
        String prefix = "Bearer ";

        if (!bearer.startsWith(prefix)) {
            bearer = prefix + bearer;
        }
        header("Authorization", bearer);

        return this;
    }

    public HttpMethod<R> authorizationBasic(String basic) {
        String prefix = "Basic ";

        if (!basic.startsWith(prefix)) {
            basic = prefix + basic;
        }
        header("Authorization", basic);

        return this;
    }

    public HttpMethod<R> authorizationBasic(String username, String password) {
        StringBuilder sb = new StringBuilder(username);
        sb.append(':').append(password);

        try {
            String basic = Base64.getEncoder().encodeToString(sb.toString().getBytes("UTF-8"));
            authorizationBasic(basic);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public HttpMethodResponse<R> response() {
        return new HttpMethodResponse<R>(this);
    }

    public HttpMethod<R> authentication() {
        if (this.authenticator != null) {
            this.authenticator.configure(this);
        }
        return this;
    }

    public HttpMethod<R> param(String name, String value) {
        this.params.put(name, value);
        return this;
    }

    public HttpMethod<R> header(String name, String value) {
        this.builder.addHeader(name, value);
        return this;
    }

    public HttpMethod<R> json(byte[] entity) {
        header("Content-Type", "application/json");
        this.builder.setEntity(new ByteArrayEntity(entity));
        return this;
    }

    public HttpMethod<R> form() {
        return new HttpMethod<R>(this.httpClient, authenticator, this.builder, this.params) {
            @Override
            protected void preExecute(RequestBuilder builder) {
                List<NameValuePair> formParams = new ArrayList<>();

                for (Map.Entry<String, String> param : getParams().entrySet()) {
                    formParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                }

                if (!formParams.isEmpty()) {
                    try {
                        builder.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Error creating form parameters");
                    }
                }
            }
        };
    }

    protected Map<String, String> getParams() {
        return this.params;
    }
}
