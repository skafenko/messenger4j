package com.github.messenger4j;

import com.github.messenger4j.spi.MessengerHttpClient;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ThreadPoolExecutor;

import okhttp3.*;

/**
 * @author Max Grabenhorst
 * @since 1.0.0
 */
final class DefaultMessengerHttpClient implements MessengerHttpClient {

    private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";

    private final OkHttpClient okHttp = new OkHttpClient();

    @Override
    public void destroy() {
        ConnectionPool connectionPool = okHttp.connectionPool();
        connectionPool.evictAll();

        okHttp.dispatcher().executorService().shutdownNow();

        Class<ConnectionPool> connectionPoolClass = ConnectionPool.class;
        try {
            Field field = connectionPoolClass.getDeclaredField("executor");
            field.setAccessible(true);
            ThreadPoolExecutor executor = ((ThreadPoolExecutor) field.get(null));
            executor.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public HttpResponse execute(HttpMethod httpMethod, String url, String jsonBody) throws IOException {
        final Request.Builder requestBuilder = new Request.Builder().url(url);
        if (httpMethod != HttpMethod.GET) {
            final MediaType jsonMediaType = MediaType.parse(APPLICATION_JSON_CHARSET_UTF_8);
            final RequestBody requestBody = RequestBody.create(jsonMediaType, jsonBody);
            requestBuilder.method(httpMethod.name(), requestBody);
        }
        final Request request = requestBuilder.build();
        try (Response response = this.okHttp.newCall(request).execute()) {
            return new HttpResponse(response.code(), response.body().string());
        }
    }
}
