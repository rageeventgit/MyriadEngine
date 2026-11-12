package net.rageevent.myriad.api.player.store;

import net.rageevent.myriad.api.server.ServerType;

public interface StatContainer extends SectionedDataContainer {
    ServerType gameType();

    @Override
    default String section() {
        return gameType().id();
    }

    @Override
    default String dotNotation() {
        return "stats." + SectionedDataContainer.super.dotNotation();
    }



}
