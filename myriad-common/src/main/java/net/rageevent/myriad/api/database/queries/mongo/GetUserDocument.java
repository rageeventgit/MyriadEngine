package net.rageevent.myriad.api.database.queries.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.api.database.MongoCollections;
import net.rageevent.myriad.api.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.api.database.api.mongo.MongoQuery;
import net.rageevent.myriad.api.database.api.mongo.MongoQueryKey;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;
import net.rageevent.myriad.api.database.mongo.MongoDataCollection;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record GetUserDocument(UUID player) implements MongoQuery<JsonNode> {

    @Override
    public MongoDataCollection collection() {
        return MongoCollections.PLAYERS;
    }

    @Override
    public CompletableFuture<QueryResult<JsonNode>> call(BsonDatabaseInteractor interactor) {
        return interactor.get(collection(), MongoQueryKey.of("_id", player.toString()));
    }
}
