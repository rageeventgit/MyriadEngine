package net.rageevent.myriad.database.api.query.result;

public record UnknownQueryResult<T>(String message) implements QueryResult<T> {

    @Override
    public T object() {
        return null;
    }

}
