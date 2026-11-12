package net.rageevent.myriad.api.player.stats;

import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.api.player.store.SectionedDataContainer;
import net.rageevent.myriad.api.player.UserRank;

@RequiredArgsConstructor
public enum StaffPlayerStats implements SectionedDataContainer {

    VANISHED(Boolean.class,false),
    NICKED(Boolean.class,false),

    NICKNAME(String.class,""){
        @Override public String section() {
            return super.section() + ".nick";
        }
    },
    PLAYER_SKIN(String.class,""){
        @Override public String section() {
            return super.section() + ".nick";
        }
    },
    FALSE_RANK(UserRank.class,""){
        @Override public String section() {
            return super.section() + ".nick";
        }
    }

    ;
    private final String databaseName;
    private final Class<?> defaultType;
    private final Object defaultValue;

    StaffPlayerStats(Class<?> defaultType, Object defaultValue) {
        this("", defaultType, defaultValue);
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
    public String section() {
        return "staff";
    }
}
