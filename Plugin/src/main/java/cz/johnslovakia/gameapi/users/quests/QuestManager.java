package cz.johnslovakia.gameapi.users.quests;

import lombok.Getter;

import java.util.ArrayList;
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
}
