package io.store.ua.utility;

import io.store.ua.exceptions.HttpException;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class HttpRequestService {
    private final ExecutorService cachedExecutorService;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build();

    public CompletableFuture<Response> fetch(Request request) {
        return CompletableFuture.supplyAsync(() -> querySync(request), cachedExecutorService);
    }

    public CompletableFuture<Response> fetchAsync(Request request) {
        return queryAsync(request);
    }

    private Response querySync(Request request) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HttpException(
                        "Unsuccessful response for request %s %s".formatted(request.method(), request.url()),
                        HttpStatus.resolve(response.code()),
                        response.body() != null ? response.body().string() : null);
            }

            return response;
        } catch (Exception e) {
            return null;
        }
    }

    private CompletableFuture<Response> queryAsync(Request request) {
        Call call = client.newCall(request);
        CompletableFuture<Response> result = new CompletableFuture<>();

        call.enqueue(
                new Callback() {
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try {
                            if (!response.isSuccessful()) {
                                var status = HttpStatus.resolve(response.code());
                                result.completeExceptionally(
                                        new HttpException("Unsuccessful response for %s %s".formatted(request.method(), request.url()), Objects.isNull(status) ? HttpStatus.SERVICE_UNAVAILABLE : status, response.peekBody(Long.MAX_VALUE).string()));
                            } else {
                                result.complete(response);
                            }
                        } catch (Exception e) {
                            result.completeExceptionally(e);
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        result.completeExceptionally(e);
                    }
                });

        return result;
    }
}
