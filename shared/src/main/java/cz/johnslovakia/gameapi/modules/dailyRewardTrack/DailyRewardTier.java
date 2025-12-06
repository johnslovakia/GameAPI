package cz.johnslovakia.gameapi.modules.dailyRewardTrack;

import cz.johnslovakia.gameapi.rewards.Reward;

public record DailyRewardTier(int tier, int neededXP, Reward reward) {}