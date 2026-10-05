package me.clip.deluxetags.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.junit.Before;
import org.junit.Test;

public class MsgUtilsTest {

    @Before
    public void setUp() {
        MsgUtils.setPattern(false);
        MsgUtils.setMiniMessage(false);
    }

    @Test
    public void keepsLegacyFormattingWhenMiniMessageIsDisabled() {
        assertEquals("\u00A7aLegacy", MsgUtils.color("&aLegacy"));
    }

    @Test
    public void keepsUnknownAngleBracketPlaceholdersWhenMiniMessageIsEnabled() {
        MsgUtils.setMiniMessage(true);

        assertEquals("<%1$s> \u00A7cHi \u00A7lthere", MsgUtils.color("<%1$s> <red>Hi &lthere"));
    }

    @Test
    public void supportsPlainMiniMessageColorTags() {
        MsgUtils.setMiniMessage(true);

        assertEquals("\u00A7aOnline", MsgUtils.color("<green>Online"));
    }

    @Test
    public void convertsLegacyAndHexOutputToValidMiniMessage() {
        assertEquals("<green>VIP</green>", MsgUtils.toMiniMessage("&aVIP"));
        String output = MsgUtils.toMiniMessage("\u00A7x\u00A7f\u00A7f\u00A78\u00A7c\u00A7b\u00A7c[Tag]");
        assertFalse(output.contains("\u00A7"));
        assertEquals(MiniMessage.miniMessage().deserialize("<#ff8cbc>[Tag]</#ff8cbc>"),
            MiniMessage.miniMessage().deserialize(output));
        assertEquals(MsgUtils.toMiniMessage("#ff8cbc[Tag]"), output);
        MsgUtils.setPattern(true);
        assertEquals(output, MsgUtils.toMiniMessage("&#ff8cbc[Tag]"));
    }

    @Test
    public void preservesNativeMiniMessageEventsAndGradients() {
        MsgUtils.setMiniMessage(true);
        String input = "<hover:show_text:'<gray>Info'><gradient:#ff8cbc:#ffffff>[Tag]</gradient></hover>";
        Component expected = MiniMessage.miniMessage().deserialize(input);
        assertEquals(expected, MiniMessage.miniMessage().deserialize(MsgUtils.toMiniMessage(input)));
        assertEquals("[Tag]", MsgUtils.stripFormatting(input));
    }

    @Test
    public void stripsMixedFormattingAndPreservesLiteralText() {
        MsgUtils.setMiniMessage(true);
        String input = "<red>[VIP] &lName\u00A7r <player>";
        assertEquals("[VIP] Name <player>", MsgUtils.stripFormatting(input));
        String output = MsgUtils.toMiniMessage(input);
        assertFalse(output.contains("\u00A7"));
        assertEquals("[VIP] Name <player>", PlainTextComponentSerializer.plainText().serialize(
            MiniMessage.miniMessage().deserialize(output)));
    }

    @Test
    public void keepsMiniMessageLiteralWhenDisabledAndHandlesEmptyTags() {
        assertEquals("<red>VIP", MsgUtils.stripFormatting("<red>&aVIP"));
        assertTrue(MsgUtils.toMiniMessage("<red>&aVIP").contains("\\<red>"));
        assertEquals("", MsgUtils.toMiniMessage(""));
        assertEquals("", MsgUtils.stripFormatting(""));
        assertEquals("VIP", MsgUtils.stripFormatting("&a&lVIP\u00A7r"));
    }
}
