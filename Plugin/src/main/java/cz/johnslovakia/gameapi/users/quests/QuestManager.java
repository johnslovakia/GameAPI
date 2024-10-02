package cz.johnslovakia.gameapi.users.quests;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerData;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class QuestManager {

    private String name;
    private List<Quest> quests = new ArrayList<>();

    public QuestManager(String name) {
        this.name = name;
    }

    public void registerQuest(Quest... quests){
        for (Quest quest : quests){
            if (!this.quests.contains(quest)){
                this.quests.add(quest);

                //TODO: uložení do databáze, v Json souboru budou překlady
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
        for (PlayerQuestData quest : playerData.getQuests()){
            freeQuests.remove(quest.getQuest());
        }

        Collections.shuffle(freeQuests);
        return freeQuests;
    }

    public void check(GamePlayer gamePlayer){
        PlayerData playerData = gamePlayer.getPlayerData();

        playerData.getQuests().removeIf(quest -> quest.isCompleted() && hasTimeElapsedSinceCompletion(quest));



        int dailyQuestCount = (int) playerData.getQuests().stream()
                .filter(quest -> quest.getQuest().getType() == QuestType.DAILY)
                .count();
        int weeklyQuestCount = (int) playerData.getQuests().stream()
                .filter(quest -> quest.getQuest().getType() == QuestType.WEEKLY)
                .count();

        while (dailyQuestCount < 2) {
            List<Quest> freeDailyQuests = getFreeQuests(gamePlayer, QuestType.DAILY);
            if (!freeDailyQuests.isEmpty()) {
                Quest newQuest = freeDailyQuests.get(0);
                playerData.getQuests().add(new PlayerQuestData(newQuest, gamePlayer));
                dailyQuestCount++;
            } else {
                break;
            }
        }

        while (weeklyQuestCount < 2) {
            List<Quest> freeWeeklyQuests = getFreeQuests(gamePlayer, QuestType.WEEKLY);
            if (!freeWeeklyQuests.isEmpty()) {
                Quest newQuest = freeWeeklyQuests.get(0);
                playerData.getQuests().add(new PlayerQuestData(newQuest, gamePlayer));
                weeklyQuestCount++;
            } else {
                break;
            }
        }
    }

    public static boolean hasTimeElapsedSinceCompletion(PlayerQuestData questData){
        LocalDate now = LocalDate.now();
        if (questData.getQuest().getType().equals(QuestType.DAILY)){
            LocalDate oneDayAfterCompletion = questData.getCompletionDate().plusDays(1);
            return now.isEqual(oneDayAfterCompletion) || now.isAfter(oneDayAfterCompletion);
        }else{
            LocalDate oneWeekAfterCompletion = questData.getCompletionDate().plusWeeks(1);
            return now.isEqual(oneWeekAfterCompletion) || now.isAfter(oneWeekAfterCompletion);
        }

    }
}
