package net.rageevent.myriad.api.database.api.query;


import net.rageevent.myriad.api.database.api.database.DatabaseInteractor;

import java.util.UUID;
/*
    Allows for locking of a key so multiple requests can be handled smoothly.
 */
public interface SequencedQuery<T,I extends DatabaseInteractor> extends Query<T,I> {

    /**
     * The unique identifier that will be locked on query.
     * 
     * @return A unique identifier.
     */
    UUID key();

}
