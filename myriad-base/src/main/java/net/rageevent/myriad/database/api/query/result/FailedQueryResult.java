package net.rageevent.myriad.database.api.query.result;

public record FailedQueryResult<T>(String message) implements QueryResult<T> {

    public FailedQueryResult(Throwable exception){
        this(exception.getClass().getSimpleName() + ": " + exception.getMessage());
    }
    @Override
    public T object() {
        return null;
    }

}
