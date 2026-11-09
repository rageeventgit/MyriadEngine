package net.rageevent.myriad.api.database.api.mongo;

import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.api.database.api.database.DatabaseInteractor;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;
import net.rageevent.myriad.api.database.mongo.MongoDataCollection;

import java.util.concurrent.CompletableFuture;

public interface BsonDatabaseInteractor extends DatabaseInteractor {

    CompletableFuture<QueryResult<Void>> set(MongoDataCollection collection, JsonNode value);

    CompletableFuture<QueryResult<Void>> update(MongoDataCollection collection, MongoQueryKey filter, JsonNode value);

    CompletableFuture<QueryResult<BatchResult>> batch(MongoDataCollection collection);

    CompletableFuture<QueryResult<JsonNode>> get(MongoDataCollection collection, MongoQueryKey key);

    CompletableFuture<QueryResult<Void>> remove(MongoDataCollection collection, MongoQueryKey key);
}
