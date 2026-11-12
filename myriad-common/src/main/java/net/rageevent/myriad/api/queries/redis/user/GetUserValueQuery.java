package net.rageevent.myriad.api.queries.redis.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.api.database.JsonUtil;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.SequencedQuery;
import net.rageevent.myriad.api.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.PassedQueryResult;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;
import net.rageevent.myriad.api.player.store.DataContainer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record GetUserValueQuery<T>(UUID key,
                                   DataContainer container) implements SequencedQuery<T, LettuceDatabaseInteractor> {

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<QueryResult<T>> call(LettuceDatabaseInteractor interactor) {
        return interactor.jsonGet(new QueryKey("user", key.toString()), "$." + container.dotNotation())
                .thenApplyAsync(result -> {
                    if (result.failed()) return new FailedQueryResult<>("Result is null");
                    JsonNode object = result.object();
                    try {
                        return new PassedQueryResult<>((T) JsonUtil.OBJECT_MAPPER.treeToValue(object, container.defaultType()));
                    } catch (JsonProcessingException e) {
                        return new FailedQueryResult<>(e);
                    }
                });
    }
}
