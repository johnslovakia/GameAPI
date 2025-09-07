package cz.johnslovakia.gameapi;

import cz.johnslovakia.gameapi.game.Game;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

@Setter @Getter
public class MinigameSettings {

    private boolean useTeams = true;
    private boolean displayHealthBar = true;
    private boolean usePreperationTask = true;
    private boolean joinQuitMessages = true;
    private boolean changingKitAfterStart = false;
    private boolean respawning = false;
    private boolean defaultGameCountdown = true;
    private boolean sendMinigameDescription = true;
    private boolean chooseRandomMap = false;
    private boolean autoBestGameJoin = true;
    private boolean restartServerAfterEnd = false;
    private boolean teleportPlayersAfterEnd = true;
    private boolean anvilCommand = false;
    private boolean workbenchCommand = false;
    private boolean enabledReJoin = false;
    private boolean enabledSpectating = true;
    private boolean enabledJoiningAfterStart = false;
    private boolean enabledMovementInPreparation = false;
    private boolean allowDefaultKitSelection = true;
    private boolean canDropCosmeticHat = false;
    private boolean useLevelSystem = true;

    private String defaultLanguage = "english";

    private int gamesPerServer = 1;
    private int maxPlayers = 16;
    private int minPlayers = 12;
    private int maxTeams = 4;
    private int maxTeamPlayers = 4;
    private int maxMapsInGame = 1;
    private int reducedTime = 25;
    private int reducedPlayers = 12;
    private int startingTime = 60;
    private int gameTime = 3600;
    private int preparationTime = 4;
    private int respawnCooldown = -1;
    private int rounds = 0;

    private Consumer<Game> ownStartingProcess;

    public boolean useTeams() {
        return useTeams;
    }

    public boolean isEnabledHealthBar() {
        return displayHealthBar;
    }

    public boolean usePreperationTask() {
        return usePreperationTask;
    }

    public boolean sendJoinQuitMessages() {
        return joinQuitMessages;
    }

    public boolean isEnabledChangingKitAfterStart() {
        return changingKitAfterStart;
    }

    public boolean isEnabledRespawning() {
        return respawning;
    }

    public boolean useDefaultGameCountdown() {
        return defaultGameCountdown;
    }

    public boolean sendMinigameDescription() {
        return sendMinigameDescription;
    }

    public boolean chooseRandomMap() {
        return chooseRandomMap;
    }

    public boolean joinToBestGameAfterJoin() {
        return autoBestGameJoin;
    }

    public boolean restartServerAfterEnd() {
        return restartServerAfterEnd;
    }

    public boolean teleportPlayersAfterEnd() {
        return teleportPlayersAfterEnd;
    }

    public boolean isEnabledAnvilCommand() {
        return anvilCommand;
    }

    public boolean isEnabledWorkbenchCommand() {
        return workbenchCommand;
    }

    public boolean isAllowedJoiningAfterStart() {
        return enabledJoiningAfterStart;
    }
}