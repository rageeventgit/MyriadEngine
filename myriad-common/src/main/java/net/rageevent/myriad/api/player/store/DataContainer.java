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

        DataAccessor.connection.post(new SetUserDataQuery(uuid, this, newValue)).whenComplete(((voidQueryResult, throwable) ->
                System.out.println(voidQueryResult.message() + " FAILED: " + voidQueryResult.failed())));
    }

    default <T> T get(UUID uuid) {
        return (T) get(uuid, defaultType());
    }

    @SneakyThrows
    default <T> T get(UUID uuid, Class<T> typeClass) {
        QueryResult<Object> queryResult = DataAccessor.connection.post(new GetUserValueQuery<>(uuid, this)).get();
        if (queryResult.failed()) {
            return typeClass.cast(defaultValue());
        }
        Object value = queryResult.object();

        if (value == null)
            return typeClass.cast(defaultValue());

        if (codec() != null)
            return typeClass.cast(codec().deserialize(value.toString()));
        return typeClass.cast(value);
    }
}
