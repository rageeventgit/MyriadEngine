package net.rageevent.myriad.database.queries.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.database.MongoCollections;
import net.rageevent.myriad.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.database.api.mongo.MongoQuery;
import net.rageevent.myriad.database.api.query.result.QueryResult;
import net.rageevent.myriad.database.mongo.MongoDataCollection;

import java.util.concurrent.CompletableFuture;

public record WriteTestDocument(JsonNode value) implements MongoQuery<Void> {

    @Override
    public MongoDataCollection collection() {
        return MongoCollections.TESTING;
    }

    @Override
    public CompletableFuture<QueryResult<Void>> call(BsonDatabaseInteractor interactor) {
        return interactor.set(collection(), value);
    }
}
