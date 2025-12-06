package cz.johnslovakia.gameapi.rewards;

import cz.johnslovakia.gameapi.modules.resources.Resource;

import java.util.Map;

public record PlayerRewardRecord(String source, Map<Resource, Integer> earned){

    public PlayerRewardRecord(Map<Resource, Integer> earned) {
        this("unknown", earned);
    }
}