package net.rageevent.myriad.api.database.queries.redis;

import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.api.database.api.communication.MessagingDatabaseInteractor;
import net.rageevent.myriad.api.database.api.query.Query;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record RetrieveUserData(UUID player) implements Query<JsonNode, MessagingDatabaseInteractor> {

    @Override
    public CompletableFuture<QueryResult<JsonNode>> call(MessagingDatabaseInteractor interactor) {
        return interactor.jsonGet(QueryKey.of("user:${player.toString()}"));
    }
}
