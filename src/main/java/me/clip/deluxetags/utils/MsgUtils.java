package me.clip.deluxetags.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MsgUtils {
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&(#[a-f0-9]{6})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_PATTERN = Pattern.compile("(#[a-f0-9]{6})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANGLE_TAG_PATTERN = Pattern.compile("(?<!\\\\)<[^<>\\r\\n]+>");
    private static final Pattern DIRECT_HEX_TAG_PATTERN = Pattern.compile("#[a-f0-9]{6}", Pattern.CASE_INSENSITIVE);
    private static final Set<String> KNOWN_MINI_MESSAGE_TAGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold",
        "gray", "grey", "dark_gray", "dark_grey", "blue", "green", "aqua", "red",
        "light_purple", "yellow", "white", "color", "colour", "c", "shadow", "bold", "b",
        "italic", "em", "i", "underlined", "u", "strikethrough", "st", "obfuscated", "obf",
        "reset", "click", "hover", "keybind", "key", "lang", "tr", "translate", "lang_or",
        "tr_or", "translate_or", "insert", "rainbow", "gradient", "transition", "font",
        "newline", "br", "selector", "sel", "score", "nbt", "data", "pride", "sprite", "head"
    )));
    private static final boolean SUPPORTS_HEX_COLORS = supportsHexColors();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final MiniMessage MINI_MESSAGE_OUTPUT = MiniMessage.builder().strict(true).build();
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile(
        "\u00A7x(?:\u00A7[0-9a-f]){6}|&x(?:&[0-9a-f]){6}|[&\u00A7]#[0-9a-f]{6}|[&\u00A7][0-9a-fk-or]",
        Pattern.CASE_INSENSITIVE);
    private static final String[] LEGACY_TAGS = {
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
        "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
    };
    private static final LegacyComponentSerializer MINI_MESSAGE_SERIALIZER = SUPPORTS_HEX_COLORS
        ? LegacyComponentSerializer.builder()
            .character('\u00A7')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build()
        : LegacyComponentSerializer.legacySection();
    private static Pattern PATTERN = HEX_PATTERN;
    private static boolean miniMessage;

    public static void setPattern(boolean legacy) {
        PATTERN = legacy ? LEGACY_HEX_PATTERN : HEX_PATTERN;
    }

    public static void setMiniMessage(boolean enabled) {
        miniMessage = enabled;
    }

    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        if (miniMessage && containsMiniMessageTag(input)) {
            input = colorMiniMessage(input);
        }

        //Hex Support for 1.16.1+
        Matcher m = PATTERN.matcher(input);
        if (SUPPORTS_HEX_COLORS) {
            while (m.find()) {
                input = input.replace(m.group(), toLegacyHex(m.group(1)));
            }
        } else {
            while (m.find()) {
                input = input.replace(m.group(), "");
            }
        }

        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /** Returns a self-contained MiniMessage representation without legacy codes. */
    public static String toMiniMessage(String input) {
        return input == null || input.isEmpty() ? input : MINI_MESSAGE_OUTPUT.serialize(toComponent(input));
    }

    /** Returns visible text, removing both supported MiniMessage and legacy formatting. */
    public static String stripFormatting(String input) {
        return input == null || input.isEmpty() ? input : PlainTextComponentSerializer.plainText().serialize(toComponent(input));
    }

    private static Component toComponent(String input) {
        if (miniMessage && containsMiniMessageTag(input)) {
            // Normalize legacy formatting from nested placeholders before MiniMessage parsing.
            return MINI_MESSAGE.deserialize(legacyToMiniMessage(escapeUnsupportedTags(input)));
        }

        // Unlike legacy chat output, component output can retain RGB on every server version.
        Matcher matcher = PATTERN.matcher(input);
        StringBuffer converted = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(converted, Matcher.quoteReplacement(toLegacyHex(matcher.group(1))));
        }
        matcher.appendTail(converted);
        return MINI_MESSAGE.deserialize(legacyToMiniMessage(MINI_MESSAGE.escapeTags(converted.toString())));
    }

    private static String legacyToMiniMessage(String input) {
        Matcher matcher = LEGACY_CODE_PATTERN.matcher(input);
        StringBuffer converted = new StringBuffer();
        while (matcher.find()) {
            String code = matcher.group().toLowerCase(Locale.ROOT);
            char value = code.charAt(1);
            String replacement;
            if (value == 'x') {
                replacement = "<reset><#" + code.substring(2).replace("\u00A7", "").replace("&", "") + ">";
            } else if (value == '#') {
                replacement = "<reset><" + code.substring(1) + ">";
            } else {
                int colorIndex = "0123456789abcdef".indexOf(value);
                if (colorIndex >= 0) {
                    replacement = "<reset><" + LEGACY_TAGS[colorIndex] + ">";
                } else {
                    switch (value) {
                        case 'k': replacement = "<obfuscated>"; break;
                        case 'l': replacement = "<bold>"; break;
                        case 'm': replacement = "<strikethrough>"; break;
                        case 'n': replacement = "<underlined>"; break;
                        case 'o': replacement = "<italic>"; break;
                        default: replacement = "<reset>";
                    }
                }
            }
            matcher.appendReplacement(converted, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(converted);
        return converted.toString();
    }

    private static String colorMiniMessage(String input) {
        try {
            return MINI_MESSAGE_SERIALIZER.serialize(MINI_MESSAGE.deserialize(escapeUnsupportedTags(input)));
        } catch (RuntimeException ignored) {
            return input;
        }
    }

    private static boolean containsMiniMessageTag(String input) {
        Matcher matcher = ANGLE_TAG_PATTERN.matcher(input);
        while (matcher.find()) {
            if (isMiniMessageTag(matcher.group())) {
                return true;
            }
        }

        return false;
    }

    private static String escapeUnsupportedTags(String input) {
        Matcher matcher = ANGLE_TAG_PATTERN.matcher(input);
        StringBuffer escaped = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group();
            String replacement = isMiniMessageTag(tag) ? tag : "\\" + tag;
            matcher.appendReplacement(escaped, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(escaped);
        return escaped.toString();
    }

    private static boolean isMiniMessageTag(String tag) {
        String content = tag.substring(1, tag.length() - 1).trim();
        if (content.isEmpty()) {
            return false;
        }

        if (content.startsWith("/") || content.startsWith("!")) {
            content = content.substring(1).trim();
        }

        if (content.isEmpty()) {
            return false;
        }

        String name = content.split(":", 2)[0].toLowerCase(Locale.ROOT);
        return DIRECT_HEX_TAG_PATTERN.matcher(name).matches() || KNOWN_MINI_MESSAGE_TAGS.contains(name);
    }

    private static boolean supportsHexColors() {
        try {
            Class.forName("net.md_5.bungee.api.ChatColor").getDeclaredMethod("of", String.class);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static String toLegacyHex(String hex) {
        StringBuilder result = new StringBuilder("\u00A7x");
        for (int i = 1; i < hex.length(); i++) {
            result.append('\u00A7').append(hex.charAt(i));
        }
        return result.toString();
    }

    public static void msg(CommandSender s, String message) {
        for (String line : color(message).split("\\\\n")) {
            s.sendMessage(line);
        }
    }
}
