package cz.johnslovakia.gameapi.modules.game;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class GameModule {

    private GameInstance game;

    protected abstract void initialize();
    protected abstract void terminate();
}
