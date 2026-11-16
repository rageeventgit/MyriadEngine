package net.rageevent.myriad;

import dev.datum.Datum;
import dev.quartz.paper.QuartzPaper;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.SneakyThrows;
import net.rageevent.fern.ServerManifest;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.server.GameMetaData;
import net.rageevent.governor.api.server.ServerProfile;
import net.rageevent.governor.api.shipments.network.server.SupplyGameProfile;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.database.MessagingDatabaseConnection;
import net.rageevent.myriad.api.database.redis.RedisConnectionInfo;
import net.rageevent.myriad.api.database.redis.RedisDatabaseConnection;
import net.rageevent.myriad.api.player.store.DataAccessor;
import net.rageevent.myriad.api.server.LobbyType;
import net.rageevent.myriad.api.server.ServerModeType;
import net.rageevent.myriad.api.server.ServerType;
import net.rageevent.myriad.commands.GameCommandNodes;
import net.rageevent.myriad.commands.StaffCommandNodes;
import net.rageevent.myriad.interals.FlagParser;
import net.rageevent.myriad.listener.ChatListener;
import net.rageevent.myriad.listener.JoinListener;
import net.rageevent.myriad.listener.PluginListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

@Getter
public final class MyriadBasePlugin extends JavaPlugin {

    private ControlUnit controlUnit;
    private Datum globalWorldManager;
    private ServerManifest serverManifest;

    private MessagingDatabaseConnection<LettuceDatabaseInteractor> redisConnector;


    @SneakyThrows
    @Override
    public void onLoad() {
        globalWorldManager = Datum.http("https://datum.rageevent.net")
                .apiKey(System.getenv("DATUM_API_KEY"))
                .build();

        int hostPort = Integer.parseInt(FlagParser.getFlag("hub.port"));
        String hostConnection = FlagParser.getFlag("hub.host");
        String redisPassword = System.getenv("REDIS_PASSWORD");

        Path serverDirectory = Bukkit.getWorldContainer().toPath();
        String serverHost = System.getenv("FERN_HOST");
        int serverPort = Integer.parseInt(System.getenv("FERN_PORT"));

        serverManifest = ServerManifest.read(serverDirectory);

        String serverName = serverManifest.name();

        Optional<ServerType> serverType =
                ServerType.TYPES.stream().filter(comparedType -> comparedType.id().equalsIgnoreCase(serverManifest.field("gametype", LobbyType.MAIN.name()))).findFirst();

        if (serverType.isEmpty()) {
            getComponentLogger().error(Text.convert("<red>Failed to find a valid gametype"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Optional<ServerModeType> modeType =
                ServerModeType.TYPES.stream()
                        .filter(comparedType -> comparedType.name().equalsIgnoreCase(serverManifest.field("mode").orElse(null))).findFirst();

        getComponentLogger().debug(Text.convert("<yellow>Server Type: <aqua>" + serverType.get().id() + " " + serverType.get().visualName()));
        ServerProfile serverProfile = ServerProfile.backend(serverType.get(), serverHost, serverPort, 25);

        controlUnit = new ControlUnit(new ControlUnit.Connection(serverName, hostConnection, hostPort), serverProfile);
        controlUnit.connect().whenComplete(((parcelConnection, throwable) -> {
            if (throwable != null) {
                getComponentLogger().error(Text.convert("<red>%s <gray>(%s)".formatted(throwable.getLocalizedMessage(), throwable.getClass().getSimpleName())));
                return;
            }
            getComponentLogger().debug(Text.convert("<green>Successfully connect to <yellow>Governor</yellow>. <gray>(%s)".formatted(parcelConnection.name())));

            modeType.ifPresent(serverModeType -> {
                String mapName = QuartzPaper.get().startupWorldName().orElse("");
                if (mapName.isEmpty()) return;
                mapName = mapName.replace(".qtz", "");
                controlUnit.deliver(new SupplyGameProfile(serverName, new GameMetaData(serverModeType, mapName)));
                getComponentLogger().debug(Text.convert("<yellow>Mode: <green>${modeType.get().visualName()} <gray>(${modeType.get().name()}"));
            });
        }));
        redisConnector = new RedisDatabaseConnection(new RedisConnectionInfo("127.0.0.1", 6379, "default", redisPassword));
        redisConnector.connect();

        DataAccessor.connection(redisConnector);

        getServer().getServicesManager().register(ControlUnit.class, controlUnit, this, ServicePriority.High);
        getServer().getServicesManager().register(ServerManifest.class, serverManifest, this, ServicePriority.High);
        getServer().getServicesManager().register(Datum.class, globalWorldManager, this, ServicePriority.High);
    }

    @SneakyThrows
    @Override
    public void onEnable() {
        Text.initialize(new ArrayList<>());

        LifecycleEventManager<Plugin> lifecycleManager = getLifecycleManager();
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(GameCommandNodes.lobbyNode(controlUnit));
            commands.register(GameCommandNodes.playNode(controlUnit));
            commands.register(StaffCommandNodes.vanish());
        });
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this, controlUnit), this);
        Bukkit.getPluginManager().registerEvents(new PluginListener(this),this);
    }

    @Override
    public void onDisable() {
        globalWorldManager.close();
        controlUnit.close();
        redisConnector.disconnect();
    }
}
