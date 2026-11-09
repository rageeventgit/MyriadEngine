package net.rageevent.myriad.api.server;

import java.util.List;

public interface ServerType {

    String visualName();
    String id();

    List<ServerType> TYPES = TypeInitializer.initializeTypes(ServerType.class);
}
