package net.rageevent.myriad.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.shipments.network.player.TransferResult;
import net.rageevent.governor.api.shipments.network.server.SendToGroupRequest;
import net.rageevent.governor.api.shipments.queue.JoinQueueRequest;
import net.rageevent.governor.api.shipments.queue.QueueResult;
import net.rageevent.myriad.Text;
import net.rageevent.myriad.api.server.LobbyType;
import net.rageevent.myriad.api.server.ServerModeType;
import org.bukkit.entity.Player;

import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
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

    public LiteralCommandNode<CommandSourceStack> playNode(ControlUnit controlUnit) {
        return literal("play").requires(CommandChecks.isPlayer()).then(argument("gametype", string()).executes(context -> {
            Player player = context.getSource().getPlayerOrThrow();
            String gameType = getString(context, "gametype");

            Optional<ServerModeType> mode = ServerModeType.TYPES.stream().filter(s -> s.name().equalsIgnoreCase(gameType)).findFirst();

            if (mode.isEmpty()) {
                player.sendMessage(Text.convert("<yellow>This is not a valid game!"));
                player.playSound(Sound.sound(Key.key("entity.endermen.teleport"), Sound.Source.MASTER, 1, 1));
                return 0;
            }
            controlUnit.ship(new JoinQueueRequest(player.getUniqueId(), mode.get())).whenComplete((result, throwable) -> {
                if (throwable != null)
                    player.sendMessage(Text.convert("<red>Couldn't put you in queue"));
                else queueResult(result).ifPresent(player::sendMessage);
            });
            return 1;
        })).executes(context -> {
            context.getSource().getSender().sendMessage(Text.convert("<green>You must provide a type of game to play."));
            return 0;
        }).build();
    }

    private static Optional<Component> transferResult(TransferResult result) {
        return switch (result.status()) {
            case SUCCESS -> Optional.empty();
            case ALREADY_CONNECTED -> Optional.of(text("You are already connected to this server!", RED));
            case NO_SERVER_AVAILABLE -> Optional.of(text("There's no server available right now. Try again soon!", RED));
            case UNKNOWN_SERVER, NOT_ONLINE, FAILED -> Optional.of(text("Couldn't send you" + (result.server() != null ? " to " + result.server() : "") + (result.detail() != null ? ": " + result.detail() : "!"), RED));
        };
    }

    private Optional<Component> queueResult(QueueResult result) {
        final String gameName = "${result.queue().visualName()}";
        return switch (result.status()) {
            case JOINED -> Optional.of(Text.convert("<yellow>You are now queued for <purple>" + gameName + "</purple>."));
            case ALREADY_QUEUED -> Optional.of(Text.convert("<red>You are already queued for <blue>" + gameName + "</blue>."));
            case LEFT -> Optional.of(Text.convert("<green>Successfully left queue for <yellow>" + gameName + "</yellow>."));
            case NOT_QUEUED -> Optional.of(Text.convert("<yellow>You are currently not queued."));
            case UNKNOWN_QUEUE -> Optional.of(Text.convert("<red>Unknown queue type. <gray>(Please contact an admin.)"));
            case NOT_ONLINE -> Optional.of(Text.convert("<red>No player online."));
        };
    }
}
