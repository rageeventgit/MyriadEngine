package net.rageevent.myriad;

import io.github.crownrealm.conduit.ServerManifest;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.SneakyThrows;
import net.rageevent.myriad.commands.GameCommandNodes;
import net.rageevent.myriad.interals.FlagParser;
import net.rageevent.myriad.overseer.ControlUnit;
import net.rageevent.myriad.overseer.parcels.network.ServerProfile;
import net.rageevent.myriad.overseer.parcels.network.ServerRemoved;
import net.rageevent.myriad.server.GameType;
import net.rageevent.myriad.server.LobbyType;
import net.rageevent.myriad.server.ServerType;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public final class MyriadBasePlugin extends JavaPlugin {

    private ControlUnit controlUnit;

    private final List<ServerType> serverTypes = Stream.concat(
            Stream.of(GameType.values()),
            Stream.of(LobbyType.values())
    ).collect(Collectors.toUnmodifiableList());
    private ServerManifest serverManifest;

    @SneakyThrows
    @Override
    public void onLoad() {
    }

    @SneakyThrows
    @Override
    public void onEnable() {
        Text.initialize(new ArrayList<>());

        int hostPort = Integer.parseInt(FlagParser.getFlag("hub.port"));
        String hostConnection = FlagParser.getFlag("hub.host");

        Path serverDirectory = Bukkit.getWorldContainer().toPath();

        serverManifest = ServerManifest.read(serverDirectory);

        Optional<ServerType> serverType = serverTypes.stream().filter(comparedType -> comparedType.id().equalsIgnoreCase(serverManifest.field("gametype", LobbyType.MAIN.name()))).findFirst();
        if (serverType.isEmpty()) {
            getComponentLogger().error(Text.convert("<red>Failed to find a valid gametype"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getComponentLogger().debug(Text.convert("<yellow>Server Type: <aqua>" + serverType.get().id() + " " + serverType.get().visualName()));

        ServerProfile serverProfile = ServerProfile.backend(serverType.get(), 25);
        controlUnit = new ControlUnit(new ControlUnit.Connection(serverManifest.name(), hostConnection, hostPort), serverProfile);

        controlUnit.connect().whenComplete(((parcelConnection, throwable) -> {
            if (throwable != null) {
                getComponentLogger().error(Text.convert("<red>%s <gray>(%s)".formatted(throwable.getLocalizedMessage(), throwable.getClass().getSimpleName())));
                return;
            }
            getComponentLogger().debug(Text.convert("<green>Successfully connect to <blue>Overseer</blue>. <gray>(%s)".formatted(parcelConnection.name())));
        }));

        getServer().getServicesManager().register(ControlUnit.class, controlUnit, this, ServicePriority.High);

        LifecycleEventManager<Plugin> lifecycleManager = getLifecycleManager();

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(GameCommandNodes.lobbyNode(controlUnit));
        });

    }

    @Override
    public void onDisable() {
        controlUnit.disconnect();
    }
}
