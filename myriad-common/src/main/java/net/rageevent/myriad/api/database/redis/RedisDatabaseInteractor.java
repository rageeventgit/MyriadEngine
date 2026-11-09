package net.rageevent.myriad.api.database.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.rageevent.myriad.api.database.JsonUtil;
import net.rageevent.myriad.api.database.api.communication.MessageAgentStore;
import net.rageevent.myriad.api.database.api.communication.MessagingDatabaseInteractor;
import net.rageevent.myriad.api.database.api.communication.Serializer;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.PassedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;
import net.rageevent.myriad.api.database.api.query.result.UnknownQueryResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisDatabaseInteractor implements MessagingDatabaseInteractor {

    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private final StatefulRedisConnection<String, String> redisConnection;
    private final MessageAgentStore messagingStore;

    private final ExecutorService redisExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

    @Override
    public <T> CompletableFuture<QueryResult<Long>> publish(QueryKey channel, T raw, Serializer<T> serializer) {
        if (!open()) return notConnected();
        return pubSubConnection.async().publish(channel.namespaced(), serializer.serialize(raw)).toCompletableFuture()
                .thenApplyAsync(v -> (QueryResult<Long>) new PassedQueryResult<>(v, "Successfully published message."), redisExecutor).exceptionally(FailedQueryResult::new);
    }

    @Override
    public CompletableFuture<QueryResult<Void>> set(QueryKey key, Object value) {
        if (!open()) return notConnected();
        return redisConnection.async().set(key.namespaced(), value.toString()).toCompletableFuture()
                .thenApplyAsync(s -> (QueryResult<Void>) new UnknownQueryResult<Void>(s), redisExecutor)
                .exceptionally(FailedQueryResult::new);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<QueryResult<String>> get(QueryKey key) {
        if (!open()) return notConnected();
        return redisConnection.async().get(key.namespaced())
                .toCompletableFuture()
                .thenApplyAsync(s -> (QueryResult<String>) new PassedQueryResult<>(s, "s"), redisExecutor).exceptionally(FailedQueryResult::new);
    }

    @Override
    public <T> CompletableFuture<QueryResult<T>> message(QueryKey channel, String json) {
        return messagingStore.handle(channel, json, redisExecutor);
    }

    @SneakyThrows
    @Override
    public CompletableFuture<QueryResult<Void>> jsonSet(QueryKey key, JsonNode value) {
        if (!open()) return notConnected();
        return redisConnection.async().jsonSet(key.namespaced(), JsonPath.ROOT_PATH, JsonUtil.OBJECT_MAPPER.writeValueAsString(value))
                .thenApplyAsync(s -> (QueryResult<Void>) new UnknownQueryResult<Void>(s))
                .exceptionally(FailedQueryResult::new).toCompletableFuture();
    }

    @Override
    public CompletableFuture<QueryResult<JsonNode>> jsonGet(QueryKey key) {
        if (!open()) return notConnected();

        return redisConnection.async().jsonGetRaw(key.namespaced(), JsonPath.ROOT_PATH).thenApplyAsync(list -> {
            if (list.isEmpty()) return new FailedQueryResult<JsonNode>("Could not find json data.");
            String rawJsonData = list.getFirst();

            try {
                JsonNode jsonData = JsonUtil.OBJECT_MAPPER.readValue(rawJsonData, JsonNode.class);
                return (QueryResult<JsonNode>) new PassedQueryResult<>(jsonData, "Found json with key '${key.namespaced()}'");
            } catch (JsonProcessingException e) {
                return new FailedQueryResult<JsonNode>(e);
            }
        }).exceptionally(FailedQueryResult::new).toCompletableFuture();
    }

    @Override
    public CompletableFuture<QueryResult<Void>> remove(QueryKey key) {
        return redisConnection.async().del(key.namespaced())
                .thenApplyAsync(s -> {
                    if (s == 0) return new FailedQueryResult<Void>("No objects deleted");
                    return (QueryResult<Void>) new PassedQueryResult<Void>("Successfully deleted '$s' documents");
                }).exceptionally(FailedQueryResult::new).toCompletableFuture();
    }

    @Override
    public void shutdown() {
        redisExecutor.shutdown();
        try {
            redisExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        messagingStore.clear();
    }

    private boolean open() {
        return redisConnection.isOpen();
    }

    private <T> CompletableFuture<QueryResult<T>> notConnected() {
        return CompletableFuture.completedFuture(new FailedQueryResult<>("No connection to redis"));
    }
}
