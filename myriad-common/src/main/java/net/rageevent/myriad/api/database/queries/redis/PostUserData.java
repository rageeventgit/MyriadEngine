package net.rageevent.myriad.api.database.queries.redis;

import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.SequencedQuery;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record PostUserData(UUID player, JsonNode data) implements SequencedQuery<Void, LettuceDatabaseInteractor> {

    @Override
    public CompletableFuture<QueryResult<Void>> call(LettuceDatabaseInteractor interactor) {
        return interactor.jsonSet(new QueryKey("user",player.toString()), data);
    }

    @Override
    public UUID key() {
        return player;
    }
}
