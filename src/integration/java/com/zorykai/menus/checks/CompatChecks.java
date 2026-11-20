package com.zorykai.menus.checks;
import com.zorykai.menus.*;
import com.zorykai.menus.gui.Conditions;
import com.zorykai.menus.placeholders.NativePlaceholders;
import com.zorykai.menus.migration.Migrator;
import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.util.UUID;
import java.math.BigDecimal;
import net.kyori.adventure.text.Component;
import static com.zorykai.menus.config.Json.*;
public final class CompatChecks implements ModInitializer {
 public void onInitialize(){CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(CommandManager.literal("zcompat").requires(s->s.hasPermissionLevel(4)).executes(ctx->{
  var server=ctx.getSource().getServer();if(!Boolean.getBoolean("zorykai.integrationTest")||!server.getServerIp().equals("127.0.0.1"))return 0;
  var results=new JsonArray();
  try{
   var app=(ZorykaiMenus)FabricLoader.getInstance().getEntrypoints("main",ModInitializer.class).stream().filter(x->x instanceof ZorykaiMenus).findFirst().orElseThrow();
   var player=new ServerPlayerEntity(server,server.getOverworld(),new GameProfile(UUID.randomUUID(),"ZorykaiCompat"),SyncedClientOptions.createDefault());
   var condition=parse("{\"partyShinySlot\":0,\"shinyExpected\":true}");
   check(results,"empty party condition fails closed",!Conditions.visible(player,condition));
   var empty=com.zorykai.menus.integrations.cobblemon.CobblemonAccess.snapshot(player);
   check(results,"advanced empty party has six free slots",empty.available()&&empty.count()==0&&empty.free()==6);
   check(results,"empty first Pokemon placeholder is safe",NativePlaceholders.resolve("cobblemon:first_name",player,null).equals(Component.text("N/D")));
   check(results,"advanced empty team conditions",!Conditions.visible(player,parse("{\"conditions\":[{\"type\":\"cobblemon:has_pokemon\"}]}"))&&Conditions.visible(player,parse("{\"conditions\":[{\"type\":\"cobblemon:party_space\",\"min\":6}]}")));

   Class<?> cobblemon=Class.forName("com.cobblemon.mod.common.Cobblemon");var instance=cobblemon.getField("INSTANCE").get(null);var storage=cobblemon.getMethod("getStorage").invoke(instance);var party=storage.getClass().getMethod("getParty",ServerPlayerEntity.class).invoke(storage,player);
   Class<?> properties=Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonProperties");var companion=properties.getField("Companion").get(null);var props=companion.getClass().getMethod("parse",String.class).invoke(companion,"pikachu shiny=true");var pokemon=properties.getMethod("create").invoke(props);
   party.getClass().getMethod("set",int.class,pokemon.getClass()).invoke(party,0,pokemon);
   check(results,"real Cobblemon shiny Pokemon accepted",Conditions.visible(player,condition));
   var occupied=com.zorykai.menus.integrations.cobblemon.CobblemonAccess.snapshot(player);
   check(results,"advanced populated party exposes name and level",occupied.count()==1&&occupied.free()==5&&!occupied.firstName().isBlank()&&occupied.firstLevel()>0);
   check(results,"read only snapshot is stable",occupied.equals(com.zorykai.menus.integrations.cobblemon.CobblemonAccess.snapshot(player)));
   player.getInventory().main.set(0,com.zorykai.menus.util.ItemsJson.read(server,parse("{\"id\":\"cobblemon:poke_ball\"}")));
   check(results,"actual Cobblemon item condition",Conditions.visible(player,parse("{\"conditions\":[{\"type\":\"cobblemon:has_item\",\"item\":{\"id\":\"cobblemon:poke_ball\"}}]}")));

   pokemon.getClass().getMethod("setShiny",boolean.class).invoke(pokemon,false);
   check(results,"real Cobblemon non shiny rejected",!Conditions.visible(player,condition));condition.addProperty("shinyExpected",false);
   check(results,"expected false condition accepted",Conditions.visible(player,condition));
   check(results,"Impactor dollars alias uses supplied exact balance",Component.text("123.45").equals(NativePlaceholders.resolve("shirokai:dollars",player,new BigDecimal("123.45"))));
   check(results,"unknown provider not invented",NativePlaceholders.resolve("zorykai_checks:absent",player,null)==null);
   check(results,"actual Core cash provider resolves",NativePlaceholders.resolve("shirokai:cash",player,null)!=null);
   check(results,"actual Core token provider resolves",NativePlaceholders.resolve("shirokai:tokens",player,null)!=null);
   check(results,"actual Dex percent provider resolves",NativePlaceholders.resolve("shirokaidex:percent national",player,null)!=null);
   check(results,"missing scoreboard objective not fabricated",NativePlaceholders.resolve("player:objective checks_absent",player,null)==null);
   var scoreboard=server.getScoreboard();var objective=scoreboard.addObjective("zcompat_gyms",net.minecraft.scoreboard.ScoreboardCriterion.DUMMY,net.minecraft.text.Text.literal("checks"),net.minecraft.scoreboard.ScoreboardCriterion.RenderType.INTEGER,false,null);
   try{scoreboard.getOrCreateScore(player,objective).setScore(7);check(results,"native scoreboard placeholder reads real score",Component.text("7").equals(NativePlaceholders.resolve("player:objective zcompat_gyms",player,null)));}finally{scoreboard.removeObjective(objective);}

   var missingIcon=parse("{\"id\":\"zorykai_checks:not_an_item\",\"fallback\":{\"id\":\"minecraft:paper\"}}");
   check(results,"missing icon uses explicit vanilla fallback",com.zorykai.menus.util.ItemsJson.readIcon(server,missingIcon).fallback());
   check(results,"fallback does not change original id",str(missingIcon,"id","").equals("zorykai_checks:not_an_item"));
   check(results,"trade item remains strict despite icon fallback",strictItem(server,missingIcon));
   new Migrator(app).run();check(results,"native migrated catalog validates",app.catalog.reload(server,true).isEmpty());
   check(results,"all four expression menus active",app.catalog.get().menus().keySet().stream().filter(x->x.startsWith("example_molang")).count()==4);
  }catch(Throwable failure){var result=new JsonObject();result.addProperty("fatal",failure.toString());results.add(result);ZorykaiMenus.LOG.error("COMPAT CHECK FAILED",failure);}
  try{Files.writeString(ZorykaiMenus.ROOT.resolve("compatibility-results.json"),GSON.toJson(results));}catch(Exception ex){throw new RuntimeException(ex);}return 1;
 })));}
 private boolean strictItem(net.minecraft.server.MinecraftServer server,JsonObject item){try{com.zorykai.menus.util.ItemsJson.read(server,item);return false;}catch(RuntimeException expected){return true;}}
 private void check(JsonArray results,String name,boolean passed){var j=new JsonObject();j.addProperty("name",name);j.addProperty("passed",passed);results.add(j);ZorykaiMenus.LOG.info("COMPAT {}: {}",passed?"PASS":"FAIL",name);if(!passed)throw new AssertionError(name);}
}
