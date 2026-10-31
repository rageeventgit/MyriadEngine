package net.rageevent.myriad.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public enum GameType implements ServerType{

    /**
       Old game type name, please update to {@link GameType#BEAST_HUNT} references.
     */
    @Deprecated
    FLEE_THE_FACILITY("Flee the Facility", "BeastHunt"),

    BEAST_HUNT("Flee the Facility","BeastHunt")

    ;

    private final String visualName;
    private final String databaseKey;

    @Override
    public String id() {
        return name();
    }
}
