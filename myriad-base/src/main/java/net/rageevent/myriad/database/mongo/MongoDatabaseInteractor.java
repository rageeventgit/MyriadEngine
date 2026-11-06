package net.rageevent.myriad.database.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.rageevent.myriad.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.database.api.mongo.MongoQueryKey;
import net.rageevent.myriad.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.database.api.query.result.PassedQueryResult;
import net.rageevent.myriad.database.api.query.result.QueryResult;
import net.rageevent.myriad.json.JsonUtil;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

@RequiredArgsConstructor
public class MongoDatabaseInteractor implements BsonDatabaseInteractor {

    private final MongoDatabase mongoDatabase;

    private final ExecutorService mongoExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));


    @Override
    public void shutdown() {
        mongoExecutor.shutdown();
        try {
            mongoExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @Override
    public CompletableFuture<QueryResult<Void>> set(MongoDataCollection collection, JsonNode value) {
        MongoCollection<Document> mongoCollection = collection(collection);
        return CompletableFuture.supplyAsync(() -> {
            InsertOneResult oneResult = mongoCollection.insertOne(toDocument(value));
            return oneResult.wasAcknowledged() ? new PassedQueryResult<>("Successfully wrote document") : new FailedQueryResult<>("Failed to write document");
        }, mongoExecutor);
    }

    @SneakyThrows
    @Override
    public CompletableFuture<QueryResult<Void>> update(MongoDataCollection collection, MongoQueryKey filter, JsonNode value) {
        MongoCollection<Document> mongoCollection = collection(collection);
        Document filterDocument = new Document(filter.key(), filter.value());
        return CompletableFuture.supplyAsync(() -> {
            UpdateResult updateResult = mongoCollection.updateOne(filterDocument, toDocument(value));

            if (updateResult.getMatchedCount() == 0 || updateResult.getModifiedCount() == 0)
                return new FailedQueryResult<>("Failed to update any documents.");
            return new PassedQueryResult<>("Updated document");
        });
    }

    @Override
    public CompletableFuture<QueryResult<JsonNode>> get(MongoDataCollection collection, MongoQueryKey key) {
        String base = key.key();
        Object value = key.value();

        MongoCollection<Document> mongoCollection = collection(collection);
        return CompletableFuture.supplyAsync(() -> {
            Document firstFound = mongoCollection.find(eq(base, value)).first();

            if (firstFound == null)
                return new FailedQueryResult<>("Couldn't find document with key '${base}' filterKey '${value}'");

            String docJson = firstFound.toJson();

            try {
                return new PassedQueryResult<>(JsonUtil.OBJECT_MAPPER.readValue(docJson, JsonNode.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, mongoExecutor);
    }

    @Override
    public CompletableFuture<QueryResult<Void>> remove(MongoDataCollection collection, MongoQueryKey key) {
        String base = key.key();
        Object value = key.value();

        MongoCollection<Document> mongoCollection = collection(collection);
        Document filter = new Document(base, value);
        return CompletableFuture.supplyAsync(() -> {
            DeleteResult deleted = mongoCollection.deleteOne(filter);

            if (deleted.getDeletedCount() == 0)
                return new FailedQueryResult<>("No document found to delete.");

            return new PassedQueryResult<>("Successfully deleted document.");
        });
    }


    @SneakyThrows
    private Document toDocument(JsonNode value) {
        return Document.parse(JsonUtil.OBJECT_MAPPER.writeValueAsString(fixNode(value)));
    }

    private MongoCollection<Document> collection(MongoDataCollection collection) {
        return mongoDatabase.getCollection(collection.databaseName());
    }
    private JsonNode fixNode(JsonNode rawNode){
        if(!rawNode.has("id")) return rawNode; //Let mongo generate one on its own.
        ObjectNode modifiedNode = (ObjectNode) rawNode;
        JsonNode idNode = rawNode.get("id");
        modifiedNode.remove("id");
        return modifiedNode.set("_id",idNode);
    }
}
