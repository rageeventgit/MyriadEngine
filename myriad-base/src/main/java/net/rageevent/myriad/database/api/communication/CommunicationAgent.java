package net.rageevent.myriad.database.api.communication;


import net.rageevent.myriad.database.api.query.result.QueryResult;

@FunctionalInterface
public interface CommunicationAgent<T> {
    QueryResult<T> handle(String json);
}
