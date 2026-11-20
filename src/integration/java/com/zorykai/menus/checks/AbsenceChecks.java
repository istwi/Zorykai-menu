package com.zorykai.menus.checks;
import com.zorykai.menus.*;
import com.zorykai.menus.gui.Conditions;
import com.zorykai.menus.integrations.cobblemon.CobblemonAccess;
import com.zorykai.menus.placeholders.NativePlaceholders;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import com.mojang.authlib.GameProfile;
import com.google.gson.*;
import java.util.*;
import java.nio.file.*;
import java.math.BigDecimal;
import static com.zorykai.menus.config.Json.*;
public final class AbsenceChecks implements ModInitializer {
 public void onInitialize(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
  if(!Boolean.getBoolean("zorykai.integrationTest")||!server.getServerIp().equals("127.0.0.1")||FabricLoader.getInstance().isModLoaded("cobblemon"))return;
  server.execute(()->{var results=new JsonArray();try{
   var app=ZorykaiMenus.instance;var player=new ServerPlayerEntity(server,server.getOverworld(),new GameProfile(UUID.randomUUID(),"AbsentCheck"),SyncedClientOptions.createDefault());
   check(results,"server starts without Cobblemon or LuckPerms",!FabricLoader.getInstance().isModLoaded("luckperms")&&app.trades!=null);
   check(results,"optional party returns unavailable",!CobblemonAccess.snapshot(player).available());
   check(results,"placeholder uses safe fallback",NativePlaceholders.resolve("cobblemon:party_count",player,null).equals(net.kyori.adventure.text.Component.text("N/D")));
   check(results,"optional condition fails closed",!Conditions.visible(player,parse("{\"conditions\":[{\"type\":\"cobblemon:has_pokemon\"}]}")));
   var shop=app.catalog.get().shops().get("example");var product=arr(shop,"products").get(0).getAsJsonObject();
   check(results,"normal price without permission provider",app.pricing.quote(player,"example",shop,product,false,1).total().compareTo(new BigDecimal("2.50"))==0);
  }catch(Throwable error){var j=new JsonObject();j.addProperty("fatal",error.toString());results.add(j);ZorykaiMenus.LOG.error("ABSENCE CHECK FAILED",error);}try{Files.writeString(ZorykaiMenus.ROOT.resolve("absence-results.json"),GSON.toJson(results));}catch(Exception e){ZorykaiMenus.LOG.error("absence report",e);}});
 });}
 private void check(JsonArray out,String name,boolean ok){var j=new JsonObject();j.addProperty("name",name);j.addProperty("passed",ok);out.add(j);ZorykaiMenus.LOG.info("ABSENCE {}: {}",ok?"PASS":"FAIL",name);if(!ok)throw new AssertionError(name);}
}
