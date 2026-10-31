package net.rageevent.myriad.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Accessors(fluent=true)
@Getter
public enum LobbyType implements ServerType{

    MAIN("Main Lobby")

    ;
    private final String visualName;
    @Override
    public String id() {
        return name();
    }
}
