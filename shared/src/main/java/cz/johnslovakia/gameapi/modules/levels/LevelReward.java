package cz.johnslovakia.gameapi.modules.levels;

import cz.johnslovakia.gameapi.rewards.Reward;

public record LevelReward(Reward reward, int... level) { }
