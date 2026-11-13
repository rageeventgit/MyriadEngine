package net.rageevent.myriad.api.queries.redis.user;

import lombok.SneakyThrows;
import net.rageevent.myriad.api.database.JsonUtil;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.SequencedQuery;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;
import net.rageevent.myriad.api.player.store.DataContainer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record SetUserDataQuery(UUID key, DataContainer container,
                               Object value) implements SequencedQuery<Void, LettuceDatabaseInteractor> {

    @SneakyThrows
    @Override
    public CompletableFuture<QueryResult<Void>> call(LettuceDatabaseInteractor interactor) {
        return interactor.append(
                new QueryKey("user", key.toString()),
                "$",
                JsonUtil.nest(container.dotNotation(),JsonUtil.OBJECT_MAPPER.valueToTree(value))
        );
    }
}
