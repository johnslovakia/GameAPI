package cz.johnslovakia.gameapi;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(builderClassName = "Builder")
public class MinigameSettings {

    @lombok.Builder.Default private boolean useTeams = true;
    @lombok.Builder.Default private boolean displayHealthBar = true;
    @lombok.Builder.Default private boolean usePreperationTask = true;
    @lombok.Builder.Default private boolean joinQuitMessages = true;
    @lombok.Builder.Default private boolean enabledChangingKitAfterStart = false;
    @lombok.Builder.Default private boolean enabledRespawning = false;
    @lombok.Builder.Default private boolean defaultGameCountdown = true;
    @lombok.Builder.Default private boolean sendMinigameDescription = true;
    @lombok.Builder.Default private boolean chooseRandomMap = false;
    @lombok.Builder.Default private boolean autoBestGameJoin = true;
    @lombok.Builder.Default private boolean restartServerAfterEnd = false;
    @lombok.Builder.Default private boolean teleportPlayersAfterEnd = true;
    @lombok.Builder.Default private boolean anvilCommand = false;
    @lombok.Builder.Default private boolean workbenchCommand = false;
    @lombok.Builder.Default private boolean enabledReJoin = false;
    @lombok.Builder.Default private boolean enabledSpectating = true;
    @lombok.Builder.Default private boolean enabledJoiningAfterStart = false;
    @lombok.Builder.Default private boolean enabledMovementInPreparation = false;
    @lombok.Builder.Default private boolean allowDefaultKitSelection = true;
    @lombok.Builder.Default private boolean canDropCosmeticHat = false;
    @lombok.Builder.Default private boolean useLevelSystem = true;
    @lombok.Builder.Default private boolean useDailyRewardTrack = true;

    @lombok.Builder.Default private String defaultLanguage = "english";

    @lombok.Builder.Default private int gamesPerServer = 1;
    @lombok.Builder.Default private int maxPlayers = 16;
    @lombok.Builder.Default private int minPlayers = 12;
    @lombok.Builder.Default private int maxTeams = 4;
    @lombok.Builder.Default private int maxTeamPlayers = 4;
    @lombok.Builder.Default private int maxMapsInGame = 1;
    @lombok.Builder.Default private int reducedTime = 25;
    @lombok.Builder.Default private int reducedPlayers = 12;
    @lombok.Builder.Default private int startingTime = 60;
    @lombok.Builder.Default private int gameTime = 3600;
    @lombok.Builder.Default private int preparationTime = 4;
    @lombok.Builder.Default private int respawnCooldown = -1;
    @lombok.Builder.Default private int rounds = 0;
}