package net.rageevent.myriad.listener;


import dev.quartz.paper.QuartzPaper;
import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.MyriadBasePlugin;
import net.rageevent.myriad.api.database.mongo.MongoConnectionInfo;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

@RequiredArgsConstructor
public class PluginListener implements Listener {

    private final MyriadBasePlugin plugin;

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        Plugin plugin = event.getPlugin();
        if (!plugin.getName().equalsIgnoreCase("Quartz")) return;

        QuartzPaper quartzPaper = QuartzPaper.get();

        quartzPaper.provideMongo(
                new MongoConnectionInfo("mongodb://127.0.0.1:27017/",
                        "fern",
                        "admin",
                        System.getenv("MONGO_PASSWORD"))
                        .toMongoUrl(), "fern");
        Bukkit.getServer().getServicesManager().register(QuartzPaper.class, quartzPaper, plugin, ServicePriority.High);
    }

}
