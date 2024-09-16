package cz.johnslovakia.gameapi.messages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Language {

    private String name;
    private File file;
    private boolean defaultLanguage;

    public Language(String name) {
        this.name = name;
    }

    public Language(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public boolean isDefaultLanguage() {
        return defaultLanguage;
    }

    public Language setDefaultLanguage(boolean defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
        return this;
    }

    private static List<Language> languages = new ArrayList<>();

    public static Language addLanguage(Language language){
        if (languages.contains(language)){
            return language;
        }
        languages.add(language);
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

    public File getFile() {
        return file;
    }

    public static List<Language> getLanguages() {
        return languages;
    }
}