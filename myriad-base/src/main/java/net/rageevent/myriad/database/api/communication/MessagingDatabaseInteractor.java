package net.rageevent.myriad.database.api.communication;


import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.database.api.query.QueryKey;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

public interface MessagingDatabaseInteractor extends KeyValueDatabaseInteractor {

    <T> CompletableFuture<QueryResult<Long>> publish(QueryKey channel, T raw, Serializer<T> serializer);
    <T> CompletableFuture<QueryResult<T>> message(QueryKey channel, String json);

    CompletableFuture<QueryResult<Void>> jsonSet(QueryKey key, JsonNode value);
    CompletableFuture<QueryResult<JsonNode>> jsonGet(QueryKey key);

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
