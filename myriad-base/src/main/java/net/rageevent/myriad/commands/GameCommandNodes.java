package net.rageevent.myriad.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.shipments.network.player.TransferResult;
import net.rageevent.governor.api.shipments.network.server.SendToGroupRequest;
import net.rageevent.myriad.Text;
import net.rageevent.myriad.server.LobbyType;
import org.bukkit.entity.Player;

import java.util.Optional;

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
                        if (error != null)
                            player.sendMessage(Text.convert("<red>Couldn't send you to the lobby."));
                        else
                            transferResult(result).ifPresent(player::sendMessage);
                    });
                    return 1;
                })
                .build();
    }

    private static Optional<Component> transferResult(TransferResult result) {
        return switch (result.status()) {
            case SUCCESS -> Optional.empty();
            case ALREADY_CONNECTED -> Optional.of(text("You are already connected to this server!", RED));
            case NO_SERVER_AVAILABLE ->
                    Optional.of(text("There's no server available right now. Try again soon!", RED));
            case UNKNOWN_SERVER, NOT_ONLINE, FAILED -> Optional.of(text("Couldn't send you"
                    + (result.server() != null ? " to " + result.server() : "")
                    + (result.detail() != null ? ": " + result.detail() : "!"), RED));
        };
    }
}
