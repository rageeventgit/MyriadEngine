package net.rageevent.myriad.api.queries;

import com.fasterxml.jackson.databind.JsonNode;
import net.rageevent.myriad.api.DataContainer;
import net.rageevent.myriad.api.database.JsonUtil;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.query.Query;
import net.rageevent.myriad.api.database.api.query.QueryKey;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record QueryDirtyUserData(UUID uuid,
                                 JsonNode containerNode) implements Query<Void, LettuceDatabaseInteractor> {

    @Override
    public CompletableFuture<QueryResult<Void>> call(LettuceDatabaseInteractor interactor) {
        return interactor.append(new QueryKey("dirty:user", uuid.toString()), containerNode);
    }
}
