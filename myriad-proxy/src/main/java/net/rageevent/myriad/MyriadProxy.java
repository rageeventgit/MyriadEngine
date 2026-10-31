package net.rageevent.myriad;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.crownrealm.conduit.ServerManifest;
import lombok.SneakyThrows;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.server.ServerProfile;
import net.rageevent.myriad.internal.FlagParser;
import net.rageevent.myriad.overseer.PlayerSync;
import net.rageevent.myriad.overseer.ServerProxyBridge;

import java.util.ArrayList;

@Plugin(id = "myriad-proxy", name = "MyriadProxy", version = "1.0-SNAPSHOT", authors = "RageEvent", description = "Allows for the linking of overseer to the server")
public class MyriadProxy {

    private final ProxyServer proxyServer;
    private final ComponentLogger logger;
    private ControlUnit controlUnit;

    @Inject
    public MyriadProxy(ProxyServer proxyServer, ComponentLogger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @SneakyThrows
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ServerManifest serverManifest = ServerManifest.read();
        Text.initialize(new ArrayList<>());

        int hostPort = Integer.parseInt(FlagParser.getFlag("governor.port"));
        String hostConnection = FlagParser.getFlag("governor.host");

        controlUnit = new ControlUnit(new ControlUnit.Connection(serverManifest.name(), hostConnection, hostPort), ServerProfile.proxy());
        controlUnit.install(new ServerProxyBridge(proxyServer, logger)).listen(new PlayerSync(proxyServer, controlUnit));

        controlUnit.connect().whenComplete(((parcelConnection, throwable) -> {
            if (throwable != null) {
                logger.error(Text.convert("<red>%s <gray>(%s)".formatted(throwable.getLocalizedMessage(), throwable.getClass().getSimpleName())));
                return;
            }
            logger.debug(Text.convert("<green>Successfully connect to <blue>Overseer</blue>. <gray>(%s)".formatted(parcelConnection.name())));
        }));

        proxyServer.getEventManager().register(this, new PlayerSync(proxyServer,controlUnit));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (controlUnit != null && controlUnit.connected())
            controlUnit.close();
    }

}
