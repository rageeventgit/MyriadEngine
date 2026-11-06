package net.rageevent.myriad.database.api.communication;


import net.rageevent.myriad.database.api.query.QueryKey;
import net.rageevent.myriad.database.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;


public interface MessageAgentStore {

    /**
     * Adds a way to handle incoming messages.
     * <p>
     *     Each handler is specific to its own {@code key}, meaning if another key
     *     has a message the other handlers will not fire or handle it.
     * </p>
     * @param key the channel that is being listened to.
     * @param json the handler for when a message is received.
     */
    void add(QueryKey key, CommunicationAgent<?> json);

    /**
     * Removes all handlers.
     * <p>Should be used during cleanup or shutdown.</p>
     */
    void clear();

    /**
     * Processes a JSON message using the handler associated with the provided key.
     *
     * <p>
     * The supplied {@link QueryKey} is used to locate a registered agent capable of
     * handling the message. If an agent is present, the provided JSON payload is
     * passed to that agent for processing and the resulting mapped output is returned.
     * </p>
     *
     * <p>
     * The returned {@link CompletableFuture} completes once the handler finishes
     * processing the message.
     * </p>
     * <p>
     *      If no handler is registered for the given key, the returned future will
     *      complete with a failed {@link QueryResult}.
     * </p>
     * @param key the channel or identifier used to locate the handler responsible
     *            for processing the message
     * @param json the JSON payload received from the message bus
     * @param <T> the expected output type produced by the handler
     * @return a {@link CompletableFuture} containing a {@link QueryResult} with the
     *         mapped result from the handler
     */
    <T> CompletableFuture<QueryResult<T>> handle(QueryKey key, String json);

    /**
     * Processes a JSON message using the handler associated with the provided key.
     *
     * <p>
     *      The supplied {@link QueryKey} is used to locate a registered agent capable of
     *      handling the message. If an agent is present, the provided JSON payload is
     *      passed to that agent for processing and the resulting mapped output is returned.
     * </p>
     *
     * <p>
     *      The returned {@link CompletableFuture} completes once the handler finishes
     *      processing the message.
     * </p>
     * <p>
     *      If no handler is registered for the given key, the returned future will
     *      complete with a failed {@link QueryResult}.
     * </p>
     *
     * @param key the channel or identifier used to locate the handler responsible
     *            for processing the message
     * @param json the JSON payload received from the message bus
     * @param <T> the expected output type produced by the handler
     * @param executorService the executor service that will handle the agent.
     * @return a {@link CompletableFuture} containing a {@link QueryResult} with the
     *         mapped result from the handler
     */
    <T> CompletableFuture<QueryResult<T>> handle(QueryKey key, String json, ExecutorService executorService);

}
