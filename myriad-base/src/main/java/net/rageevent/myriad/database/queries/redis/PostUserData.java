package net.rageevent.myriad.database.queries.redis;

import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.database.api.communication.MessagingDatabaseInteractor;
import net.rageevent.myriad.database.api.query.Query;
import net.rageevent.myriad.database.api.query.QueryKey;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

public record PostUserData(QueryKey key, JsonNode data) implements Query<Void, MessagingDatabaseInteractor> {

    @Override
    public CompletableFuture<QueryResult<Void>> call(MessagingDatabaseInteractor interactor) {
        return interactor.jsonSet(key, data);
    }
}
