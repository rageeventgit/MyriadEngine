package net.rageevent.myriad.queries;

import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.SequencedQuery;
import net.rageevent.myriad.api.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.PassedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;
import net.rageevent.myriad.api.player.store.DataContainer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record PlayerHasSectionQuery(UUID key, String section) implements SequencedQuery<Boolean, LettuceDatabaseInteractor> {

    public PlayerHasSectionQuery(UUID key, DataContainer container) {
        this(key, container.dotNotation());
    }

    @Override
    public CompletableFuture<QueryResult<Boolean>> call(LettuceDatabaseInteractor interactor) {
        return interactor.jsonGet(new QueryKey("user", key.toString()), section)
                .thenApplyAsync(result -> result.failed() ? new FailedQueryResult<>("No section found") : new PassedQueryResult<>("Found section."));
    }
}
