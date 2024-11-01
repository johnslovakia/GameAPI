package cz.johnslovakia.gameapi.users.achievements;

import cz.johnslovakia.gameapi.economy.Economy;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;

import java.util.Map;
import java.util.Set;

public interface Achievement {

    QuestType getType();
    String getName();
    Map<Economy, Integer> getRewards();
    Set<Trigger<?>> getTriggers();

}
