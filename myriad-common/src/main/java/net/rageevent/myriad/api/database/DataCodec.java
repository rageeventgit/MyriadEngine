package net.rageevent.myriad.api.database;

public interface DataCodec<T> {

    Object serialize(T object);
    T deserialize(String value);
}
