package net.rageevent.myriad.api.database.api.communication;


import net.rageevent.myriad.api.database.api.query.result.QueryResult;

@FunctionalInterface
public interface CommunicationAgent<T> {
    QueryResult<T> handle(String json);
}
