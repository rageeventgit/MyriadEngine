package net.rageevent.myriad;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.format.TextColor;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum UserRank {

    NORMAL("", ThemeColor.GRAY),

    FORAGER("\uD83E\uDE93",ThemeColor.DARK_GREEN),
    COLLECTOR("⛏",ThemeColor.LIGHT_GREEN),

    ANCHOR("⚓",ThemeColor.BLUE),
    CAPTAIN("⛵",ThemeColor.DARK_BLUE),

    YOUTUBER("\uD83D\uDCBE",ThemeColor.GOLD),
    ADMIN("\uD83D\uDD79",ThemeColor.RED),

    ;
    private final String symbol;
    private final TextColor color;
}
