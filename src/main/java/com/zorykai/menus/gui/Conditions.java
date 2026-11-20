package com.zorykai.menus.gui;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import static com.zorykai.menus.config.Json.*;
public final class Conditions {
 private Conditions(){}
 public static boolean visible(ServerPlayerEntity player,JsonObject button){
  if(!button.has("partyShinySlot"))return true;
  int slot=num(button,"partyShinySlot",-1);if(slot<0||slot>5||!FabricLoader.getInstance().isModLoaded("cobblemon"))return false;
  try{
   Class<?> cobblemon=Class.forName("com.cobblemon.mod.common.Cobblemon");Object instance=cobblemon.getField("INSTANCE").get(null);
   Object storage=cobblemon.getMethod("getStorage").invoke(instance);
   Object party=storage.getClass().getMethod("getParty",ServerPlayerEntity.class).invoke(storage,player);
   Object pokemon=party.getClass().getMethod("get",int.class).invoke(party,slot);
   return pokemon!=null&&(Boolean)pokemon.getClass().getMethod("getShiny").invoke(pokemon)==bool(button,"shinyExpected",true);
  }catch(ReflectiveOperationException e){com.zorykai.menus.ZorykaiMenus.LOG.debug("Condição Cobblemon indisponível",e);return false;}
 }
}
