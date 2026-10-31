package net.rageevent.myriad.overseer;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.overseer.api.ParcelConnection;
import net.rageevent.myriad.overseer.parcels.network.PlayerJoinedNetwork;
import net.rageevent.myriad.overseer.parcels.network.PlayerLeftNetwork;
import net.rageevent.myriad.overseer.parcels.network.PlayerSwitchedServer;
import net.rageevent.myriad.overseer.parcels.network.ProxyRoster;
import net.rageevent.myriad.overseer.protocol.ConnectionListener;

import java.util.List;

@RequiredArgsConstructor
public class PlayerSync implements ConnectionListener {

    private final ProxyServer proxyServer;
    private final ControlUnit controlUnit;


    @Subscribe
    public void onLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        controlUnit.send(new PlayerJoinedNetwork(player.getUniqueId(), player.getUsername(), currentServer(player)));
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        String server = currentServer(player);
        if (server == null) return;
        controlUnit.send(new PlayerSwitchedServer(player.getUniqueId(), server));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        controlUnit.send(new PlayerLeftNetwork(player.getUniqueId()));
    }

    @Override
    public void onReady(ParcelConnection connection) {
        List<ProxyRoster.Entry> roster = proxyServer.getAllPlayers().stream()
                .map(player -> new ProxyRoster.Entry(player.getUniqueId(), player.getUsername(), currentServer(player)))
                .toList();
        controlUnit.send(new ProxyRoster(roster));
    }

    private static String currentServer(Player player) {
        return player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null);
    }
}
