package net.rageevent.myriad.api.database.queries.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.api.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.api.database.api.mongo.MongoQuery;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;
import net.rageevent.myriad.api.database.mongo.MongoDataCollection;

import java.util.concurrent.CompletableFuture;

public record WriteJsonDocument(MongoDataCollection collection, JsonNode value) implements MongoQuery<Void> {

    @Override
    public MongoDataCollection collection() {
        return collection;
    }

    @Override
    public CompletableFuture<QueryResult<Void>> call(BsonDatabaseInteractor interactor) {
        return interactor.set(collection(), value);
    }
}
