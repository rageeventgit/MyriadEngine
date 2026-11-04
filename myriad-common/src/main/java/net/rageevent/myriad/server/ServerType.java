package net.rageevent.myriad.server;

import net.rageevent.myriad.games.TypeInitializer;

import java.util.List;

public interface ServerType {

    String visualName();
    String id();

    List<ServerType> TYPES = TypeInitializer.initializeTypes(ServerType.class);
}
