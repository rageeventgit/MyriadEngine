package net.rageevent.myriad.data;

import lombok.RequiredArgsConstructor;
import net.rageevent.myriad.database.api.communication.MessagingDatabaseInteractor;
import net.rageevent.myriad.database.api.database.MessagingDatabaseConnection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class UserDataAccessor {

    private final MessagingDatabaseConnection<MessagingDatabaseInteractor> redisConnection;
    private final Set<UUID> dirtyUsers = ConcurrentHashMap.newKeySet();

}
