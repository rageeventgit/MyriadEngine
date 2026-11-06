package net.rageevent.myriad.database.api.communication;


import net.rageevent.myriad.database.api.query.QueryKey;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

public interface MessagingDatabaseInteractor extends KeyValueDatabaseInteractor {

    <T> CompletableFuture<QueryResult<Long>> publish(QueryKey channel, T raw, Serializer<T> serializer);
    <T> CompletableFuture<QueryResult<T>> message(QueryKey channel, String json);

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
    default CompletableFuture<Void> remove(QueryKey key){
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
