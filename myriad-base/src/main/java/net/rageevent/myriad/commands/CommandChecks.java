package net.rageevent.myriad.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.experimental.UtilityClass;
import net.rageevent.myriad.api.player.UserRank;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

@UtilityClass
public class CommandChecks {

    public Predicate<CommandSourceStack> isPlayer() {
        return stack -> stack.getSender() instanceof Player;
    }
    public Predicate<CommandSourceStack> isStaffRank() {
        return stack -> {
            if(!(stack.getSender() instanceof Player player)) return false;
            return player.getPlayerRank() == UserRank.ADMIN;
        };
    }
}
