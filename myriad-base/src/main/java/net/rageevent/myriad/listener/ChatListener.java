package net.rageevent.myriad.listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.rageevent.myriad.Text;
import net.rageevent.myriad.ThemeColor;
import net.rageevent.myriad.api.player.UserRank;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener, ChatRenderer {

    @EventHandler
    public void onChatEvent(AsyncChatEvent event) {
        event.renderer(this);
    }

    @Override
    public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
        UserRank playerRank = source.getPlayerRank();

        if (playerRank == UserRank.NORMAL)
            return source.name().color(playerRank.color()).append(Component.text(": ").append(message)).color(playerRank.color());

        return Text
                .convert("<rank:${playerRank.name().toLowerCase()}>").appendSpace()
                .append(source.name().color(playerRank.color()))
                .append(Component.text(":", ThemeColor.SILVER).appendSpace()
                        .append(message));
    }
}
