package com.zorykai.menus.checks;
import com.zorykai.menus.*;
import com.zorykai.menus.commands.Commands;
import com.zorykai.menus.config.Catalog;
import com.zorykai.menus.gui.LockedMenu;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import java.nio.file.*;
import java.util.*;
import static com.zorykai.menus.config.Json.*;
import static net.minecraft.server.command.CommandManager.literal;
public final class OptionalMainChecks extends MarketChecks {
 int originalCalls;
 @Override public void onInitialize(){if(!Boolean.getBoolean("zorykai.optionalMainTest"))return;ServerTickEvents.END_SERVER_TICK.register(s->{if(!started&&++ticks==20){started=true;app=ZorykaiMenus.instance;new Thread(()->run(s),"optional-main-checks").start();}});}
 void cmd(String c){act(()->app.server.getCommandManager().executeWithPrefix(x.getCommandSource(),c));}
 boolean closed(){return server(()->x.currentScreenHandler==x.playerScreenHandler);}
 @Override void run(MinecraftServer s){try{
  x=server(()->{try{return player(s,"PriorityViewer");}catch(Exception e){throw new RuntimeException(e);}});
  check("fresh server starts with no example menus or shops",server(()->app.catalog.get().menus().isEmpty()&&app.catalog.get().shops().isEmpty()));
  check("empty mainMenu does not block validation",server(()->app.catalog.reload(s,true)).isEmpty());
  Path config=ZorykaiMenus.ROOT.resolve("config.json");JsonObject cfg=parse(Files.readString(config));cfg.addProperty("mainMenu","deleted_main");Files.writeString(config,GSON.toJson(cfg));check("stale deleted mainMenu reference no longer blocks reload",server(()->app.catalog.reload(s,true)).isEmpty());
  act(()->{app.menus.back(x);s.getCommandManager().executeWithPrefix(x.getCommandSource(),"zmenu");});check("back and bare player command work safely without main menu",closed());
  act(()->s.getPlayerManager().addToOperators(x.getGameProfile()));cmd("zmenu");check("admin gets safe no-main response rather than a missing menu error",closed());act(()->s.getPlayerManager().removeFromOperators(x.getGameProfile()));
  Files.writeString(ZorykaiMenus.ROOT.resolve("menus/selected.json"),"{\"id\":\"selected\",\"size\":27}");Files.writeString(ZorykaiMenus.ROOT.resolve("shops/selected_shop.json"),"{\"id\":\"selected_shop\",\"size\":27}");
  cfg.add("aliases",parse("{\"help\":{\"menu\":\"selected\"},\"kit\":{\"menu\":\"selected\"},\"warps\":{\"menu\":\"selected\"},\"leaveoriginal\":{\"menu\":\"selected\"}}"));Files.writeString(config,GSON.toJson(cfg));check("custom menus load without main or second examples",server(()->app.catalog.reload(s,true)).isEmpty());
  act(()->{var d=s.getCommandManager().getDispatcher();for(String name:List.of("kit","warps","leaveoriginal")){var old=d.register(literal(name).executes(c->{originalCalls++;return 1;}).then(literal("oldsub").executes(c->{originalCalls++;return 1;})));d.register(literal("origin:"+name).executes(c->{originalCalls++;return 1;}).redirect(old));}Commands.aliases(app,d);});
  cmd("kit");check("conflict remains original without explicit priority",closed()&&originalCalls==1);
  cfg.add("aliasOverrides",parse("{\"names\":[\"help\",\"kit\",\"warps\"]}").getAsJsonArray("names"));Files.writeString(config,GSON.toJson(cfg));check("priority configuration validates",server(()->app.catalog.reload(s,true)).isEmpty());act(()->Commands.aliases(app,s.getCommandManager().getDispatcher()));
  for(String alias:List.of("help","kit","warps")){cmd(alias);check("explicit priority opens selected menu for /"+alias,server(()->x.currentScreenHandler instanceof LockedMenu));act(()->x.closeHandledScreen());}
  int count=originalCalls;cmd("kit oldsub");check("old unqualified subcommands are not merged into menu alias",closed()&&originalCalls==count);cmd("origin:kit");check("existing namespaced original remains available",closed()&&originalCalls==count+1);
  cmd("leaveoriginal");check("unselected conflicting command remains unchanged",closed()&&originalCalls==count+2);
  cmd("zmenu open selected");cmd("zmenu shop selected_shop");check("priority aliases do not unlock generic player commands",closed());
  act(()->s.getCommandManager().executeWithPrefix(s.getCommandSource(),"zmenu open selected "+x.getGameProfile().getName()));check("trusted console opens selected menu without main",server(()->x.currentScreenHandler instanceof LockedMenu&&!x.hasPermissionLevel(4)));act(()->x.closeHandledScreen());
  act(()->s.getCommandManager().executeWithPrefix(s.getCommandSource(),"zmenu shop selected_shop "+x.getGameProfile().getName()));check("trusted console opens selected shop without examples",server(()->x.currentScreenHandler instanceof LockedMenu));act(()->x.closeHandledScreen());
  Files.writeString(ZorykaiMenus.ROOT.resolve("menus/selected.json"),"{\"id\":\"selected\",\"permission\":\"never.allow\",\"size\":27}");act(()->app.catalog.reload(s,true));cmd("kit");check("overridden alias preserves viewer target permission",closed());
  Files.delete(ZorykaiMenus.ROOT.resolve("menus/selected.json"));check("deleting the remaining menu still permits reload",server(()->app.catalog.reload(s,true)).isEmpty());act(()->{try{Catalog.seed(ZorykaiMenus.ROOT);}catch(Exception e){throw new RuntimeException(e);}});check("seeding again never recreates deleted examples or custom menu",!Files.exists(ZorykaiMenus.ROOT.resolve("menus/main.json"))&&!Files.exists(ZorykaiMenus.ROOT.resolve("menus/second.json"))&&!Files.exists(ZorykaiMenus.ROOT.resolve("menus/selected.json")));
  cmd("kit");check("removed alias target cannot use stale menu data",closed());
 }catch(Throwable e){var j=new JsonObject();j.addProperty("fatal",e.toString());results.add(j);ZorykaiMenus.LOG.error("OPTIONAL MAIN CHECK FAILED",e);}finally{try{Files.writeString(Path.of("optional-main-results.json"),GSON.toJson(results));}catch(Exception e){throw new RuntimeException(e);}act(()->s.stop(false));}}
}
