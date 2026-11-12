package net.rageevent.myriad.api.player.stats;

import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.api.player.store.DataContainer;
import net.rageevent.myriad.api.database.DataCodec;
import net.rageevent.myriad.api.database.DataCodecs;
import net.rageevent.myriad.api.player.UserRank;

import java.util.UUID;

@RequiredArgsConstructor
public enum BasePlayerStats implements DataContainer {

    UNIQUE_ID("key", UUID.class, null, DataCodecs.UUID),
    USERNAME(String.class, ""),
    RANK(UserRank.class, UserRank.NORMAL),
    ;
    private final String databaseName;
    private final Class<?> defaultType; // This will have codecs / serializes & deserializers to convert to the correct type.
    private final Object defaultValue;
    private final DataCodec codec;


    BasePlayerStats(Class<?> defaultType, Object defaultValue, DataCodec codec) {
        this("", defaultType, defaultValue, codec);
    }

    BasePlayerStats(Class<?> defaultType, Object defaultValue) {
        this("", defaultType, defaultValue, null);
    }

    @Override
    public String databaseName() {
        return databaseName.isBlank() ? name().toLowerCase() : databaseName.toLowerCase();
    }

    @Override
    public Class<?> defaultType() {
        return defaultType;
    }

    @Override
    public Object defaultValue() {
        return defaultValue;
    }

    @Override
    public DataCodec codec() {
        return codec;
    }
}
