package net.rageevent.myriad.listener;

import lombok.RequiredArgsConstructor;
import net.rageevent.governor.api.ControlUnit;
import net.rageevent.governor.api.shipments.network.player.data.PullUserData;
import net.rageevent.myriad.MyriadBasePlugin;
import net.rageevent.myriad.api.player.UserRank;
import net.rageevent.myriad.api.player.stats.BasePlayerStats;
import net.rageevent.myriad.api.player.stats.StaffPlayerStats;
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
        controlUnit.ship(new PullUserData(player, StaffPlayerStats.VANISHED.section())).whenComplete((userDataResponse, throwable) -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if(throwable != null)
                    System.out.println(throwable.getMessage() + ": " + throwable.getClass().getSimpleName());
                System.out.println(userDataResponse.message());
            }, 0);
        });
    }
}
