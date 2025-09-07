package cz.johnslovakia.gameapi.levelSystem;

import cz.johnslovakia.gameapi.utils.rewards.Reward;

public record LevelReward(Reward reward, int... level) { }
