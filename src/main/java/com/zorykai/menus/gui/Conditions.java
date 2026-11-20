package com.zorykai.menus.gui;
import com.google.gson.JsonObject;
import com.zorykai.menus.integrations.cobblemon.CobblemonAccess;
import com.zorykai.menus.util.ItemsJson;
import com.zorykai.menus.shops.InventoryPlan;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import static com.zorykai.menus.config.Json.*;
public final class Conditions {
 private Conditions(){}
 public static boolean visible(ServerPlayerEntity player,JsonObject definition){
  if(definition.has("partyShinySlot")){
   int slot=num(definition,"partyShinySlot",-1);var party=CobblemonAccess.snapshot(player);
   if(!party.available()||!party.occupied(slot)||party.shiny(slot)!=bool(definition,"shinyExpected",true))return false;
  }
  for(var entry:arr(definition,"conditions")){var condition=entry.getAsJsonObject();String type=str(condition,"type","");boolean passed;
   if(type.equals("cobblemon:has_item"))passed=CobblemonAccess.loaded()&&InventoryPlan.available(player.getInventory().main,ItemsJson.read(player.getServer(),obj(condition,"item")))>=num(condition,"quantity",1);
   else if(com.zorykai.menus.api.ZorykaiMenusAPI.conditionRegistered(type))passed=com.zorykai.menus.api.ZorykaiMenusAPI.testCondition(type,player,GSON.toJson(condition));
   else{var party=CobblemonAccess.snapshot(player);passed=party.available()&&switch(type){case "cobblemon:has_pokemon"->party.count()>0;case "cobblemon:min_party"->party.count()>=num(condition,"min",1);case "cobblemon:party_space"->party.free()>=num(condition,"min",1);default->false;};}
   if(!passed)return false;
  }
  return true;
 }
 public static void validate(JsonObject definition,MinecraftServer server){
  for(var entry:arr(definition,"conditions")){var c=entry.getAsJsonObject();String type=str(c,"type","");switch(type){
   case "cobblemon:has_pokemon" -> {}
   case "cobblemon:min_party","cobblemon:party_space" -> {int min=num(c,"min",1);if(min<0||min>6)throw new IllegalArgumentException("conditions.min: intervalo0..6");}
   case "cobblemon:has_item" -> {var item=ItemsJson.read(server,obj(c,"item"));if(!str(obj(c,"item"),"id","").startsWith("cobblemon:")||item.getCount()!=1||num(c,"quantity",1)<1)throw new IllegalArgumentException("conditions.item: item Cobblemon unitário e quantidade positiva exigidos");}
   default -> {if(!com.zorykai.menus.api.ZorykaiMenusAPI.conditionRegistered(type))throw new IllegalArgumentException("conditions.type: não registrado "+type);}
  }}
 }
}
