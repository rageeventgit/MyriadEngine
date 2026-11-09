package net.rageevent.myriad.api.database.redis;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.api.database.api.communication.CommunicationAgent;
import net.rageevent.myriad.api.database.api.communication.MessageAgentStore;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class RedisMessagingAgent implements MessageAgentStore {

    private final ConcurrentMap<QueryKey, CommunicationAgent<?>> connections = new ConcurrentHashMap<>();

    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;

    @Override
    public void add(QueryKey queryKey, CommunicationAgent<?> communicationAgent) {
        connections.put(queryKey, communicationAgent);
        pubSubConnection.async().subscribe(queryKey.namespaced());
    }

    @Override
    public void clear() {
        connections.keySet().forEach(queryKey -> pubSubConnection.async().unsubscribe(queryKey.namespaced()));
        connections.clear();
    }

    @Override
    public <T> CompletableFuture<QueryResult<T>> handle(QueryKey key, String json) {
        if (!connections.containsKey(key)) return CompletableFuture.completedFuture(new FailedQueryResult<>("Message agent not found."));
        CommunicationAgent<T> communicationAgent = (CommunicationAgent<T>) connections.get(key);
        return CompletableFuture.supplyAsync(() -> communicationAgent.handle(json));
    }

    @Override
    public <T> CompletableFuture<QueryResult<T>> handle(QueryKey key, String json, ExecutorService executorService) {
        if (!connections.containsKey(key)) return CompletableFuture.completedFuture(new FailedQueryResult<>("Message agent not found."));
        CommunicationAgent<T> communicationAgent = (CommunicationAgent<T>) connections.get(key);
        return CompletableFuture.supplyAsync(() -> communicationAgent.handle(json), executorService);
    }
}
