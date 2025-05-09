package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class StringUtils {

    public static String stylizeText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    public static String betterNumberFormat(long number) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        return numberFormat.format(number);
    }

    public static String getFontTextComponentJSON(String text, String font){
        BaseComponent textComponent = new TextComponent(text);
        textComponent.setFont((font.contains(":") ? font : "gameapi:" + font));

        return ComponentSerializer.toString(textComponent);
    }

    public static String formatItemStackName(GamePlayer gamePlayer, ItemStack item){
        if (item == null) return "§cItemStack is null!";

        String formatedName = null;
        if (item.hasItemMeta()){
            if (item.getItemMeta().hasDisplayName()){
                formatedName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            }
        }
        if (formatedName == null){
            formatedName = WordUtils.capitalize(item.getType().name().toLowerCase().replaceAll("_", " "));
        }

        formatedName = (item.getAmount() > 1 ? item.getAmount() + "x " : "") + formatedName;
        int maxLoreLength = 35;


        if (!item.getEnchantments().isEmpty()){
            StringBuilder stringBuilder = new StringBuilder(" (");
            int i = 1;
            for (Enchantment enchantment : item.getEnchantments().keySet()){
                String formatedEnchantment = WordUtils.capitalize(enchantment.getKey().getKey().toLowerCase().replaceAll("_", " "));
                formatedEnchantment += " " + numeral(item.getEnchantmentLevel(enchantment));
                stringBuilder.append(formatedEnchantment);
                if (item.getEnchantments().size() > i){
                    stringBuilder.append(",");
                    if (formatedName.length() + formatedEnchantment.length() >= maxLoreLength){
                        stringBuilder.append("\n ");
                    }
                }

                i++;
            }
            formatedName += stringBuilder;
        }
        if (Utils.isPotion(item)){
            StringBuilder stringBuilder = new StringBuilder();

            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
            if (potionMeta == null) return formatedName;

            List<PotionEffect> list = new ArrayList<>();
            if (potionMeta.hasCustomEffects()){
                list.addAll(potionMeta.getCustomEffects());
            }else{
                list.addAll(Objects.requireNonNull(potionMeta.getBasePotionType()).getPotionEffects());
            }

            int i = 1;
            for (PotionEffect effect : list) {
                PotionEffectType effectType = effect.getType();
                int duration = effect.getDuration();

                String formatedPotion = WordUtils.capitalize(effectType.getKey().getKey().toLowerCase().replaceAll("_", " ")) + " " + numeral(effect.getAmplifier());
                formatedPotion += " " + getDurationString(duration);
                stringBuilder.append(formatedPotion);
                if (item.getEnchantments().size() > i){
                    stringBuilder.append(",");
                    if (formatedName.length() + formatedPotion.length() >= maxLoreLength){
                        stringBuilder.append("\n ");
                    }
                }
                i++;
            }

            formatedName = stringBuilder.toString();
        }

        if (item.hasItemMeta()) {
            Damageable damageable = (Damageable) item.getItemMeta();
            if (damageable != null) {
                if (damageable.getDamage() != 0) {
                    String duration = item.getType().getMaxDurability() - damageable.getDamage() + " " + MessageManager.get(gamePlayer, "word.uses").getTranslated();
                    if (formatedName.contains("(")) {
                        if (formatedName.length() >= maxLoreLength) {
                            formatedName += ",\n ";
                        } else {
                            formatedName += ", ";
                        }

                        formatedName += duration + ")";
                    } else {
                        formatedName += " (" + duration + ")";
                    }
                }
            }
        }

        if (formatedName.contains("(") && !formatedName.contains(")")){
            formatedName += ")";
        }

        return formatedName;
    }



    public static String randomString(int length, boolean numeric,
                                      boolean alphabetical, boolean symbolic) {
        if (!numeric && !alphabetical && !symbolic) {
            alphabetical = true;
            numeric = true;
        }

        String characters = (alphabetical ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ" : "")
                + (numeric ? "0123456789" : "")
                + (symbolic ? "~,.:?;[]{}´`^!@#$%¨&*()-_+=></ " : "");
        Random random = new Random();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = characters.charAt(random.nextInt(characters.length()));
            if (random.nextInt(2) == 0 && Character.isUpperCase(c)) {
                c = Character.toLowerCase(c);
            }
            sb.append(c);
        }

        return sb.toString();
    }

    public static String colorizer(String message) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');

            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder("");
            for (char c : ch) {
                builder.append("&" + c);
            }

            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String colorize(String str) {
        Pattern HEX_PATTERN = Pattern.compile("&(#\\w{6})");
        Matcher matcher = HEX_PATTERN.matcher(ChatColor.translateAlternateColorCodes('&', str));
        StringBuffer buffer = new StringBuffer();

        while (matcher.find())
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of(matcher.group(1)).toString());

        return matcher.appendTail(buffer).toString();
    }

    private final static int CENTER_PX = 154;

    //Source: SirSpoodles on Spigot
    public static String getCenteredMessage(String message){
        if(message == null || message.equals("")) return "";
        message = ChatColor.translateAlternateColorCodes('&', message);

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for(char c : message.toCharArray()){
            if(c == '�'){
                previousCode = true;
                continue;
            }else if(previousCode == true){
                previousCode = false;
                if(c == 'l' || c == 'L'){
                    isBold = true;
                    continue;
                }else isBold = false;
            }else{
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while(compensated < toCompensate){
            sb.append(" ");
            compensated += spaceLength;
        }
        return sb.toString() + message;
    }


    public static String getDurationString(int seconds) {

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = (seconds % 3600) % 60;

        //int minutes = (seconds % 3600) / 60;
        //seconds = seconds % 60;

        return (hours > 1 ? hours : "") + twoDigitString(minutes) + ":" + twoDigitString(seconds);
    }

    private static String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }



    //Source: SirSpoodles on Spigot
    public enum DefaultFontInfo{

        A('A', 5),
        a('a', 5),
        B('B', 5),
        b('b', 5),
        C('C', 5),
        c('c', 5),
        D('D', 5),
        d('d', 5),
        E('E', 5),
        e('e', 5),
        F('F', 5),
        f('f', 4),
        G('G', 5),
        g('g', 5),
        H('H', 5),
        h('h', 5),
        I('I', 3),
        i('i', 1),
        J('J', 5),
        j('j', 5),
        K('K', 5),
        k('k', 4),
        L('L', 5),
        l('l', 1),
        M('M', 5),
        m('m', 5),
        N('N', 5),
        n('n', 5),
        O('O', 5),
        o('o', 5),
        P('P', 5),
        p('p', 5),
        Q('Q', 5),
        q('q', 5),
        R('R', 5),
        r('r', 5),
        S('S', 5),
        s('s', 5),
        T('T', 5),
        t('t', 4),
        U('U', 5),
        u('u', 5),
        V('V', 5),
        v('v', 5),
        W('W', 5),
        w('w', 5),
        X('X', 5),
        x('x', 5),
        Y('Y', 5),
        y('y', 5),
        Z('Z', 5),
        z('z', 5),
        NUM_1('1', 5),
        NUM_2('2', 5),
        NUM_3('3', 5),
        NUM_4('4', 5),
        NUM_5('5', 5),
        NUM_6('6', 5),
        NUM_7('7', 5),
        NUM_8('8', 5),
        NUM_9('9', 5),
        NUM_0('0', 5),
        EXCLAMATION_POINT('!', 1),
        AT_SYMBOL('@', 6),
        NUM_SIGN('#', 5),
        DOLLAR_SIGN('$', 5),
        PERCENT('%', 5),
        UP_ARROW('^', 5),
        AMPERSAND('&', 5),
        ASTERISK('*', 5),
        LEFT_PARENTHESIS('(', 4),
        RIGHT_PERENTHESIS(')', 4),
        MINUS('-', 5),
        UNDERSCORE('_', 5),
        PLUS_SIGN('+', 5),
        EQUALS_SIGN('=', 5),
        LEFT_CURL_BRACE('{', 4),
        RIGHT_CURL_BRACE('}', 4),
        LEFT_BRACKET('[', 3),
        RIGHT_BRACKET(']', 3),
        COLON(':', 1),
        SEMI_COLON(';', 1),
        DOUBLE_QUOTE('"', 3),
        SINGLE_QUOTE('\'', 1),
        LEFT_ARROW('<', 4),
        RIGHT_ARROW('>', 4),
        QUESTION_MARK('?', 5),
        SLASH('/', 5),
        BACK_SLASH('\\', 5),
        LINE('|', 1),
        TILDE('~', 5),
        TICK('`', 2),
        PERIOD('.', 1),
        COMMA(',', 1),
        SPACE(' ', 3),
        DEFAULT('a', 4);

        private char character;
        private int length;

        DefaultFontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }

        public char getCharacter(){
            return this.character;
        }

        public int getLength(){
            return this.length;
        }

        public int getBoldLength(){
            if(this == DefaultFontInfo.SPACE) return this.getLength();
            return this.length + 1;
        }

        public static DefaultFontInfo getDefaultFontInfo(char c){
            for(DefaultFontInfo dFI : DefaultFontInfo.values()){
                if(dFI.getCharacter() == c) return dFI;
            }
            return DefaultFontInfo.DEFAULT;
        }
    }

    public static int unnumeral(String number) {
        if (number.startsWith("M")) return 1000 + unnumeral(number.replaceFirst("M", ""));
        if (number.startsWith("CM")) return 900 + unnumeral(number.replaceFirst("CM", ""));
        if (number.startsWith("D")) return 500 + unnumeral(number.replaceFirst("D", ""));
        if (number.startsWith("CD")) return 400 + unnumeral(number.replaceFirst("CD", ""));
        if (number.startsWith("C")) return 100 + unnumeral(number.replaceFirst("C", ""));
        if (number.startsWith("XC")) return 90 + unnumeral(number.replaceFirst("XC", ""));
        if (number.startsWith("L")) return 50 + unnumeral(number.replaceFirst("L", ""));
        if (number.startsWith("XL")) return 40 + unnumeral(number.replaceFirst("XL", ""));
        if (number.startsWith("X")) return 10 + unnumeral(number.replaceFirst("X", ""));
        if (number.startsWith("IX")) return 9 + unnumeral(number.replaceFirst("IX", ""));
        if (number.startsWith("V")) return 5 + unnumeral(number.replaceFirst("V", ""));
        if (number.startsWith("IV")) return 4 + unnumeral(number.replaceFirst("IV", ""));
        if (number.startsWith("I")) return 1 + unnumeral(number.replaceFirst("I", ""));
        return 0;
    }

    public static String numeral(int number) {
        if (number<=0) return "";
        if (number-1000>=0) return "M" + numeral(number-1000);
        if (number-900>=0) return "CM" + numeral(number-900);
        if (number-500>=0) return "D" + numeral(number-500);
        if (number-400>=0) return "CD" + numeral(number-400);
        if (number-100>=0) return "C" + numeral(number-100);
        if (number-90>=0) return "XC" + numeral(number-90);
        if (number-50>=0) return "L" + numeral(number-50);
        if (number-40>=0) return "XL" + numeral(number-40);
        if (number-10>=0) return "X" + numeral(number-10);
        if (number-9>=0) return "IX" + numeral(number-9);
        if (number-5>=0) return "V" + numeral(number-5);
        if (number-4>=0) return "IV" + numeral(number-4);
        if (number-1>=0) return "I" + numeral(number-1);
        return null;
    }
    // Or

    public String numural(int number) {
        String[] symbols = new String[] { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        int[] numbers = new int[] { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        for (int i = 0; i < numbers.length; i++) {
            if (number >= numbers[i]) {
                return symbols[i] + numural(number - numbers[i]);
            }
        }
        return "";
    }

    public int unnumural(String number) {
        String[] symbols = new String[] { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        int[] numbers = new int[] { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        for (int i = 0; i < symbols.length; i++) {
            if (number.startsWith(symbols[i])) {
                return numbers[i] + unnumural(number.replaceFirst(symbols[i], ""));
            }
        }
        return 0;
    }

    private static final Map<String, Integer> advances = new HashMap<>();

    static {
        advances.put("\uDAFF\uDF6A", -150);
        advances.put("\uDAFF\uDF9C", -100);
        advances.put("\uDAFF\uDFCE", -50);
        advances.put("\uDAFF\uDFCF", -49);
        advances.put("\uDAFF\uDFD0", -48);
        advances.put("\uDAFF\uDFD1", -47);
        advances.put("\uDAFF\uDFD2", -46);
        advances.put("\uDAFF\uDFD3", -45);
        advances.put("\uDAFF\uDFD4", -44);
        advances.put("\uDAFF\uDFD5", -43);
        advances.put("\uDAFF\uDFD6", -42);
        advances.put("\uDAFF\uDFD7", -41);
        advances.put("\uDAFF\uDFD8", -40);
        advances.put("\uDAFF\uDFD9", -39);
        advances.put("\uDAFF\uDFDA", -38);
        advances.put("\uDAFF\uDFDB", -37);
        advances.put("\uDAFF\uDFDC", -36);
        advances.put("\uDAFF\uDFDD", -35);
        advances.put("\uDAFF\uDFDE", -34);
        advances.put("\uDAFF\uDFDF", -33);
        advances.put("\uDAFF\uDFE0", -32);
        advances.put("\uDAFF\uDFE1", -31);
        advances.put("\uDAFF\uDFE2", -30);
        advances.put("\uDAFF\uDFE3", -29);
        advances.put("\uDAFF\uDFE4", -28);
        advances.put("\uDAFF\uDFE5", -27);
        advances.put("\uDAFF\uDFE6", -26);
        advances.put("\uDAFF\uDFE7", -25);
        advances.put("\uDAFF\uDFE8", -24);
        advances.put("\uDAFF\uDFE9", -23);
        advances.put("\uDAFF\uDFEA", -22);
        advances.put("\uDAFF\uDFEB", -21);
        advances.put("\uDAFF\uDFEC", -20);
        advances.put("\uDAFF\uDFED", -19);
        advances.put("\uDAFF\uDFEE", -18);
        advances.put("\uDAFF\uDFEF", -17);
        advances.put("\uDAFF\uDFF0", -16);
        advances.put("\uDAFF\uDFF1", -15);
        advances.put("\uDAFF\uDFF2", -14);
        advances.put("\uDAFF\uDFF3", -13);
        advances.put("\uDAFF\uDFF4", -12);
        advances.put("\uDAFF\uDFF5", -11);
        advances.put("\uDAFF\uDFF6", -10);
        advances.put("\uDAFF\uDFF7", -9);
        advances.put("\uDAFF\uDFF8", -8);
        advances.put("\uDAFF\uDFF9", -7);
        advances.put("\uDAFF\uDFFA", -6);
        advances.put("\uDAFF\uDFFB", -5);
        advances.put("\uDAFF\uDFFC", -4);
        advances.put("\uDAFF\uDFFD", -3);
        advances.put("\uDAFF\uDFFE", -2);
        advances.put("\uDAFF\uDFFF", -1);
        advances.put("\uDB00\uDC00", 0);
        advances.put("\uDB00\uDC01", 1);
        advances.put("\uDB00\uDC02", 2);
        advances.put("\uDB00\uDC03", 3);
        advances.put("\uDB00\uDC04", 4);
        advances.put("\uDB00\uDC05", 5);
        advances.put("\uDB00\uDC06", 6);
        advances.put("\uDB00\uDC07", 7);
        advances.put("\uDB00\uDC08", 8);
        advances.put("\uDB00\uDC09", 9);
        advances.put("\uDB00\uDC0A", 10);
        advances.put("\uDB00\uDC0B", 11);
        advances.put("\uDB00\uDC0C", 12);
        advances.put("\uDB00\uDC0D", 13);
        advances.put("\uDB00\uDC0E", 14);
        advances.put("\uDB00\uDC0F", 15);
        advances.put("\uDB00\uDC10", 16);
        advances.put("\uDB00\uDC11", 17);
        advances.put("\uDB00\uDC12", 18);
        advances.put("\uDB00\uDC13", 19);
        advances.put("\uDB00\uDC14", 20);
        advances.put("\uDB00\uDC15", 21);
        advances.put("\uDB00\uDC16", 22);
        advances.put("\uDB00\uDC17", 23);
        advances.put("\uDB00\uDC18", 24);
        advances.put("\uDB00\uDC19", 25);
        advances.put("\uDB00\uDC1A", 26);
        advances.put("\uDB00\uDC1B", 27);
        advances.put("\uDB00\uDC1C", 28);
        advances.put("\uDB00\uDC1D", 29);
        advances.put("\uDB00\uDC1E", 30);
        advances.put("\uDB00\uDC1F", 31);
        advances.put("\uDB00\uDC20", 32);
        advances.put("\uDB00\uDC21", 33);
        advances.put("\uDB00\uDC22", 34);
        advances.put("\uDB00\uDC23", 35);
        advances.put("\uDB00\uDC24", 36);
        advances.put("\uDB00\uDC25", 37);
        advances.put("\uDB00\uDC26", 38);
        advances.put("\uDB00\uDC27", 39);
        advances.put("\uDB00\uDC28", 40);
        advances.put("\uDB00\uDC29", 41);
        advances.put("\uDB00\uDC2A", 42);
        advances.put("\uDB00\uDC2B", 43);
        advances.put("\uDB00\uDC2C", 44);
        advances.put("\uDB00\uDC2D", 45);
        advances.put("\uDB00\uDC2E", 46);
        advances.put("\uDB00\uDC2F", 47);
        advances.put("\uDB00\uDC30", 48);
        advances.put("\uDB00\uDC31", 49);
        advances.put("\uDB00\uDC32", 50);
        advances.put("\uDB00\uDC64", 100);
        advances.put("\uDB00\uDC96", 150);
    }

    public static String calculateNegativeSpaces(int value) {
        StringBuilder result = new StringBuilder();

        for (String symbol : advances.keySet()) {
            int advanceValue = advances.get(symbol);

            if (advanceValue != 0) {
                if (advanceValue <= value) {
                    int times = value / advanceValue;
                    result.append(String.valueOf(symbol).repeat(Math.max(0, times)));
                    value -= times * advanceValue;
                }
            } else {
                Bukkit.getLogger().log(Level.WARNING, "AdvanceValue is zero for symbol: " + symbol);
            }
        }

        if (result.isEmpty()) {
            return "Žádný symbol neodpovídá";
        }

        return result.toString();
    }
}
