package cz.johnslovakia.gameapi.users.quests;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.eTrigger.Condition;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class QuestManager {

    private final String name;
    private final List<Quest> quests = new ArrayList<>();

    public QuestManager(String name) {
        this.name = name;
    }

    public void registerQuest(Quest... quests){
        for (Quest quest : quests){
            if (!this.quests.contains(quest)){
                this.quests.add(quest);

                for (Trigger<?> t : quest.getTriggers()) {
                    GameAPI.getInstance().getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() {
                    }, EventPriority.NORMAL, (listener, event) -> onEventCall(quest, event), GameAPI.getInstance());
                }
            }
        }
    }

    public Quest getQuest(QuestType type, String name){
        for (Quest quest : quests){
            if (type != quest.getType()){
                continue;
            }
            if (quest.getName().equals(name)){
                return quest;
            }
        }
        return null;
    }


    public List<Quest> getFreeQuests(GamePlayer gamePlayer, QuestType type){
        PlayerData playerData = gamePlayer.getPlayerData();

        List<Quest> freeQuests = new ArrayList<>(quests.stream().filter(quest -> quest.getType().equals(type)).toList());
        for (PlayerQuestData quest : playerData.getQuestData()){
            freeQuests.remove(quest.getQuest());
        }

        Collections.shuffle(freeQuests);
        return freeQuests;
    }

    public void check(GamePlayer gamePlayer){
        PlayerData playerData = gamePlayer.getPlayerData();

        playerData.getQuestData().removeIf(quest -> quest.isCompleted() && hasTimeElapsedSinceCompletion(quest));



        int dailyQuestCount = (int) playerData.getQuestData().stream()
                .filter(quest -> quest.getQuest().getType() == QuestType.DAILY)
                .count();
        int weeklyQuestCount = (int) playerData.getQuestData().stream()
                .filter(quest -> quest.getQuest().getType() == QuestType.WEEKLY)
                .count();

        while (dailyQuestCount < 2) {
            List<Quest> freeDailyQuests = getFreeQuests(gamePlayer, QuestType.DAILY);
            if (!freeDailyQuests.isEmpty()) {
                Quest newQuest = freeDailyQuests.get(0);
                playerData.getQuestData().add(new PlayerQuestData(newQuest, gamePlayer));
                dailyQuestCount++;
            } else {
                break;
            }
        }

        while (weeklyQuestCount < 2) {
            List<Quest> freeWeeklyQuests = getFreeQuests(gamePlayer, QuestType.WEEKLY);
            if (!freeWeeklyQuests.isEmpty()) {
                Quest newQuest = freeWeeklyQuests.get(0);
                playerData.getQuestData().add(new PlayerQuestData(newQuest, gamePlayer));
                weeklyQuestCount++;
            } else {
                break;
            }
        }
    }

    public static boolean hasTimeElapsedSinceCompletion(PlayerQuestData questData){
        LocalDate now = LocalDate.now();

        if (questData.getCompletionDate() == null){
            return true;
        }

        if (questData.getQuest().getType().equals(QuestType.DAILY)){
            LocalDate oneDayAfterCompletion = questData.getCompletionDate().plusDays(1);
            return now.isEqual(oneDayAfterCompletion) || now.isAfter(oneDayAfterCompletion);
        }else{
            LocalDate oneWeekAfterCompletion = questData.getCompletionDate().plusWeeks(1);
            return now.isEqual(oneWeekAfterCompletion) || now.isAfter(oneWeekAfterCompletion);
        }
    }


    private boolean checkConditions(Quest quest, GamePlayer target) {
        boolean result = true;
        boolean alternativeResult = false;

        for(Method method : quest.getClass().getDeclaredMethods()){

            if(!method.isAnnotationPresent(Condition.class)) continue;
            if(!method.getReturnType().equals(boolean.class))
                if(method.getParameterCount() > 1) continue;
            method.setAccessible(true);

            Condition condition = method.getAnnotation(Condition.class);

            boolean invokeResult = false;
            try {
                if (method.getParameterCount() == 0) {
                    invokeResult = (boolean) method.invoke(quest);
                } else {
                    invokeResult = (boolean) method.invoke(quest, target);
                }
            } catch (InvocationTargetException | IllegalAccessException ignored) {
            }

            if(!condition.alternative()) {
                result = (invokeResult == !condition.negate()) && result;
            } else {
                alternativeResult = (invokeResult && !condition.negate()) || alternativeResult;
            }
        }
        return result || alternativeResult;
    }

    private void onEventCall(Quest quest, Event event){
        for (Trigger<?> trigger : quest.getTriggers()) {
            Class<? extends Event> clazz = trigger.getEventClass();
            if (clazz.equals(event.getClass())) {
                if (!trigger.validate(clazz.cast(event))) continue;
                //GamePlayer gamePlayer = trigger.compute(clazz.cast(event));
                for (GamePlayer gamePlayer : trigger.compute(clazz.cast(event))) {
                    if (checkConditions(quest, gamePlayer)) {
                        trigger.getResponse().accept(gamePlayer);
                        return;
                    }
                }
            }
        }
    }
}
