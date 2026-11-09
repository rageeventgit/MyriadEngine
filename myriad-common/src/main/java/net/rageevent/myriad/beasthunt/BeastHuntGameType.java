package net.rageevent.myriad.beasthunt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.rageevent.myriad.api.server.GameType;
import net.rageevent.myriad.api.server.ServerModeType;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum BeastHuntGameType implements ServerModeType {

    BEAST_CLASSIC("Beast Hunt", GameType.BEAST_HUNT),
    ;

    private final String visualName;
    private final GameType type;

}
