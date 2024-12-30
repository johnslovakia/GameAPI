package cz.johnslovakia.gameapi.users.achievements;

import cz.johnslovakia.gameapi.users.resources.Resource;
import cz.johnslovakia.gameapi.users.quests.QuestType;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;

import java.util.Map;
import java.util.Set;

public interface Achievement {

    QuestType getType();
    String getName();
    Map<Resource, Integer> getRewards();
    Set<Trigger<?>> getTriggers();

}
