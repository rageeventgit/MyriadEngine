package net.rageevent.myriad.extensions;

import dev.lotus.Extension;
import dev.lotus.This;
import net.rageevent.myriad.api.player.UserRank;
import net.rageevent.myriad.api.player.stats.BasePlayerStats;
import net.rageevent.myriad.api.player.store.DataContainer;
import org.bukkit.entity.Player;

@Extension(Player.class)
public class PlayerExtensions {

    public static UserRank getPlayerRank(@This Player player) {
        return BasePlayerStats.RANK.get(player.getUniqueId());
    }
    public static void setPlayerStat(@This Player player, DataContainer stat, Object value) {
        stat.set(player.getUniqueId(), value);
    }
    public static <T> T getPlayerStat(@This Player player, DataContainer stat, Class<T> typeClass){
        return typeClass.cast(stat.get(player.getUniqueId()));
    }
}
