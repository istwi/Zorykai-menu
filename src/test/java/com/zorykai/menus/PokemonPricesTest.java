package com.zorykai.menus;

import org.junit.jupiter.api.Test;
import com.google.gson.*;
import com.zorykai.menus.pokemon.PokemonPrices;
import com.zorykai.menus.shops.Journal;
import java.nio.file.Files;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.zorykai.menus.config.Json.*;

class PokemonPricesTest {
 private JsonObject config(){try(var in=getClass().getResourceAsStream("/defaults/pokemon-sales.json")){return parse(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}}
 private PokemonPrices prices(){return new PokemonPrices(parse("{\"magikarp\":{\"bucket\":\"common\",\"fishing\":true,\"parents\":[]},\"gyarados\":{\"bucket\":\"rare\",\"parents\":[\"magikarp\"]},\"rarity\":{\"bucket\":\"ultra-rare\",\"parents\":[]},\"cyclea\":{\"parents\":[\"cycleb\"]},\"cycleb\":{\"parents\":[\"cyclea\"]}}"));}
 @Test void commonFishingReferenceIs250AndRareSpeciesCostMore(){var cfg=config();PokemonPrices.validate(cfg);assertEquals(new BigDecimal("250.00"),prices().quote(cfg,"cobblemon:magikarp",false,2,Map.of()).total());assertEquals(new BigDecimal("7500.00"),prices().quote(cfg,"rarity",false,2,Map.of()).total());}
 @Test void easyEvolutionCapsAnOtherwiseRareDirectSpawn(){assertEquals(new BigDecimal("312.50"),prices().quote(config(),"gyarados",false,2,Map.of()).total());}
 @Test void liveCommonDatapackRouteLowersAnUltraRareResearchPrice(){assertEquals(new BigDecimal("250.00"),prices().quote(config(),"rarity",false,2,Map.of("rarity","common")).total());}
 @Test void unknownPokemonNeedLiveRouteOrExplicitPriceAndCyclesTerminate(){assertThrows(IllegalArgumentException.class,()->prices().quote(config(),"unknown",false,2,Map.of()));assertThrows(IllegalArgumentException.class,()->prices().quote(config(),"cyclea",false,2,Map.of()));assertEquals(new BigDecimal("250.00"),prices().quote(config(),"newmon",false,2,Map.of("newmon","common")).total());}
 @Test void explicitSpeciesPriceShinyCapAndCurrencyRoundingAreDeterministic(){var c=config();obj(c,"speciesPrices").addProperty("magikarp",30.125);c.addProperty("maxPrice",50);assertEquals(new BigDecimal("30.12"),prices().quote(c,"magikarp",false,2,Map.of()).total());assertEquals(new BigDecimal("50.00"),prices().quote(c,"magikarp",true,2,Map.of()).total());}
 @Test void disabledBlockedAndInvalidConfigurationsCannotPay(){var c=config();c.addProperty("enabled",false);assertThrows(IllegalArgumentException.class,()->prices().quote(c,"magikarp",false,2,Map.of()));c.addProperty("enabled",true);arr(c,"blockedSpecies").add("magikarp");assertThrows(IllegalArgumentException.class,()->prices().quote(c,"magikarp",false,2,Map.of()));for(String k:List.of("referencePrice","maxPrice","shinyMultiplier","evolutionMultiplier")){var invalid=config();invalid.addProperty(k,-1);assertThrows(IllegalArgumentException.class,()->PokemonPrices.validate(invalid));}var bad=config();bad.addProperty("keepParty",0);assertThrows(IllegalArgumentException.class,()->PokemonPrices.validate(bad));}
 @Test void historicalPokemonSnapshotIsArchivedAtomicallyAndCannotDeleteAnotherPendingSale()throws Exception{
  var folder=Files.createTempDirectory("pokemon-journal");try(var j=new Journal(folder.resolve("pending.sqlite"))){
   var tx=parse("{\"id\":\"one\",\"player\":\"00000000-0000-0000-0000-000000000001\",\"kind\":\"pokemon_sale\",\"pokemon\":\"test-mon\",\"snapshot\":\"original data\",\"stage\":\"POKEMON_CREDIT_INTENT\"}");j.put(tx);assertThrows(Exception.class,()->{var duplicate=tx.deepCopy();duplicate.addProperty("id","two");j.put(duplicate);});j.finishPokemon(tx,"sold");assertNull(j.get(UUID.fromString(str(tx,"player",""))));
   var next=tx.deepCopy();next.addProperty("id","next");j.put(next);j.finishPokemon(tx,"sold");assertEquals("next",str(j.get(UUID.fromString(str(tx,"player",""))),"id",""));
  }
  try(var db=java.sql.DriverManager.getConnection("jdbc:sqlite:"+folder.resolve("pending.sqlite"));var q=db.createStatement();var rs=q.executeQuery("SELECT payload,result FROM pokemon_sale_history WHERE id='one'")){assertTrue(rs.next());assertTrue(rs.getString(1).contains("original data"));assertEquals("sold",rs.getString(2));}
 }
}
