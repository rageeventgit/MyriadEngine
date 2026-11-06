package net.rageevent.myriad.database.mongo;

import io.lettuce.core.api.StatefulRedisConnection;
import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.database.api.communication.KeyValueDatabaseInteractor;
import net.rageevent.myriad.database.api.query.QueryKey;
import net.rageevent.myriad.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class MongoDatabaseInteractor implements KeyValueDatabaseInteractor {

    private final StatefulRedisConnection<String, String> redisConnection;

    private final ExecutorService mongoExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));


    @Override
    public CompletableFuture<QueryResult<Void>> set(QueryKey key, Object value) {
        if (closed()) return notConnected();
//        return redisConnection.async().set(key.namespaced(), value.toString()).toCompletableFuture()
//                .thenApplyAsync(s -> (QueryResult<Void>) new UnknownQueryResult<Void>(s), redisExecutor)
//                .exceptionally(FailedQueryResult::new);
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<QueryResult<String>> get(QueryKey key) {
        if (closed()) return notConnected();
//        return redisConnection.async().get(key.namespaced())
//                .toCompletableFuture()
//                .thenApplyAsync(s -> (QueryResult<String>) new PassedQueryResult<>(s, "s"), redisExecutor).exceptionally(FailedQueryResult::new);
        return null;
    }

    @Override
    public CompletableFuture<Void> remove(QueryKey key) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> exists(QueryKey key) {
        return null;
    }

    @Override
    public void shutdown() {
        mongoExecutor.shutdown();
        try {
            mongoExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean closed() {
        return !redisConnection.isOpen();
    }

    private <T> CompletableFuture<QueryResult<T>> notConnected() {
        return CompletableFuture.completedFuture(new FailedQueryResult<>("No connection to Mongo"));
    }

}
