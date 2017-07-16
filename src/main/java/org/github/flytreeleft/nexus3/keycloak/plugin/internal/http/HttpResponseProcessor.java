package org.github.flytreeleft.nexus3.keycloak.plugin.internal.http;

import java.io.InputStream;

public interface HttpResponseProcessor<R> {

    R process(InputStream inputStream);
}
