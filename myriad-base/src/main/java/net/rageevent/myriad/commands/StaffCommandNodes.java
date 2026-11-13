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

    public LiteralCommandNode<CommandSourceStack> launch() {
        return literal("launch").requires(CommandChecks.isStaffRank())
                .then(literal("all").executes(context -> {
                    CommandSourceStack source = context.getSource();

                    Player player = source.getPlayerOrThrow();
                    for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
                        launchEntity(player, otherPlayer);
                    }

                    return 1;
                }))
                .then(argument("target", ArgumentTypes.entities()).executes(context -> {
                    CommandSourceStack source = context.getSource();
                    final EntitySelectorArgumentResolver resolver = context.getArgument("target", EntitySelectorArgumentResolver.class);
                    Player player = source.getPlayerOrThrow();
                    for (Entity entity : resolver.resolve(source)) {
                        launchEntity(player, entity);

                    }
                    return 1;
                }))
                .build();
    }

    private void launchEntity(Player player, Entity entity) {
        entity.setVelocity(entity.getVelocity().add(new Vector(0, 10, 0)));
        player.sendMessage(Text.convert("<green>Launched ${entity.getName()}"));
    }
}
