package net.rageevent.myriad.api.database;

import lombok.experimental.UtilityClass;
import net.rageevent.myriad.api.player.UserRank;

import java.util.UUID;

@UtilityClass
public class DataCodecs {

    public final DataCodec UUID = new DataCodec() {
        @Override
        public Object serialize(Object object) {
            return object.toString();
        }

        @Override
        public UUID deserialize(String value) {
            return java.util.UUID.fromString(value);
        }
    };
    public final DataCodec USER_RANK = new DataCodec() {
        @Override
        public Object serialize(Object object) {
            return ((UserRank) object).name();
        }

        @Override
        public UserRank deserialize(String value) {
            return UserRank.valueOf(value);
        }
    };

}
