package net.rageevent.myriad.database.api.mongo;

import net.rageevent.myriad.database.api.query.Query;
import net.rageevent.myriad.database.mongo.MongoDataCollection;

public interface MongoQuery<T> extends Query<T, BsonDatabaseInteractor> {

    MongoDataCollection collection();
}
