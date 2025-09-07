package cz.johnslovakia.gameapi.messages;

import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.users.GamePlayer;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Language {

    @Getter
    private static List<Language> languages = new ArrayList<>();


    private final String name;
    private File file;
    @Setter
    private boolean defaultLanguage;

    public Language(String name) {
        this.name = name;
    }

    public Language(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public boolean isDefaultLanguage() {
        return defaultLanguage;
    }



    public static Language addLanguage(Language language){
        if (languages.contains(language)){
            return language;
        }
        languages.add(language);
        if (Minigame.getInstance().getSettings().getDefaultLanguage().equalsIgnoreCase(language.getName()))
            language.setDefaultLanguage(true);
        return language;
    }

    public static Language getLanguage(String language){
        for (Language l : getLanguages()){
            if (l.getName().equalsIgnoreCase(language)){
                return l;
            }
        }
        return null;
    }

    public static Language getDefaultLanguage(){
        for (Language l : getLanguages()){
            if (l.isDefaultLanguage()){
                return l;
            }
        }
        return (getLanguage("english") != null ? getLanguage("english") : getLanguages().get(0));
    }

}