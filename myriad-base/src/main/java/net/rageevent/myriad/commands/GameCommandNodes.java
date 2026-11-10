package net.rageevent.myriad.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.shipments.network.player.TransferResult;
import net.rageevent.governor.api.shipments.network.server.SendToGroupRequest;
import net.rageevent.myriad.MyriadBasePlugin;
import net.rageevent.myriad.Text;
import net.rageevent.myriad.ThemeColor;
import net.rageevent.myriad.api.database.MongoCollections;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.database.DatabaseConnection;
import net.rageevent.myriad.api.database.api.database.MessagingDatabaseConnection;
import net.rageevent.myriad.api.database.api.mongo.BatchResult;
import net.rageevent.myriad.api.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.api.database.api.mongo.MongoBatch;
import net.rageevent.myriad.api.database.api.mongo.MongoQueryKey;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.queries.mongo.GetUserDocument;
import net.rageevent.myriad.api.database.queries.mongo.WriteJsonDocument;
import net.rageevent.myriad.api.database.queries.redis.PostUserData;
import net.rageevent.myriad.api.database.queries.redis.RetrieveUserData;
import net.rageevent.myriad.json.JsonUtil;
import net.rageevent.myriad.api.player.UserRank;
import net.rageevent.myriad.api.server.LobbyType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static net.kyori.adventure.text.Component.text;
import static net.rageevent.myriad.ThemeColor.RED;

@UtilityClass
public class GameCommandNodes {

    public LiteralCommandNode<CommandSourceStack> lobbyNode(ControlUnit controlUnit) {
        return literal("lobby").requires(source -> source.getSender() instanceof Player).executes(context -> {
            CommandSourceStack source = context.getSource();
            Player player = source.getPlayerOrThrow();
            controlUnit.ship(new SendToGroupRequest(player.getUniqueId(), LobbyType.MAIN)).whenComplete((result, error) -> {
                if (error != null) player.sendMessage(Text.convert("<red>Couldn't send you to the lobby."));
                else transferResult(result).ifPresent(player::sendMessage);
            });
            return 1;
        }).build();
    }

    public LiteralCommandNode<CommandSourceStack> symbols() {
        return literal("testtext").requires(source -> source.getSender() instanceof Player).then(argument("text", greedyString()).executes(context -> {
            CommandSourceStack source = context.getSource();
            Player player = source.getPlayerOrThrow();

            String text = getString(context, "text");
            player.sendMessage(Component.text("Custom Colors: " + ThemeColor.COLOR_MAP.size()));
            player.sendMessage(Text.convert(text));
            player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 1, 2));
            return 1;
        })).build();
    }

    public static LiteralCommandNode<CommandSourceStack> redisTest(MyriadBasePlugin plugin) {
        MessagingDatabaseConnection<LettuceDatabaseInteractor> redisConnector = plugin.getRedisConnector();
        return literal("redistest").requires(source -> source.getSender() instanceof Player).executes(context -> {
            CommandSourceStack source = context.getSource();
            Player player = source.getPlayerOrThrow();
            LettuceDatabaseInteractor interactor = redisConnector.interactor();

            interactor.set(QueryKey.singlet("test"), "Hello world!").whenComplete((result, error) -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (error != null) {
                        player.sendMessage(Text.convert("<red>${error.getMessage()}"));
                        return;
                    }
                    player.sendMessage(Text.convert("<aqua><misc:checkmark></aqua> <green>${result.message()}"));

                }, 0);
            });
            Bukkit.getScheduler().runTaskLater(plugin, () -> interactor.get(QueryKey.singlet("test")).whenComplete((result, error) -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (error != null) {
                        player.sendMessage(Text.convert("<red>${error.getMessage()}"));
                        return;
                    }
                    player.sendMessage(Text.convert("<yellow><misc:bullet></yellow> <gold>Result:</gold> <green>${result.object()}"));

                }, 0);
            }), 20 * 2);

            Bukkit.getScheduler().runTaskLater(plugin, () -> interactor.remove(QueryKey.singlet("test")).whenComplete((result, error) -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (error != null) {
                        player.sendMessage(Text.convert("<red>${error.getMessage()}"));
                        return;
                    }
                    player.sendMessage(Text.convert("<dark-red><misc:bullet></dark-red> <red>Message:</red> <yellow>${result.message()}"));
                }, 0);
            }), 20 * 6);
            return 1;
        }).build();
    }

    private static Optional<Component> transferResult(TransferResult result) {
        return switch (result.status()) {
            case SUCCESS -> Optional.empty();
            case ALREADY_CONNECTED -> Optional.of(text("You are already connected to this server!", RED));
            case NO_SERVER_AVAILABLE ->
                    Optional.of(text("There's no server available right now. Try again soon!", RED));
            case UNKNOWN_SERVER, NOT_ONLINE, FAILED ->
                    Optional.of(text("Couldn't send you" + (result.server() != null ? " to " + result.server() : "") + (result.detail() != null ? ": " + result.detail() : "!"), RED));
        };
    }

    public static LiteralCommandNode<CommandSourceStack> batchTest(MyriadBasePlugin plugin) {
        DatabaseConnection<BsonDatabaseInteractor> mongoInteractor = plugin.getMongoConnector();
        return literal("transferTest").requires(source -> source.getSender() instanceof Player).executes(context -> {
            CommandSourceStack source = context.getSource();
            Player player = source.getPlayerOrThrow();

            List<UUID> ids = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                ObjectNode testNode = JsonUtil.OBJECT_MAPPER.createObjectNode();
                UUID id = UUID.randomUUID();

                testNode.put("id", id.toString());
                testNode.put("username", "$i");
                testNode.put("rank", UserRank.NORMAL.name());

                testNode.putObject("stats").put("testhunt", 0);
                ids.add(id);
                mongoInteractor.interactor().set(MongoCollections.TESTING, testNode);
                player.sendMessage(Text.convert("<yellow><misc:bullet></yellow><green>Wrote '${id.toString()}' to mongo! <dark-gray>($i)</dark-gray>"));
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                List<MongoBatch> mongoBatches = new ArrayList<>();

                for (UUID id : ids) {
                    ObjectNode defaultNode = JsonUtil.OBJECT_MAPPER.createObjectNode();
                    MongoQueryKey updateKey = MongoQueryKey.id(id.toString());

                    defaultNode.put("rank", UserRank.values()[ThreadLocalRandom.current().nextInt(0, UserRank.values().length)].name());

                    defaultNode.putObject("stats")
                            .put("testhunt", ThreadLocalRandom.current().nextInt(25));

                    mongoBatches.add(new MongoBatch(updateKey, defaultNode));
                }
                mongoInteractor.interactor().batch(MongoCollections.TESTING, mongoBatches).whenComplete((result, _) -> {
                    if (result.failed())
                        player.sendMessage(Text.convert("<dark-red><misc:x></dark-red> <red>${result.message()}"));
                    BatchResult object = result.object();
                    player.sendMessage(Text.convert("<silver><misc:bullet></silver><yellow>Changed <blue>${object.itemsChanged()}</blue> out of <red>${object.items()}</red> items."));
                });

            }, 20 * 4);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (UUID id : ids) {
                    mongoInteractor.interactor().remove(MongoCollections.TESTING, MongoQueryKey.id(id.toString())).whenComplete((result, _) -> {
                        player.sendMessage(Text.convert("<purple><misc:bullet></purple> <yellow>${result.message()}</yellow>"));
                    });
                }
            }, 20 * 10);
            return 1;
        }).build();
    }

    public static LiteralCommandNode<CommandSourceStack> transferTest(MyriadBasePlugin plugin) {
        DatabaseConnection<BsonDatabaseInteractor> mongoInteractor = plugin.getMongoConnector();
        MessagingDatabaseConnection<LettuceDatabaseInteractor> redisConnector = plugin.getRedisConnector();
        return literal("transferTest").requires(source -> source.getSender() instanceof Player).executes(context -> {
            CommandSourceStack source = context.getSource();
            Player player = source.getPlayerOrThrow();

            ObjectNode testNode = JsonUtil.OBJECT_MAPPER.createObjectNode();

            UUID playerUuid = player.getUniqueId();
            testNode.put("id", playerUuid.toString());
            testNode.put("username", player.getName());
            testNode.put("rank", UserRank.NORMAL.name());
            testNode.putObject("stats").put("skywars", "0");

            mongoInteractor.post(new WriteJsonDocument(MongoCollections.TESTING, testNode)).whenComplete(((queryResult, _) ->
                    player.sendMessage(Text.convert("<gold><misc:bullet> <dark-green>${queryResult.message()}"))));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                mongoInteractor.post(new GetUserDocument(playerUuid)).whenComplete((result, _) -> {
                    player.sendMessage(Text.convert("<gold><misc:bullet> <dark-green>${result.message()}"));
                    JsonNode node = result.object();

                    redisConnector.post(new PostUserData(UUID.fromString(node.get("id").asText()), node)).whenComplete((redisResult, _) ->
                            player.sendMessage(Text.convert("<yellow><misc:bullet></yellow> <pink>${redisResult.message()}")));
                });
            }, 20 * 2);

            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    redisConnector.post(new RetrieveUserData(playerUuid)).whenComplete((result, _) -> {
                        if (result.failed()) {
                            player.sendMessage(Text.convert("<dark-red><misc:x></dark-red> <red>${result.message()}"));
                            return;
                        }
                        JsonNode node = result.object();
                        try {
                            player.sendMessage(JsonUtil.OBJECT_MAPPER.writeValueAsString(node));
                        } catch (JsonProcessingException e) {
                            player.sendMessage(Text.convert("<dark-red><misc:x></dark-red> <red>${e.getMessage()}"));
                        }
                    }), 20 * 6);
            return 1;
        }).build();
    }


}
