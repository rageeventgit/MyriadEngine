package net.rageevent.myriad.database.api.query.result;

public sealed interface QueryResult<T> permits PassedQueryResult, FailedQueryResult, UnknownQueryResult {

    T object();

    String message();


    default boolean failed() {
        return this instanceof FailedQueryResult;
    }
}
