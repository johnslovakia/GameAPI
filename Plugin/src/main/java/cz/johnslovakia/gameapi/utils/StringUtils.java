package cz.johnslovakia.gameapi.utils;

import org.bukkit.ChatColor;

import java.awt.*;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

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
        /*Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, net.md_5.bungee.api.ChatColor.of(color) + "");
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);*/
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
}
