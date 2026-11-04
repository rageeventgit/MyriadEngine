package net.rageevent.myriad.games.beasthunt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.rageevent.myriad.server.GameType;
import net.rageevent.myriad.server.ServerModeType;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum BeastHuntModeType implements ServerModeType {

    BEAST_CLASSIC("Beast Hunt", GameType.BEAST_HUNT);

    private final String visualName;
    private final GameType type;
}
