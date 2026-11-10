package net.rageevent.myriad.api.database.api.mongo;

import com.fasterxml.jackson.databind.JsonNode;

public record MongoBatch(MongoQueryKey filter, JsonNode updated) {
}
