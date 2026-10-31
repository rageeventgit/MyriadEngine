package net.rageevent.myriad.format;

import net.kyori.adventure.text.format.TextColor;

public interface TextFormatHolder {

    String symbol();
    String name();

    /**
      To determine if a symbol has any specific text color needed, it cannot be overwritten.
     */
    default TextColor color() {
        return null;
    }

    /**
     * When tags are added, they will be categorized if valid. See {@link Symbol#MISC_X} and {@link net.rageevent.myriad.player.UserRank}
     */
    default boolean categorized() {
        return false;
    }

}
