package cz.johnslovakia.gameapi.utils.rewards;

import cz.johnslovakia.gameapi.users.resources.Resource;

import java.util.Map;

public record PlayerRewardRecord(Map<Resource, Integer> earned){}