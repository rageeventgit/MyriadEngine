package net.rageevent.myriad.overseer;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.rageevent.governor.api.EnvelopeAgent;
import net.rageevent.governor.api.EnvelopeCarrier;
import net.rageevent.governor.api.shipments.network.player.TransferPlayerRequest;
import net.rageevent.governor.api.shipments.network.player.TransferResult;
import net.rageevent.governor.api.shipments.network.server.RegisterGameServer;
import net.rageevent.governor.api.shipments.network.server.RemoveGameServer;
import net.rageevent.myriad.Text;
import net.rageevent.myriad.server.ServerType;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@RequiredArgsConstructor
public class ServerProxyBridge implements EnvelopeCarrier {

    private final ProxyServer proxyServer;
    private final ComponentLogger logger;


    private final Set<String> registeredByUs = ConcurrentHashMap.newKeySet();
    private final Map<String, ServerType> groups = new ConcurrentHashMap<>();

    @Override
    public void register(EnvelopeAgent parcelHandlers) {
        parcelHandlers.onAsyncTransaction(TransferPlayerRequest.class, (_, request) -> transfer(request))
                .on(RegisterGameServer.class, (_, added) -> addServer(added))
                .on(RemoveGameServer.class, (_, removed) -> removeServer(removed));


    }

    private void removeServer(RemoveGameServer removed) {
        String removedName = removed.name();
        groups.remove(removedName);
        if (!registeredByUs.remove(removedName)) return;
        proxyServer.getServer(removedName).ifPresentOrElse(server -> {
            proxyServer.unregisterServer(server.getServerInfo());
            logger.info(Text.convert("<green>Unregistered server <pink>'%s'</pink>.".formatted(removedName)));
        }, () ->
                logger.warn(Text.convert("<red>Couldn't find a server with name <yellow>'%s'</yellow>.".formatted(removedName))));
    }

    private void addServer(RegisterGameServer added) {
        String addedServerName = added.name();
        ServerType addedServerGroup = added.serverType();
        groups.put(addedServerName, addedServerGroup);

        if (added.host() == null || proxyServer.getServer(addedServerName).isPresent()) {
            logger.warn(Text.convert("<red>Server <yellow>'%s'</yellow> is already registered.".formatted(addedServerName)));
            return;
        }
        proxyServer.registerServer(new ServerInfo(addedServerName, new InetSocketAddress(added.host(), added.port())));
        registeredByUs.add(addedServerName);
        logger.info(Text.convert("<green>Registered server <pink>'%s'</pink>. <silver>(%s)</silver>".formatted(addedServerName, addedServerGroup.visualName())));
    }

    private CompletableFuture<TransferResult> transfer(TransferPlayerRequest request) {
        Optional<Player> player = player(request.player());

        String serverName = request.server();
        if (player.isEmpty())
            return done(TransferResult.Status.NOT_ONLINE, serverName, "Player not on this proxy.");

        Optional<RegisteredServer> server = proxyServer.getServer(serverName);

        return server.map(registeredServer -> player.get().createConnectionRequest(registeredServer).connect()
                        .thenApply(result -> toTransferResult(result, serverName))
                        .exceptionally(error -> new TransferResult(TransferResult.Status.FAILED, request.server(), error.getMessage())))
                .orElseGet(() -> done(TransferResult.Status.UNKNOWN_SERVER, serverName, "No server found."));
    }

    private TransferResult toTransferResult(ConnectionRequestBuilder.Result result, String server) {
        return switch (result.getStatus()) {
            case SUCCESS -> new TransferResult(TransferResult.Status.SUCCESS, server, null);
            case ALREADY_CONNECTED -> new TransferResult(TransferResult.Status.ALREADY_CONNECTED, server, null);
            case CONNECTION_IN_PROGRESS ->
                    new TransferResult(TransferResult.Status.FAILED, server, "Already connecting to a server");
            case CONNECTION_CANCELLED ->
                    new TransferResult(TransferResult.Status.FAILED, server, "Connection cancelled");
            case SERVER_DISCONNECTED ->
                    new TransferResult(TransferResult.Status.FAILED, server, result.getReasonComponent()
                            .map(reason -> PlainTextComponentSerializer.plainText().serialize(reason))
                            .orElse("Disconnected by the server"));
        };
    }

    private CompletableFuture<TransferResult> done(TransferResult.Status status, String server, String detail) {
        return CompletableFuture.completedFuture(new TransferResult(status, server, detail));
    }

    private Optional<Player> player(UUID uuid) {
        return proxyServer.getPlayer(uuid);
    }
}
