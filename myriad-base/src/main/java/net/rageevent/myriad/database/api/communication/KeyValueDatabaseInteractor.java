package net.rageevent.myriad.database.api.communication;


import net.rageevent.myriad.database.api.database.DatabaseInteractor;
import net.rageevent.myriad.database.api.query.QueryKey;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

public interface KeyValueDatabaseInteractor extends DatabaseInteractor {

    <T> CompletableFuture<QueryResult<Void>> set(QueryKey key, T value);

    <R> CompletableFuture<QueryResult<R>> get(QueryKey key);

    CompletableFuture<QueryResult<Void>> remove(QueryKey key);

    CompletableFuture<Boolean> exists(QueryKey key);
}
