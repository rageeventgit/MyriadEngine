package net.rageevent.myriad.api.database.api.mongo;

import net.rageevent.myriad.api.database.api.query.Query;
import net.rageevent.myriad.api.database.mongo.MongoDataCollection;

public interface MongoQuery<T> extends Query<T, BsonDatabaseInteractor> {

    MongoDataCollection collection();
}
