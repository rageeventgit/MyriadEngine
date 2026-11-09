package net.rageevent.myriad.api.database.api.communication;

public interface Serializer<T> {
    String serialize(T data);
    T deserialize(String data);
}
