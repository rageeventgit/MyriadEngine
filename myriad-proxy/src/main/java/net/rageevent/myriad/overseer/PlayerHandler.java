package net.rageevent.myriad.overseer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.annotations.Evolving;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.EnvelopeAgent;
import net.rageevent.governor.api.EnvelopeCarrier;
import net.rageevent.governor.api.EnvelopeConnection;
import net.rageevent.governor.api.exceptions.GovernorException;
import net.rageevent.governor.api.protocol.ConnectionListener;
import net.rageevent.governor.api.shipments.network.player.PlayerJoinedNetwork;
import net.rageevent.governor.api.shipments.network.player.PlayerLeftNetwork;
import net.rageevent.governor.api.shipments.network.player.PlayerSwitchedServer;
import net.rageevent.governor.api.shipments.network.player.ProxyRoster;
import net.rageevent.governor.api.shipments.network.player.data.PullUserData;
import net.rageevent.governor.api.shipments.network.player.data.PushUserData;
import net.rageevent.governor.api.shipments.network.player.data.UserDataResponse;
import net.rageevent.myriad.Text;
import net.rageevent.myriad.api.database.JsonUtil;
import net.rageevent.myriad.api.database.MongoCollections;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.database.DatabaseConnection;
import net.rageevent.myriad.api.database.api.database.MessagingDatabaseConnection;
import net.rageevent.myriad.api.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.api.database.api.mongo.MongoQueryKey;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.player.UserRank;
import net.rageevent.myriad.api.player.stats.BasePlayerStats;
import net.rageevent.myriad.api.queries.mongo.WriteJsonDocument;
import net.rageevent.myriad.api.queries.redis.PostUserData;
import net.rageevent.myriad.api.queries.redis.RetrieveUserData;
import net.rageevent.myriad.api.queries.redis.user.SetUserDataQuery;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class PlayerHandler implements EnvelopeCarrier, ConnectionListener {

    private final ProxyServer proxyServer;
    private final ComponentLogger logger;
    private final DatabaseConnection<BsonDatabaseInteractor> mongoConnector;
    private final MessagingDatabaseConnection<LettuceDatabaseInteractor> redisConnector;
    private final ControlUnit controlUnit;

    @Override
    public void register(EnvelopeAgent envelopeAgent) {
        envelopeAgent.onAsyncTransaction(PullUserData.class, (envelopeConnection, pullUserData) -> {
            UUID player = pullUserData.player();
            String section = pullUserData.section();
            return mongoConnector.interactor()
                    .nested(MongoCollections.PLAYERS, MongoQueryKey.id(player.toString()), List.of(section))
                    .thenCompose(result -> {
                        if (result.failed()) {
                            return CompletableFuture.failedFuture(
                                    new GovernorException("Couldn't load '" + section + "' for " + player));
                        }
                        var patch = JsonUtil.toMergePatch(section, JsonUtil.OBJECT_MAPPER.valueToTree(result.object()));
                        return redisConnector.interactor()
                                .append(new QueryKey("user", player.toString()), "$", patch)
                                .thenApply(written -> new UserDataResponse("Successfully found data"));
                    });
        });
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        UUID player = event.getUniqueId();

        mongoConnector.interactor().nested(MongoCollections.PLAYERS, MongoQueryKey.id(player.toString()),
                        List.of(
                                BasePlayerStats.UNIQUE_ID.databaseName(),
                                BasePlayerStats.USERNAME.databaseName(),
                                BasePlayerStats.RANK.databaseName()
                        ))
                .whenComplete((result, error) -> {
                    ObjectNode found = (ObjectNode) result.object();
                    if (result.failed()) {
                        found = JsonUtil.OBJECT_MAPPER.createObjectNode();
                        found.put("_id", player.toString());
                        found.put(BasePlayerStats.UNIQUE_ID.databaseName(), player.toString());
                        found.put(BasePlayerStats.USERNAME.databaseName(), event.getUsername());
                        found.put(BasePlayerStats.RANK.databaseName(), UserRank.NORMAL.name());
                        mongoConnector.post(new WriteJsonDocument(MongoCollections.PLAYERS, found));
                    }
                    redisConnector.post(new PostUserData(player, found));
                });
    }

    @Subscribe
    public void onLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        controlUnit.deliver(new PlayerJoinedNetwork(player.getUniqueId(), player.getUsername(), currentServer(player)));
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        String server = currentServer(player);
        if (server == null) return;
        controlUnit.deliver(new PlayerSwitchedServer(player.getUniqueId(), server));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        controlUnit.deliver(new PlayerLeftNetwork(player.getUniqueId()));

        redisConnector.post(new RetrieveUserData(player.getUniqueId())).whenComplete((queryResult, _) -> {
            if (queryResult.failed()) {
                logger.error(Text.convert("<red>Failed to retrieve user data..."));
                return; //No ideas or cares on how to handle this...
            }
            JsonNode object = queryResult.object();
            mongoConnector.interactor().update(MongoCollections.PLAYERS, MongoQueryKey.id(player.getUniqueId().toString()), object).whenComplete((mongoResult, _) -> {
                logger.debug(Text.convert("<yellow>${mongoResult.message()}"));
            });
            redisConnector.interactor().remove(new QueryKey("user", player.getUniqueId().toString()));
        });
    }

    @Override
    public void ready(EnvelopeConnection connection) {
        List<ProxyRoster.UserEntry> roster = proxyServer.getAllPlayers().stream()
                .map(player -> new ProxyRoster.UserEntry(player.getUniqueId(), player.getUsername(), currentServer(player)))
                .toList();
        controlUnit.deliver(new ProxyRoster(roster));
    }

    private static String currentServer(Player player) {
        return player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null);
    }


}
