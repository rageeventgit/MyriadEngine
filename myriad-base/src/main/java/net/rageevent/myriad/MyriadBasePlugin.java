package net.rageevent.myriad;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.SneakyThrows;
import net.rageevent.fern.ServerManifest;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.server.ServerProfile;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.database.DatabaseConnection;
import net.rageevent.myriad.api.database.api.database.MessagingDatabaseConnection;
import net.rageevent.myriad.api.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.api.database.mongo.MongoConnectionInfo;
import net.rageevent.myriad.api.database.mongo.MongoDatabaseConnection;
import net.rageevent.myriad.api.database.redis.RedisConnectionInfo;
import net.rageevent.myriad.api.database.redis.RedisDatabaseConnection;
import net.rageevent.myriad.api.server.LobbyType;
import net.rageevent.myriad.api.server.ServerModeType;
import net.rageevent.myriad.api.server.ServerType;
import net.rageevent.myriad.commands.GameCommandNodes;
import net.rageevent.myriad.interals.FlagParser;
import net.rageevent.myriad.listener.ChatListener;
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

    private ServerManifest serverManifest;

    private MessagingDatabaseConnection<LettuceDatabaseInteractor> redisConnector;
    private DatabaseConnection<BsonDatabaseInteractor> mongoConnector;

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

        String redisPassword = FlagParser.getFlag("redis.p");
        String mongoPassword = FlagParser.getFlag("mongo.p");


        Path serverDirectory = Bukkit.getWorldContainer().toPath();

        serverManifest = ServerManifest.read(serverDirectory);

        Optional<ServerType> serverType = ServerType.TYPES.stream().filter(comparedType -> comparedType.id().equalsIgnoreCase(serverManifest.field("gametype", LobbyType.MAIN.name()))).findFirst();

        if (serverType.isEmpty()) {
            getComponentLogger().error(Text.convert("<red>Failed to find a valid gametype"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Optional<ServerModeType> modeType =
                ServerModeType.TYPES.stream()
                        .filter(comparedType -> comparedType.name().equalsIgnoreCase(serverManifest.field("mode").orElse(null))).findFirst();

        getComponentLogger().debug(Text.convert("<yellow>Server Type: <aqua>" + serverType.get().id() + " " + serverType.get().visualName()));
        ServerModeType serverModeType = modeType.orElse(null);

        if (serverModeType != null) {
            getComponentLogger().debug(Text.convert("<yellow>Mode: <green>${modeType.get().visualName()} <gray>(${modeType.get().name()}"));
        }

        ServerProfile serverProfile = ServerProfile.backend(serverType.get(), serverModeType, 25);

        controlUnit = new ControlUnit(new ControlUnit.Connection(serverManifest.name(), hostConnection, hostPort), serverProfile);
        controlUnit.connect().whenComplete(((parcelConnection, throwable) -> {
            if (throwable != null) {
                getComponentLogger().error(Text.convert("<red>%s <gray>(%s)".formatted(throwable.getLocalizedMessage(), throwable.getClass().getSimpleName())));
                return;
            }
            getComponentLogger().debug(Text.convert("<green>Successfully connect to <blue>Overseer</blue>. <gray>(%s)".formatted(parcelConnection.name())));
        }));
        redisConnector = new RedisDatabaseConnection(new RedisConnectionInfo("127.0.0.1", 6379, "default", redisPassword));
        redisConnector.connect();

        mongoConnector = new MongoDatabaseConnection(new MongoConnectionInfo("mongodb://127.0.0.1:27017/", "fern", "admin", mongoPassword));
        mongoConnector.connect();

        getServer().getServicesManager().register(ControlUnit.class, controlUnit, this, ServicePriority.High);
        getServer().getServicesManager().register(ServerManifest.class, serverManifest, this, ServicePriority.High);

        LifecycleEventManager<Plugin> lifecycleManager = getLifecycleManager();

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(GameCommandNodes.lobbyNode(controlUnit));
            commands.register(GameCommandNodes.symbols());
            commands.register(GameCommandNodes.redisTest(this));
            commands.register(GameCommandNodes.transferTest(this));
        });

        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
    }

    @Override
    public void onDisable() {
        controlUnit.close();
        redisConnector.disconnect();
        mongoConnector.disconnect();
    }
}
