package com.zorykai.menus;
import com.zorykai.menus.migration.LegacyExpressions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LegacyExpressionsTest {
 @Test void supportsAllSixSuppliedSlots(){for(int i=0;i<6;i++)assertEquals(i,LegacyExpressions.shinySlot("t.party = q.gui.player.party; t.pokemon = t.party.get_pokemon("+i+"); t.pokemon.is_shiny;").orElseThrow());}
 @Test void unknownOrAdditionalCodeFailsClosed(){assertTrue(LegacyExpressions.shinySlot("t.party = q.gui.player.party; t.pokemon = t.party.get_pokemon(6); t.pokemon.is_shiny;").isEmpty());assertTrue(LegacyExpressions.message("q.gui.player.tell('test'); q.other();").isEmpty());assertEquals("test",LegacyExpressions.message("q.gui.player.tell('test');").orElseThrow());}
}
