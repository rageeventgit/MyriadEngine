package net.rageevent.myriad.api.codec;

import com.fasterxml.jackson.databind.JsonNode;

public interface DataCodec<T> {

    Object serialize(T object);

    T deserialize(String value);
}
