package cz.johnslovakia.gameapi.modules.scores;

import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.function.BiFunction;

@RequiredArgsConstructor
public class ScoreGroup {

    @Getter
    private final String key;
    private final String displayName;

    @Setter
    private BiFunction<String, GamePlayer, String> placeholder;

    public String getDisplayName(GamePlayer gamePlayer){
        return placeholder != null ? placeholder.apply(displayName, gamePlayer) : displayName;
    }
}
