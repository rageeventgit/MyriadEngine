package net.rageevent.myriad.api.format;

import net.kyori.adventure.text.format.TextColor;
import net.rageevent.myriad.api.player.UserRank;

public interface TextFormatHolder {

    String symbol();
    String dub();

    /**
      To determine if a symbol has any specific text color needed, it cannot be overwritten.
     */
    default TextColor color() {
        return null;
    }

    /**
     * When tags are added, they will be categorized if valid. See {@link Symbol#MISC_X} and {@link UserRank}
     */
    default boolean categorized() {
        return false;
    }
    
}
