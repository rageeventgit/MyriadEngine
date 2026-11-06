package net.rageevent.myriad.database.api.query;


import net.rageevent.myriad.database.api.database.DatabaseInteractor;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a database operation that can be executed using a
 * {@link DatabaseInteractor}.
 *
 * A query encapsulates a single unit of work such as inserting,
 * updating, or retrieving data from the database. Implementations
 * define the logic required to perform the operation when executed
 * by the provided interactor.
 *
 * Queries are typically executed asynchronously and return their
 * result through a {@link CompletableFuture}.
 *
 * @param <T> the result type returned by the query
 * @param <I> the type of {@link DatabaseInteractor} used to execute the query
 */
public interface Query<T, I extends DatabaseInteractor> {

   /**
    * Executes the query using the provided database interactor.
    *
    * Implementations should perform the required database operations
    * and return the result wrapped in a {@link QueryResult}.
    *
    * @param interactor the database interactor used to perform the operation
    * @return a future containing the result of the query execution
    */
   CompletableFuture<QueryResult<T>> call(I interactor);

}
