package net.rageevent.myriad.api.database.api.database;


import net.rageevent.myriad.api.database.api.query.Query;
import net.rageevent.myriad.api.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

public interface DatabaseConnection<I extends DatabaseInteractor> {

    /**
     * Connects to a database.
     *
     * <p>
     * Allows a connection to a specified database to be created.
     * The database won't connect again if a connection is present.
     * </p>
     *
     */
    void connect();

    /**
     * Disconnects the current connection to the database.
     *
     * <p>
     * If there is a current connection to a specified database,
     * the connection will be closed
     * </p>
     *
     */
    void disconnect();

    /**
     * A query wrapper capable of receiving queries and sending to the specified database.
     * There is only one instance of an interactor per database connection.
     * <p>
     * Sends the database a query using generated statements, or messages.
     * It is expected to use a {@link Query} via {@linkplain DatabaseConnection#post(Query)}
     * method to send queries as they have expected inputs & outputs.
     * </p>
     * <p>
     * Every operation performed by the interactor is async and thread-safe.
     * </p>
     *
     * @return an instance of {@link DatabaseInteractor}
     * @see DatabaseConnection#post(Query)
     */
    I interactor();

    /**
     * Sends a premade query to the database.
     *
     * <p>
     * The query is handled by a {@link DatabaseInteractor} which sends the query to the database,
     * handles the receiving and formatting.
     * </p>
     *
     * @param query the pre-written query that is going to be sent to the database.
     * @param <T>   the expected return type of the query.
     * @return a {@link CompletableFuture} containing a {@link QueryResult} with {@code T}, or if failed
     * a {@link net.rageevent.work.api.query.result.FailedQueryResult} if the response is failed.
     */
    <T, Q extends Query<T, I>> CompletableFuture<QueryResult<T>> post(Q query);
}
