package net.rageevent.myriad.listener;

import lombok.RequiredArgsConstructor;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.shipments.network.player.data.PullUserData;
import net.rageevent.myriad.MyriadBasePlugin;
import net.rageevent.myriad.api.player.UserRank;
import net.rageevent.myriad.api.player.stats.BasePlayerStats;
import net.rageevent.myriad.api.player.stats.StaffPlayerStats;
import net.rageevent.myriad.queries.PlayerHasSectionQuery;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

@RequiredArgsConstructor
public class JoinListener implements Listener {

    private final MyriadBasePlugin plugin;
    private final ControlUnit controlUnit;

    //TODO: Add the ability to request just a string on the player data.
    @EventHandler
    public void onJoin(AsyncPlayerPreLoginEvent event) {
        UUID player = event.getUniqueId();
        if (BasePlayerStats.RANK.get(player, UserRank.class) != UserRank.ADMIN) return;
        plugin.getRedisConnector().post(new PlayerHasSectionQuery(player, StaffPlayerStats.VANISHED.section())).whenComplete((result, _) -> {
            if (result.failed())
                controlUnit.ship(new PullUserData(player, StaffPlayerStats.VANISHED.section(), false));
        });
    }
}
