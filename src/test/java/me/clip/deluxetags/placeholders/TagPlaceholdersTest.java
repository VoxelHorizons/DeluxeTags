package me.clip.deluxetags.placeholders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TagPlaceholdersTest {
    @Test
    public void reservesOutputSuffixesForPlayerTagsRatherThanIdentifierLookups() {
        TagPlaceholders placeholders = new TagPlaceholders(null);
        // Identifier lookups would access the plugin even with a null player.
        assertEquals("", placeholders.onRequest(null, "tag"));
        assertEquals("", placeholders.onRequest(null, "tag_legacy"));
        assertEquals("", placeholders.onRequest(null, "tag_mini"));
        assertEquals("", placeholders.onRequest(null, "tag_stripped"));
    }
}
