package cz.johnslovakia.gameapi.users.quests;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.datastorage.Type;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import cz.johnslovakia.gameapi.utils.eTrigger.Condition;
import cz.johnslovakia.gameapi.utils.eTrigger.Trigger;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.QuestUnclaimedReward;
import cz.johnslovakia.gameapi.utils.rewards.unclaimed.UnclaimedReward;
import lombok.Getter;
import net.bytebuddy.asm.Advice;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

@Getter
public class QuestManager {

    private final String name;
    private final List<Quest> quests = new ArrayList<>();

    public QuestManager(String name) {
        this.name = name;

        Minigame.getInstance().getMinigameTable().createNewColumn(Type.JSON, "Quests");
    }

    public void registerQuest(Quest... quests){
        for (Quest quest : quests){
            if (!this.quests.contains(quest)){
                this.quests.add(quest);

                for (Trigger<?> t : quest.getTriggers()) {
                    Minigame.getInstance().getPlugin().getServer().getPluginManager().registerEvent(t.getEventClass(), new Listener() {
                    }, EventPriority.NORMAL, (listener, event) -> onEventCall(quest, event), Minigame.getInstance().getPlugin());
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

        playerData.getQuestData().removeIf(this::shouldReset);


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
                playerData.getQuestData().add(new PlayerQuestData(newQuest, gamePlayer, LocalDate.now(), 0));
                dailyQuestCount++;
            } else {
                break;
            }
        }

        while (weeklyQuestCount < 2) {
            List<Quest> freeWeeklyQuests = getFreeQuests(gamePlayer, QuestType.WEEKLY);
            if (!freeWeeklyQuests.isEmpty()) {
                Quest newQuest = freeWeeklyQuests.get(0);
                playerData.getQuestData().add(new PlayerQuestData(newQuest, gamePlayer, LocalDate.now(), 0));
                weeklyQuestCount++;
            } else {
                break;
            }
        }
    }

    public boolean shouldReset(PlayerQuestData data){
        Optional<QuestUnclaimedReward> unclaimedReward = data.getGamePlayer().getPlayerData()
                .getUnclaimedRewards(UnclaimedReward.Type.QUEST).stream()
                .filter(r -> r instanceof QuestUnclaimedReward)
                .map(r -> (QuestUnclaimedReward) r)
                .filter(r -> r.getQuestName().equals(data.getQuest().getName()))
                .findFirst();
        if (unclaimedReward.isPresent())
            return false;


        LocalDate today = LocalDate.now();
        LocalDate startDate = data.getStartDate();
        if (startDate == null)
            return true;

        if (data.getQuest().getType().equals(QuestType.WEEKLY)) {
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int startWeek = startDate.get(weekFields.weekOfWeekBasedYear());
            int currentWeek = today.get(weekFields.weekOfWeekBasedYear());

            int startYear = startDate.getYear();
            int currentYear = today.getYear();

            return startWeek != currentWeek || startYear != currentYear;
        } else {
            return !startDate.isEqual(today);
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
