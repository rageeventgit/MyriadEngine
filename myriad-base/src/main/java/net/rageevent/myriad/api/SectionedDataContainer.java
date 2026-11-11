package net.rageevent.myriad.api;

import net.rageevent.myriad.api.server.ServerType;

public interface SectionedDataContainer extends DataContainer {
    String section();

    @Override
    default String dotNotation() {
        if(section() == null || section().isBlank())
            return databaseName();

        return section() + "." + databaseName();
    }
}
