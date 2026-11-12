package net.rageevent.myriad.api.player.store;

import lombok.SneakyThrows;
import net.rageevent.myriad.api.database.DataCodec;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;
import net.rageevent.myriad.api.queries.redis.user.GetUserValueQuery;
import net.rageevent.myriad.api.queries.redis.user.SetUserDataQuery;

import java.util.UUID;

public interface DataContainer {

    String databaseName();

    Class<?> defaultType();

    default Object defaultValue() {
        return null;
    }

    default String dotNotation() {
        return databaseName();
    }

    default DataCodec codec() {
        return null;
    }

    default void set(UUID uuid, Object value) {
        Object newValue = value;
        if (codec() != null)
            newValue = codec().serialize(value);
        DataAccessor.connection.post(new SetUserDataQuery(uuid, this, newValue));
    }

    @SneakyThrows
    default <T> T get(UUID uuid) {
        QueryResult<Object> queryResult = DataAccessor.connection.post(new GetUserValueQuery<>(uuid, this)).get();
        System.out.println(dotNotation());
        if(queryResult.failed()) {
            System.out.println(queryResult.message());
            return null;
        }

        Object value = queryResult.object();
        if(codec() != null)
            return (T) defaultType().cast(codec().deserialize(value.toString()));
        return (T) defaultType().cast(value);
    }
}
