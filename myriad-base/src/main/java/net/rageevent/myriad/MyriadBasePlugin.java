package net.rageevent.myriad;

import dev.quartz.paper.QuartzPaper;
import dev.quartz.paper.QuartzPlugin;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.SneakyThrows;
import net.rageevent.fern.ServerManifest;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.server.GameMetaData;
import net.rageevent.governor.api.server.ServerProfile;
import net.rageevent.governor.api.shipments.network.server.RegisterGameServer;
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

    /**
     * Processes incoming dirty-state tracking blocks received from the Redis cache layer
     * and stages them as efficient, high-performance batch updates for MongoDB.
     *
     * <p><b>Architecture Strategy:</b>
     * To isolate the primary game loop execution environments from expensive I/O operations and
     * data-mapping computations, data persistence tasks are handled entirely by this proxy pipeline.
     * </p>
     *
     * <p><b>Execution Logic:</b>
     * <ol>
     *   <li>The proxy extracts the targeting criteria (e.g., <code>playerId</code>) to construct
     *       a specific MongoDB document filter.</li>
     *   <li>The nested, hierarchical <code>"dirty"</code> document structure provided natively
     *       by RedisJSON is unpacked on the fly.</li>
     *   <li>The nested nodes are recursively flattened into standard dot-notation string keys
     *       (e.g., transforming <code>{"status": {"isActive": true}}</code> into
     *       <code>"status.isActive": true</code>). This critical step prevents MongoDB's
     *       <code>$set</code> operator from accidentally overwriting concurrent, unrelated
     *       nested fields.</li>
     *   <li>The safe, flattened mutations are added to a bulk write batch queue to be flushed
     *       to the database cluster asynchronously or via an unordered network pipe.</li>
     * </ol>
     * </p>
     *
     */

    @SneakyThrows
    @Override
    public void onLoad() {
    }


    /*
     * Dynamic Player Data Cache & Batched Database Writer
     *
     * DESIGN CONTEXT & ARCHITECTURE:
     * - Powered by RedisJSON (ReJSON) to manipulate structured key profiles via JSON paths.
     * - Utilizes a "Dirty-Delta Tracking" pattern to minimize expensive, full-document database serialization.
     * - Player data is categorized into:
     *   1. Global Metadata: Read-heavy fields (rank, username, current server) cached globally while online.
     *   2. Server-Specific Data: Heavy gameplay data (inventories, stats) loaded dynamically on server-join.
     *
     * DATA SYNC & FLUSH PIPELINE:
     * - Write Operations: Modifying key states creates or appends data to a dedicated dirty key:
     *   'dirty:serverId:playerId' containing explicit modification operators (e.g., set, increment).
     * - Interval Batched Saving: Every 3 minutes, a background cron task fetches pending dirty keys (using
     *   Redis SCAN or an index SET to avoid blocking), batches the delta writes, flushes them to the persistent
     *   database, and deletes the dirty key.
     * - Server Swapping Exception (Anti-Race Condition): Upon a cross-server transfer, the 3-minute interval
     *   is bypassed. The source server forces an immediate sync of its dirty data to guarantee data integrity,
     *   blocks the target server via a locking status flag if needed, and safely transfers ownership.
     * - Network Disconnect: On total network logout, all remaining dirty data is immediately flushed,
     *   the final document is mirrored to the database, and the ReJSON key is evicted ('DEL') to prevent memory bloat.
     */


    @SneakyThrows
    @Override
    public void onEnable() {
        Text.initialize(new ArrayList<>());

        int hostPort = Integer.parseInt(FlagParser.getFlag("hub.port"));
        String hostConnection = FlagParser.getFlag("hub.host");
        String redisPassword = FlagParser.getFlag("redis.p");
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
                if(mapName.isEmpty()) return;
                mapName = mapName.replace(".qtz","");
                controlUnit.deliver(new SupplyGameProfile(serverName, new GameMetaData(serverModeType, mapName)));
                getComponentLogger().debug(Text.convert("<yellow>Mode: <green>${modeType.get().visualName()} <gray>(${modeType.get().name()}"));
            });
        }));
        redisConnector = new RedisDatabaseConnection(new RedisConnectionInfo("127.0.0.1", 6379, "default", redisPassword));
        redisConnector.connect();

        DataAccessor.connection(redisConnector);

        getServer().getServicesManager().register(ControlUnit.class, controlUnit, this, ServicePriority.High);
        getServer().getServicesManager().register(ServerManifest.class, serverManifest, this, ServicePriority.High);

        LifecycleEventManager<Plugin> lifecycleManager = getLifecycleManager();
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(GameCommandNodes.lobbyNode(controlUnit));
            commands.register(StaffCommandNodes.launch());
            commands.register(StaffCommandNodes.vanish());
        });


        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this, controlUnit), this);
    }

    @Override
    public void onDisable() {
        controlUnit.close();
        redisConnector.disconnect();
    }
}
