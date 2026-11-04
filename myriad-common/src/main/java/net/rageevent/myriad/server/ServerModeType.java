package net.rageevent.myriad.server;

import net.rageevent.myriad.games.TypeInitializer;

import java.util.List;

public interface ServerModeType {

    String visualName();
    String name();
    GameType type();

    List<ServerModeType> TYPES = TypeInitializer.initializeTypes(ServerModeType.class);
}
