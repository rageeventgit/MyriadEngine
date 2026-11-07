package net.rageevent.myriad.api;

import net.rageevent.myriad.server.ServerType;

public interface DataContainer {

    String databaseName();
    Class<?> defaultType();
}
