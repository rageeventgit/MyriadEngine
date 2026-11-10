package net.rageevent.myriad.api.database.api.communication;


import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

public interface LettuceDatabaseInteractor extends KeyValueDatabaseInteractor {

    <T> CompletableFuture<QueryResult<Long>> publish(QueryKey channel, T raw, Serializer<T> serializer);
    <T> CompletableFuture<QueryResult<T>> message(QueryKey channel, String json);

    CompletableFuture<QueryResult<Void>> jsonSet(QueryKey key, JsonNode value);
    CompletableFuture<QueryResult<JsonNode>> jsonGet(QueryKey key);
    CompletableFuture<QueryResult<JsonNode>> jsonGet(QueryKey key, String path);
    CompletableFuture<QueryResult<Void>> append(QueryKey key, JsonNode node);
    CompletableFuture<QueryResult<Void>> jsonRemove(QueryKey key, String path);

    @Override
    default <T> CompletableFuture<QueryResult<Void>> set(QueryKey key, T value){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    default <R> CompletableFuture<QueryResult<R>> get(QueryKey key){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    default CompletableFuture<Boolean> exists(QueryKey key){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    default CompletableFuture<QueryResult<Void>> remove(QueryKey key){
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
