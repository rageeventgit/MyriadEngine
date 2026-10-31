package net.rageevent.myriad.format;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum Symbol implements TextFormatHolder {

    UI_MOUSE("\uD83D\uDDB1"),

    MISC_BULLET("•"),

    MISC_CHECKMARK("✔"),
    MISC_X("✖"),

    CHAT_LINE("―"),
    ;
    private final String symbol;

    @Override
    public boolean categorized() {
        return true;
    }
}
