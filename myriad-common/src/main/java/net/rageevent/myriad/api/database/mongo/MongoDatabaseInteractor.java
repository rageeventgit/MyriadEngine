package net.rageevent.myriad.api.database.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.rageevent.myriad.api.database.JsonUtil;
import net.rageevent.myriad.api.database.api.mongo.BatchResult;
import net.rageevent.myriad.api.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.api.database.api.mongo.MongoBatch;
import net.rageevent.myriad.api.database.api.mongo.MongoQueryKey;
import net.rageevent.myriad.api.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.PassedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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


    //TODO: Fix this, it will override everything.
    @SneakyThrows
    @Override
    public CompletableFuture<QueryResult<Void>> update(MongoDataCollection collection, MongoQueryKey filter, JsonNode changed) {
        MongoCollection<Document> mongoCollection = collection(collection);
        Document filterDocument = new Document(filter.key(), filter.value());
        return CompletableFuture.supplyAsync(() -> {
            UpdateResult updateResult = mongoCollection.updateOne(filterDocument, toDocument(changed));
            if (updateResult.getMatchedCount() == 0 || updateResult.getModifiedCount() == 0)
                return new FailedQueryResult<>("Failed to update any documents.");
            return new PassedQueryResult<>("Updated document");
        });
    }

    @Override
    public CompletableFuture<QueryResult<BatchResult>> batch(MongoDataCollection collection, List<MongoBatch> updatedBatches) {
        MongoCollection<Document> mongoCollection = collection(collection);
        return CompletableFuture.supplyAsync(() -> {
            if (updatedBatches.isEmpty())
                return new FailedQueryResult<>("No documents to modify.");

            List<WriteModel<Document>> updatesList = new ArrayList<>();
            for (MongoBatch mongoBatch : updatedBatches) {
                MongoQueryKey filter = mongoBatch.filter();
                JsonNode updated = mongoBatch.updated();

                String updatedJson;
                try {
                    updatedJson = JsonUtil.OBJECT_MAPPER.writeValueAsString(updated);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                Document changedDataDocument = Document.parse(updatedJson);

                Document filterDocument = new Document(filter.key(), filter.value());
                Document flattenedDocument = new Document();

                flatten("", changedDataDocument, flattenedDocument);

                //Lotus is bugging out the operation, so I have to do this...
                Document updateOperation = new Document("$" + "set", flattenedDocument);
                updatesList.add(new UpdateOneModel<>(filterDocument, updateOperation));
            }
            BulkWriteResult bulkWriteResult = mongoCollection.bulkWrite(updatesList);

            if (bulkWriteResult.getModifiedCount() == 0)
                return new FailedQueryResult<>("Failed to modify documents.");

            return new PassedQueryResult<>(new BatchResult(updatedBatches.size(), bulkWriteResult.getModifiedCount()));
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
                return new PassedQueryResult<>(fromMongoNode(JsonUtil.OBJECT_MAPPER.readValue(docJson, JsonNode.class)));
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
        return Document.parse(JsonUtil.OBJECT_MAPPER.writeValueAsString(toMongoNode(value)));
    }

    private MongoCollection<Document> collection(MongoDataCollection collection) {
        return mongoDatabase.getCollection(collection.databaseName());
    }

    private JsonNode toMongoNode(JsonNode rawNode) {
        if (!rawNode.has("id")) return rawNode; //Let mongo generate one on its own.
        ObjectNode modifiedNode = (ObjectNode) rawNode;
        JsonNode idNode = rawNode.get("id");
        modifiedNode.remove("id");
        return modifiedNode.set("_id", idNode);
    }

    private JsonNode fromMongoNode(JsonNode rawNode) {
        if (!rawNode.has("_id")) return rawNode; //Don't know how this would be possible.
        ObjectNode modifiedNode = (ObjectNode) rawNode;
        JsonNode idNode = rawNode.get("_id");
        modifiedNode.remove("_id");
        return modifiedNode.set("id", idNode);
    }

    private void flatten(String prefix, Document current, Document result) {
        for (Map.Entry<String, Object> entry : current.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            String newKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof Document)
                flatten(newKey, (Document) value, result);
            else
                result.put(newKey, value);
        }
    }
}
