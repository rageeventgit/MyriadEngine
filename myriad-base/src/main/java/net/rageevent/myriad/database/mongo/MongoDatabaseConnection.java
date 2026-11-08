package net.rageevent.myriad.database.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.database.api.database.DatabaseConnection;
import net.rageevent.myriad.database.api.mongo.BsonDatabaseInteractor;
import net.rageevent.myriad.database.api.mongo.MongoQuery;
import net.rageevent.myriad.database.api.query.Query;
import net.rageevent.myriad.database.api.query.result.FailedQueryResult;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class MongoDatabaseConnection implements DatabaseConnection<BsonDatabaseInteractor> {

    private static final String DATABASE_NAME = "myriad-db";

    private final MongoConnectionInfo connectionInfo;

    private MongoDatabaseInteractor interactor;
    private MongoClient mongoClient;

    @Override
    public void connect() {
        MongoCredential credential = MongoCredential.createCredential(connectionInfo.username(), connectionInfo.something(), connectionInfo.password().toCharArray());

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .credential(credential)
                .applyToClusterSettings(b -> b.hosts(List.of(new ServerAddress("127.0.0.1", 27017))))
                .build();

        mongoClient = MongoClients.create(clientSettings);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DATABASE_NAME);
        interactor = new MongoDatabaseInteractor(mongoDatabase);
    }

    @Override
    public void disconnect() {
        mongoClient.close();
    }

    @Override
    public BsonDatabaseInteractor interactor() {
        return interactor;
    }

    @Override
    public <T, Q extends Query<T, BsonDatabaseInteractor>> CompletableFuture<QueryResult<T>> post(Q query) {
        if (!(query instanceof MongoQuery<?>))
            return CompletableFuture.completedFuture(new FailedQueryResult<>("Query type must be of mongo."));

        return query.call(interactor);
    }

}
