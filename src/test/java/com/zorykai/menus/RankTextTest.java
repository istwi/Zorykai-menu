package com.zorykai.menus;
import com.zorykai.menus.placeholders.RankText;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RankTextTest {
 private String json(String prefix){return GsonComponentSerializer.gson().serialize(RankText.render(prefix)).toLowerCase(java.util.Locale.ROOT);}
 @Test void sectionAndMiniMessageMixedPrefixDoesNotFallBackToLiteralBold(){String s=json("§4§lOWNER<bold>");assertFalse(s.contains("<bold>"));assertFalse(s.contains("§"));assertTrue(s.contains("owner"));assertTrue(s.contains("\"bold\":true"));assertTrue(s.contains("dark_red")||s.contains("#aa0000"));}
 @Test void ampersandAndHexFormatsAreSupported(){String s=json("&#12ab34<bold>OWNER</bold>");assertTrue(s.contains("#12ab34"));assertTrue(s.contains("\"bold\":true"));assertFalse(s.contains("<bold>"));assertTrue(json("&c&lVIP").contains("\"bold\":true"));}
 @Test void expandedLegacyHexAndRegularMiniMessageStillWork(){assertTrue(json("§x§f§f§a§a§0§0&lVIP").contains("gold")||json("§x§f§f§a§a§0§0&lVIP").contains("#ffaa00"));String s=json("<red><bold>OWNER</bold></red>");assertTrue(s.contains("\"bold\":true"));assertFalse(s.contains("<bold>"));}
 @Test void literalAmpersandsStayLiteral(){assertTrue(json("Owner & Staff").contains("owner & staff"));}
}
