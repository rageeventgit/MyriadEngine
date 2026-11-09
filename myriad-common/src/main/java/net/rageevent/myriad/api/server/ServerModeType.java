package net.rageevent.myriad.api.server;

import java.util.List;

public interface ServerModeType {

    String visualName();
    String name();
    GameType type();

    List<ServerModeType> TYPES = TypeInitializer.initializeTypes(ServerModeType.class);
}
