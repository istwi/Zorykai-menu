package com.zorykai.menus;
import com.google.gson.*;
import com.zorykai.menus.pokemon.*;
import com.zorykai.menus.shops.Journal;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.math.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.zorykai.menus.config.Json.*;
class PokemonSalesLimitsTest {
 JsonObject cfg()throws Exception{try(var in=getClass().getResourceAsStream("/defaults/pokemon-sales.json")){return parse(new String(in.readAllBytes()));}}
 PokemonPrices prices(){return new PokemonPrices(parse("{\"fish\":{\"bucket\":\"common\"},\"dragon\":{\"bucket\":\"rare\",\"parents\":[\"fish\"]}}"));}
 @Test void speciesRarityDoesNotInheritCheapEvolutionRoute(){assertTrue(prices().dailyLimited("dragon",Map.of()));assertFalse(prices().dailyLimited("fish",Map.of()));assertFalse(prices().dailyLimited("dragon",Map.of("dragon","uncommon")));assertTrue(prices().dailyLimited("unknown",Map.of()));}
 @Test void quotaCountsOnlySuccessfulRareSalesAndSurvivesRestart()throws Exception{
  Path file=Files.createTempDirectory("pokemon-quota").resolve("journal.db");UUID player=UUID.randomUUID();long start=System.currentTimeMillis()-1000,end=start+100000;
  try(var j=new Journal(file)){
   for(int i=0;i<12;i++){var tx=new JsonObject();tx.addProperty("id","tx"+i);tx.addProperty("player",player.toString());tx.addProperty("pokemon",UUID.randomUUID().toString());tx.addProperty("dailyLimited",i<11);j.put(tx);j.finishPokemon(tx,i==10?"cancelled":"sold");j.finishPokemon(tx,i==10?"cancelled":"sold");}
   assertEquals(10,j.pokemonSoldBetween(player,start,end));assertEquals(0,j.pokemonSoldBetween(UUID.randomUUID(),start,end));assertEquals(0,j.pokemonSoldBetween(player,end,end+100000));
  }
  try(var j=new Journal(file)){assertEquals(10,j.pokemonSoldBetween(player,start,end));}
 }
 @Test void invalidMultipliersLimitsAndTimezoneAreRejected()throws Exception{for(String key:List.of("rareDailyLimit")){var c=cfg();c.addProperty(key,0);assertThrows(Exception.class,()->PokemonPrices.validate(c));}var c=cfg();c.addProperty("dailyTimezone","invalid/zone");assertThrows(Exception.class,()->PokemonPrices.validate(c));}
}
