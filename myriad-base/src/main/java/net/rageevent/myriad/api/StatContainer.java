package net.rageevent.myriad.api;

import net.rageevent.myriad.server.ServerType;

//Has stats based on game types.
public interface StatContainer extends DataContainer{
    ServerType gameType();
}
