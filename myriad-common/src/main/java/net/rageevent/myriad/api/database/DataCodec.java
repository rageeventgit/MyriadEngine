package net.rageevent.myriad.api.database;

public interface DataCodec {

    Object serialize(Object object);
    Object deserialize(String value);
}
