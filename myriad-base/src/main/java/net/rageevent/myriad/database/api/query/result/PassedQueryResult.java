package net.rageevent.myriad.database.api.query.result;

public record PassedQueryResult<T>(T object, String message) implements QueryResult<T> {

    public PassedQueryResult(T object) {
        this(object, "Query passed successfully.");
    }
    public PassedQueryResult(String message) {
        this(null, message);
    }

}
