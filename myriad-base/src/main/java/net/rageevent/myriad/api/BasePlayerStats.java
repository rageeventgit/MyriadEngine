package net.rageevent.myriad.api;

import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.player.UserRank;

import java.util.UUID;

@RequiredArgsConstructor
public enum BasePlayerStats implements DataContainer {

    UNIQUE_ID("uuid", UUID.class),
    USERNAME(String.class),
    RANK(UserRank.class),
    ;
    private final String databaseName;
    private final Class<?> defaultType; // This will have codecs / serializes & deserializers to convert to the correct type.

    BasePlayerStats(Class<?> defaultType) {
        this("", defaultType);
    }

    @Override
    public String databaseName() {
        return databaseName.isBlank() ? name().toLowerCase() : databaseName.toLowerCase();
    }

    @Override
    public Class<?> defaultType() {
        return defaultType;
    }

}
