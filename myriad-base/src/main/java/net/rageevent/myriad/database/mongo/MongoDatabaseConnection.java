package net.rageevent.myriad.database.mongo;

import net.rageevent.myriad.database.api.communication.KeyValueDatabaseInteractor;
import net.rageevent.myriad.database.api.database.DatabaseConnection;
import net.rageevent.myriad.database.api.query.Query;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

public class MongoDatabaseConnection implements DatabaseConnection<KeyValueDatabaseInteractor> {
    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public KeyValueDatabaseInteractor interactor() {
        return null;
    }

    @Override
    public <T> CompletableFuture<QueryResult<T>> post(Query<T, KeyValueDatabaseInteractor> query) {
        return null;
    }
}
