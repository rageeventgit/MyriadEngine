package net.rageevent.myriad.commands;


import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import lombok.experimental.UtilityClass;
import net.rageevent.myriad.Text;
import net.rageevent.myriad.api.player.stats.StaffPlayerStats;
import net.rageevent.myriad.api.player.store.DataContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

@UtilityClass
public class StaffCommandNodes {
    public LiteralCommandNode<CommandSourceStack> vanish() {
        return literal("vanish").requires(CommandChecks.isStaffRank()).executes(context -> {
            Player player = context.getSource().getPlayerOrThrow();
            DataContainer vanished = StaffPlayerStats.VANISHED;
            boolean newValue = !vanished.get(player.getUniqueId(), Boolean.class);

            if(newValue){
                player.sendMessage(Text.convert("<yellow>Vanished!"));
            }else
                player.sendMessage(Text.convert("<blue>Unvanished!"));

            vanished.set(player.getUniqueId(),newValue);
            return 1;
        }).build();
    }
    public LiteralCommandNode<CommandSourceStack> staffChat() {
        return literal("staffchat").requires(CommandChecks.isStaffRank()).executes(context -> {
            Player player = context.getSource().getPlayerOrThrow();
            //TODO: Make a broadcast message request
            return 1;
        }).build();
    }
}
