package net.rageevent.myriad.api.database;

import lombok.experimental.UtilityClass;
import net.rageevent.myriad.api.player.UserRank;

import java.util.UUID;

@UtilityClass
public class DataCodecs {

    public final DataCodec<UUID> UUID = new DataCodec<>() {
        @Override
        public Object serialize(UUID object) {
            return object.toString();
        }

        @Override
        public UUID deserialize(String value) {
            return java.util.UUID.fromString(value);
        }
    };
    public final DataCodec<UserRank> USER_RANK = new DataCodec<>() {
        @Override
        public Object serialize(UserRank object) {
            return object.name();
        }

        @Override
        public UserRank deserialize(String value) {
            return UserRank.valueOf(value);
        }
    };

}
