package net.rageevent.myriad;

import net.kyori.adventure.text.format.TextColor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ThemeColor {

    public static final TextColor RED = TextColor.color(251, 82, 119);
    public static final TextColor DARK_RED = TextColor.color(153, 0, 36);
    public static final TextColor BLUE = TextColor.color(85, 146, 250);
    public static final TextColor DARK_BLUE = TextColor.color(85, 92, 184);
    public static final TextColor GREEN = TextColor.color(134, 255, 180);
    public static final TextColor DARK_GREEN = TextColor.color(70, 129, 90);
    public static final TextColor YELLOW = TextColor.color(250, 255, 92);
    public static final TextColor GOLD = TextColor.color(255, 154, 57);
    public static final TextColor PURPLE = TextColor.color(138, 81, 253);
    public static final TextColor PINK = TextColor.color(255, 94, 244);
    public static final TextColor SILVER = TextColor.color(233, 240, 255);
    public static final TextColor AQUA = TextColor.color(133, 255, 229);
    public static final TextColor DARK_GRAY = TextColor.color(90, 100, 110);
    public static final TextColor GRAY = TextColor.color(149, 156, 170); //The more superior naming convention.
    public static final TextColor GREY = GRAY;
    public static final TextColor WHITE = TextColor.color(255, 255, 255);

    public static final Map<String, TextColor> COLOR_MAP = new HashMap<>(14);

    /*
        This could've been an Enum, but I'm too lazy to convert it to an Enum.
        Stupid...
     */
    static {
        Class<ThemeColor> themeColorClass = ThemeColor.class;
        Field[] colors = themeColorClass.getDeclaredFields();

        for (Field unregisteredColor : colors) {
            int fieldModifiers = unregisteredColor.getModifiers();
            if (!(Modifier.isStatic(fieldModifiers) && Modifier.isFinal(fieldModifiers))) continue;
            unregisteredColor.setAccessible(true);

            try {

                Object fieldObj = unregisteredColor.get(themeColorClass);
                if (!fieldObj.getClass().isAssignableFrom(TextColor.class)) continue;

                TextColor textColor = (TextColor) fieldObj;
                String fieldName = unregisteredColor.getName().toLowerCase();
                COLOR_MAP.putIfAbsent(fieldName, textColor);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
