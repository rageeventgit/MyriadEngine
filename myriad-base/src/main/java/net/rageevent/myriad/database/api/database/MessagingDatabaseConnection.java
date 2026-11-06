package net.rageevent.myriad.database.api.database;


import net.rageevent.myriad.database.api.communication.MessageAgentStore;

/**
 * A type of connection that allows for messaging
 * via {@link MessageAgentStore} to send and receive
 * messages across servers.
 *
 * @param <I> the type of interactor that is being used.
 * @see DatabaseConnection
 */
public interface MessagingDatabaseConnection<I extends DatabaseInteractor> extends DatabaseConnection<I> {

    /**
     * Handles and receives messages from a provided source.
     *
     * <p>
     *     Any messages or handlers go through the agent, regardless
     *     of what is being sent besides {@link net.rageevent.work.api.query.Query}s.
     * </p>
     *
     * @return an agent that is used to send and handle messages.
     * @see net.rageevent.work.redis.RedisMessagingAgent
     */
    MessageAgentStore agent();

}
