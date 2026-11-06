package net.rageevent.myriad.database.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.database.api.communication.MessageAgentStore;
import net.rageevent.myriad.database.api.communication.MessagingDatabaseInteractor;
import net.rageevent.myriad.database.api.database.MessagingDatabaseConnection;
import net.rageevent.myriad.database.api.query.Query;
import net.rageevent.myriad.database.api.query.QueryKey;
import net.rageevent.myriad.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
public class RedisDatabaseConnection implements MessagingDatabaseConnection<MessagingDatabaseInteractor> {

    private final RedisConnectionInfo connectionInfo;

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private MessagingDatabaseInteractor interactor;
    private MessageAgentStore agentStore;

    private boolean opened;

    @Override
    public void connect() {
        opened = true;

        RedisURI.Builder uriBuilder = RedisURI.Builder.redis(connectionInfo.url(), connectionInfo.port());

        if (!(connectionInfo.username().isBlank() && connectionInfo.password().isBlank()))
            uriBuilder.withAuthentication(connectionInfo.username(), connectionInfo.password());

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
    public MessagingDatabaseInteractor interactor() {
        return interactor;
    }

    @Override
    public MessageAgentStore agent() {
        return agentStore;
    }

    //This will act as any other query, but with the added ability of publishing to redis.


    @Override
    public <T, Q extends Query<T, MessagingDatabaseInteractor>> CompletableFuture<QueryResult<T>> post(Q query) {
        if (!opened)
            return CompletableFuture.completedFuture(new FailedQueryResult<>("Connection not opened."));
        return query.call(interactor);
    }
}
