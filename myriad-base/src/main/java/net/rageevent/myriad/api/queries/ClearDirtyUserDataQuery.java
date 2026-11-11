package net.rageevent.myriad.api.queries;

import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.SequencedQuery;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record ClearDirtyUserDataQuery(UUID key) implements SequencedQuery<Void, LettuceDatabaseInteractor> {

    @Override
    public CompletableFuture<QueryResult<Void>> call(LettuceDatabaseInteractor interactor) {
        return interactor.remove(new QueryKey("user:dirty", key.toString()));
    }
}
