package net.rageevent.myriad.api;

import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.rageevent.myriad.api.database.api.communication.LettuceDatabaseInteractor;
import net.rageevent.myriad.api.database.api.database.MessagingDatabaseConnection;

@Accessors(fluent = true)
public class DataAccessor {

    @Setter
    static MessagingDatabaseConnection<LettuceDatabaseInteractor> connection = null;

}
