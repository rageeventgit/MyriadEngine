package net.rageevent.myriad.api.database.api.mongo;

import org.jspecify.annotations.NonNull;

public record MongoQueryKey(String key, Object value) {

    public static MongoQueryKey of(String key,Object value) {
        return new MongoQueryKey(key,value);
    }
    public static MongoQueryKey id(Object value) {
        return new MongoQueryKey("_id",value);
    }
    public @NonNull String namespaced() {
        return key + ":" + value;
    }

    public static MongoQueryKey singlet(String key) {
        return new MongoQueryKey(key, null);
    }
}
