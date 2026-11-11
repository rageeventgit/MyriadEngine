package net.rageevent.myriad.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.database.MessagingDatabaseConnection;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.queries.redis.PostUserData;
import net.rageevent.myriad.api.database.queries.redis.RetrieveUserData;
import net.rageevent.myriad.api.player.UserRank;
import net.rageevent.myriad.api.server.LobbyType;
import net.rageevent.myriad.api.stats.BasePlayerStats;
import net.rageevent.myriad.json.JsonUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

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

    public LiteralCommandNode<CommandSourceStack> symbols() {
        return literal("testtext").requires(source -> source.getSender() instanceof Player).then(argument("text", greedyString()).executes(context -> {
            CommandSourceStack source = context.getSource();
            Player player = source.getPlayerOrThrow();

            String text = getString(context, "text");
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

            ObjectNode testNode = JsonUtil.OBJECT_MAPPER.createObjectNode();

            UUID playerUuid = player.getUniqueId();
            testNode.put("id", playerUuid.toString());
            testNode.put("username", player.getName());
            testNode.put("rank", UserRank.NORMAL.name());
            redisConnector.post(new PostUserData(playerUuid, testNode)).whenComplete(((result, error) -> {
                if (error != null) {
                    player.sendMessage(Text.convert("<red>${error.getMessage()}"));
                    return;
                }
                player.sendMessage(Text.convert("<aqua><misc:bullet></aqua> <green>${result.message()}"));
            }));

            redisConnector.post(new RetrieveUserData(playerUuid)).whenComplete((result, error) -> {
                if (error != null) {
                    player.sendMessage(Text.convert("<red>${error.getMessage()}"));
                    return;
                }
                player.sendMessage(Text.convert("<yellow><misc:bullet></yellow> <gold>Result:</gold> <green>${result.object()}"));
            });

            ObjectNode updatedNode = JsonUtil.OBJECT_MAPPER.createObjectNode();
            updatedNode.putObject("skywars").put("skywars_wins", 1120);

            try {
                player.sendMessage(Text.convert(JsonUtil.OBJECT_MAPPER.writeValueAsString(updatedNode)));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> interactor
                    .append(new QueryKey("user", playerUuid.toString()), "$.stats", updatedNode)
                    .whenComplete((result, error) -> {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (error != null) {
                                player.sendMessage(Text.convert("<red>${error.getMessage()}"));
                                return;
                            }
                            player.sendMessage(Text.convert("<dark-red><misc:bullet></dark-red> <red>Message:</red> <yellow>${result.message()}"));

                            redisConnector.post(new RetrieveUserData(playerUuid)).whenComplete((updatedResult, _) -> {
                                player.sendMessage(Text.convert("<yellow><misc:bullet></yellow> <gold>Updated Result:</gold> <green>${updatedResult.object()}"));
                            });
                        }, 0);
                    }), 20 * 2);

            Bukkit.getScheduler().runTaskLater(plugin, () -> interactor.remove(new QueryKey("user", playerUuid.toString())).whenComplete((result, error) -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (error != null) {
                        player.sendMessage(Text.convert("<red>${error.getMessage()}"));
                        return;
                    }
                    player.sendMessage(Text.convert("<dark-red><misc:bullet></dark-red> <red>Message:</red> <yellow>${result.message()}"));
                }, 0);
            }), 20 * 15);
            return 1;
        }).build();
    }


    public static LiteralCommandNode<CommandSourceStack> userTest(MyriadBasePlugin plugin) {
        MessagingDatabaseConnection<LettuceDatabaseInteractor> redisConnector = plugin.getRedisConnector();

        return literal("batchTest").requires(source -> source.getSender() instanceof Player).executes(context -> {
            CommandSourceStack source = context.getSource();
            Player player = source.getPlayerOrThrow();

            ObjectNode testNode = JsonUtil.OBJECT_MAPPER.createObjectNode();

            UUID playerUuid = player.getUniqueId();
            testNode.put("id", playerUuid.toString());
            testNode.put("username", player.getName());
            testNode.put("rank", UserRank.NORMAL.name());

            redisConnector.post(new PostUserData(playerUuid, testNode)).whenComplete((a, b) -> player.sendMessage(Text.convert(a.message())));
            redisConnector.post(new RetrieveUserData(playerUuid)).whenComplete((a, b) -> {
                try {
                    player.sendMessage(Text.convert(JsonUtil.OBJECT_MAPPER.writeValueAsString(a.object())));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
            BasePlayerStats.RANK.set(playerUuid, UserRank.ADMIN);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                UserRank rank = BasePlayerStats.RANK.get(playerUuid);
                player.sendMessage(Text.convert("<green>You are now").append(Component.text(rank.name(), rank.color())).append(Component.text(".", ThemeColor.GREEN)));
            }, 20 * 4);
            return 1;
        }).build();
    }


}
