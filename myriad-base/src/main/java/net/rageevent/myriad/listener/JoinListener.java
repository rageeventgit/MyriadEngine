package net.rageevent.myriad.listener;

import lombok.RequiredArgsConstructor;
import net.rageevent.governor.api.ControlUnit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

@RequiredArgsConstructor
public class JoinListener implements Listener {

    private final ControlUnit controlUnit;


    //TODO: Add the ability to request just a string on the player data.
    @EventHandler
    public void onJoin(AsyncPlayerPreLoginEvent event){
        UUID uniqueId = event.getUniqueId();

    }

}
