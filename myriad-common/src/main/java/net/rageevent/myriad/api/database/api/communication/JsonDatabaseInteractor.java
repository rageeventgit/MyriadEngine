package net.rageevent.myriad.api.database.api.communication;


import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.api.database.api.database.DatabaseInteractor;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

public interface JsonDatabaseInteractor extends DatabaseInteractor {

    CompletableFuture<QueryResult<Void>> jsonSet(QueryKey key, JsonNode value);

    CompletableFuture<QueryResult<Void>> jsonSet(QueryKey key,String path, JsonNode value);

    CompletableFuture<QueryResult<JsonNode>> jsonGet(QueryKey key);
    CompletableFuture<QueryResult<JsonNode>> jsonGet(QueryKey key, String path);

    CompletableFuture<QueryResult<Void>> append(QueryKey key,String path, JsonNode node);
    CompletableFuture<QueryResult<Void>> append(QueryKey key, JsonNode node);
    CompletableFuture<QueryResult<Void>> jsonRemove(QueryKey key, String path);
}
