package net.rageevent.myriad.overseer;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.RequiredArgsConstructor;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.EnvelopeConnection;
import net.rageevent.governor.api.protocol.ConnectionListener;
import net.rageevent.governor.api.shipments.network.player.PlayerJoinedNetwork;
import net.rageevent.governor.api.shipments.network.player.PlayerLeftNetwork;
import net.rageevent.governor.api.shipments.network.player.PlayerSwitchedServer;
import net.rageevent.governor.api.shipments.network.player.ProxyRoster;

import java.util.List;

@RequiredArgsConstructor
public class PlayerSync implements ConnectionListener {

    private final ProxyServer proxyServer;
    private final ControlUnit controlUnit;


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
