package net.rageevent.myriad.api.database.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.api.database.api.communication.MessageAgentStore;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.database.MessagingDatabaseConnection;
import net.rageevent.myriad.api.database.api.query.Query;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.QueryLane;
import net.rageevent.myriad.api.database.api.query.SequencedQuery;
import net.rageevent.myriad.api.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
public class RedisDatabaseConnection implements MessagingDatabaseConnection<LettuceDatabaseInteractor> {

    private final RedisConnectionInfo connectionInfo;

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private LettuceDatabaseInteractor interactor;
    private MessageAgentStore agentStore;

    private boolean opened;

    private QueryLane queryLane;

    @Override
    public void connect() {
        opened = true;

        int redisPort = connectionInfo.port();
        RedisURI.Builder uriBuilder = redisPort == -1 ?
                RedisURI.Builder.redis(connectionInfo.url()) : RedisURI.Builder.redis(connectionInfo.url(), redisPort);


        if (!connectionInfo.password().isBlank())
            uriBuilder = uriBuilder.withPassword(connectionInfo.password());

        client = RedisClient.create(uriBuilder.build());

        connection = client.connect();

        pubSubConnection = client.connectPubSub();
        agentStore = new RedisMessagingAgent(pubSubConnection);
        pubSubConnection.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                if (!channel.matches("(\\w+):(\\w+)")) return; //Aligns to channel format.
                agentStore.handle(QueryKey.of(channel), message);
            }
        });
        queryLane = new QueryLane(2, "redis-lane");
        interactor = new RedisDatabaseInteractor(pubSubConnection, connection, agentStore);
    }

    @Override
    public void disconnect() {
        opened = false;
        agentStore.clear();
        interactor.shutdown();

        pubSubConnection.close();
        connection.close();
        client.shutdown();
    }

    @Override
    public LettuceDatabaseInteractor interactor() {
        return interactor;
    }

    @Override
    public MessageAgentStore agent() {
        return agentStore;
    }

    //This will act as any other query, but with the added ability of publishing to redis.


    @Override
    public <T, Q extends Query<T, LettuceDatabaseInteractor>> CompletableFuture<QueryResult<T>> post(Q query) {
        if (!opened)
            return CompletableFuture.completedFuture(new FailedQueryResult<>("Connection not opened."));

        if (query instanceof SequencedQuery<?, ?> sequencedQuery) {
            CompletableFuture<QueryResult<T>> out = new CompletableFuture<>();
            queryLane.executor(sequencedQuery.key()).execute(() -> query.call(interactor).whenComplete((res, ex) -> {
                if (ex != null) out.completeExceptionally(ex);
                else out.complete(res);
            }));
            return out;
        }

        return query.call(interactor);
    }
}
