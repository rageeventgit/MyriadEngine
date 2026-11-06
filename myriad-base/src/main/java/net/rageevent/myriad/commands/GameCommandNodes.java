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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.shipments.network.player.TransferResult;
import net.rageevent.governor.api.shipments.network.server.SendToGroupRequest;
import net.rageevent.myriad.MyriadBasePlugin;
import net.rageevent.myriad.Text;
import net.rageevent.myriad.ThemeColor;
import net.rageevent.myriad.database.MongoCollections;
import net.rageevent.myriad.database.api.communication.MessagingDatabaseInteractor;
import net.rageevent.myriad.database.api.database.DatabaseConnection;
import net.rageevent.myriad.database.api.database.MessagingDatabaseConnection;
import net.rageevent.myriad.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.database.api.mongo.MongoQueryKey;
import net.rageevent.myriad.database.api.query.QueryKey;
import net.rageevent.myriad.database.queries.mongo.GetTestDocument;
import net.rageevent.myriad.database.queries.mongo.WriteTestDocument;
import net.rageevent.myriad.json.JsonUtil;
import net.rageevent.myriad.player.UserRank;
import net.rageevent.myriad.server.LobbyType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static net.kyori.adventure.text.Component.text;
import static net.rageevent.myriad.ThemeColor.BLUE;
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
        MessagingDatabaseConnection<MessagingDatabaseInteractor> redisConnector = plugin.getRedisConnector();
        return literal("redistest").requires(source -> source.getSender() instanceof Player).executes(context -> {
            CommandSourceStack source = context.getSource();
            Player player = source.getPlayerOrThrow();
            MessagingDatabaseInteractor interactor = redisConnector.interactor();

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


    public static LiteralCommandNode<CommandSourceStack> mongoTest(MyriadBasePlugin plugin) {
        DatabaseConnection<BsonDatabaseInteractor> mongoInteractor = plugin.getMongoConnector();
        return literal("mongotest").requires(source -> source.getSender() instanceof Player).executes(context -> {
            CommandSourceStack source = context.getSource();
            Player player = source.getPlayerOrThrow();

            ObjectNode testNode = JsonUtil.OBJECT_MAPPER.createObjectNode();

            String playerUuid = player.getUniqueId().toString();
            testNode.put("id", playerUuid);
            testNode.put("username", player.getName());
            testNode.put("rank", UserRank.NORMAL.name());
            testNode.putObject("stats").put("skywars","0");

            mongoInteractor.post(new WriteTestDocument(testNode)).whenComplete(((queryResult, _) ->
                    player.sendMessage(Text.convert("<gold><misc:bullet> <dark-green>${queryResult.message()}"))));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                mongoInteractor.post(new GetTestDocument(MongoQueryKey.id(playerUuid))).whenComplete((result, _) -> {
                    player.sendMessage(Text.convert("<gold><misc:bullet> <dark-green>${result.message()}"));
                    JsonNode node = result.object();
                    try {
                        player.sendMessage(Text.convert(JsonUtil.OBJECT_MAPPER.writeValueAsString(node)));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
            },20*2);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                mongoInteractor.interactor().remove(MongoCollections.TESTING,MongoQueryKey.id(playerUuid)).whenComplete((result, _) ->
                        player.sendMessage(Text.convert("<blue><misc:bullet> <pink>${result.message()}")));
            },20*10);
            return 1;
        }).build();
    }
}
