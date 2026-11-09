package net.rageevent.myriad.api.database;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.rageevent.myriad.api.database.mongo.MongoDataCollection;

@RequiredArgsConstructor
@Accessors(fluent = true)
@Getter
public enum MongoCollections implements MongoDataCollection {

    PLAYERS("users"),
    TESTING("tests"),
    ;

    private final String databaseName;
}