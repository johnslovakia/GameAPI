package cz.johnslovakia.gameapi.modules.serverManagement;

import lombok.Getter;

@Getter
public class PendingServerAction {

    private final PendingActionType type;
    /**
     * Extra payload. Meaning depends on type:
     * - {@link PendingActionType#SPECTATE}: "game:" + game name
     * - {@link PendingActionType#JOIN_ARENA}: arena ID (last char of server name)
     */
    private final String data;

    public PendingServerAction(PendingActionType type, String data) {
        this.type = type;
        this.data = data;
    }
}