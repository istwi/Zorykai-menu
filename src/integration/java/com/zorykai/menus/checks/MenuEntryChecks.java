package com.zorykai.menus.checks;

import com.zorykai.menus.*;
import com.zorykai.menus.commands.Commands;
import com.zorykai.menus.gui.LockedMenu;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.Items;
import java.nio.file.*;
import com.google.gson.*;
import static com.zorykai.menus.config.Json.*;

public final class MenuEntryChecks extends MarketChecks {
 @Override public void onInitialize(){if(!Boolean.getBoolean("zorykai.entryTest"))return;ServerTickEvents.END_SERVER_TICK.register(s->{if(!started&&++ticks==20){started=true;app=ZorykaiMenus.instance;new Thread(()->run(s),"menu-entry-checks").start();}});}
 void close(){act(()->x.closeHandledScreen());}
 void playerCommand(String cmd){act(()->app.server.getCommandManager().executeWithPrefix(x.getCommandSource(),cmd));}
 void consoleCommand(String cmd){act(()->app.server.getCommandManager().executeWithPrefix(app.server.getCommandSource(),cmd));}
 boolean closed(){return server(()->x.currentScreenHandler==x.playerScreenHandler);}
 boolean allowedMenu(){return server(()->x.currentScreenHandler instanceof LockedMenu&&x.currentScreenHandler.getSlot(13).getStack().getItem()==Items.PAPER);}
 boolean internalMenu(){return server(()->x.currentScreenHandler instanceof LockedMenu&&x.currentScreenHandler.getSlot(13).getStack().isEmpty());}
 @Override void run(MinecraftServer s){try{
  x=server(()->{try{return player(s,"EntryViewer");}catch(Exception e){throw new RuntimeException(e);}});
  Files.writeString(ZorykaiMenus.ROOT.resolve("menus/entry_allowed.json"),"""
   {"id":"entry_allowed","size":27,"aliases":["entry_local"],"buttons":[{"id":"next","slots":[13],"icon":{"id":"minecraft:paper"},"actions":[{"type":"OPEN_MENU","id":"entry_internal"}]}]}
   """);
  Files.writeString(ZorykaiMenus.ROOT.resolve("menus/entry_internal.json"),"{\"id\":\"entry_internal\",\"size\":27}");
  Files.writeString(ZorykaiMenus.ROOT.resolve("menus/entry_protected.json"),"{\"id\":\"entry_protected\",\"permission\":\"entry.never\",\"size\":27}");
  Path config=ZorykaiMenus.ROOT.resolve("config.json");JsonObject cfg=parse(Files.readString(config));cfg.add("aliases",parse("{\"entry_public\":{\"menu\":\"entry_allowed\"},\"entry_shop\":{\"shop\":\"example\"},\"entry_private\":{\"menu\":\"entry_protected\"}}"));Files.writeString(config,GSON.toJson(cfg));
  check("menu and alias configuration loads",server(()->app.catalog.reload(s,true)).isEmpty());act(()->Commands.aliases(app,s.getCommandManager().getDispatcher()));
  check("ordinary viewer is not OP",!server(()->x.hasPermissionLevel(4)));
  playerCommand("zmenu");check("bare zmenu cannot open a menu for ordinary players",closed());
  playerCommand("zmenu open entry_internal");check("generic open cannot bypass aliases",closed());
  playerCommand("zmenu shop example");check("generic shop cannot bypass aliases",closed());
  playerCommand("zmenu open entry_allowed "+x.getGameProfile().getName());check("player cannot invoke targeted console entry",closed());
  check("generic open command requirement rejects ordinary viewer",!server(()->s.getCommandManager().getDispatcher().getRoot().getChild("zmenu").getChild("open").canUse(x.getCommandSource())));
  playerCommand("entry_public");check("configured public alias opens its chosen menu",allowedMenu());click(x,13,0);check("configured internal navigation still works",internalMenu());
  close();playerCommand("entry_local");check("alias declared in menu JSON still works",allowedMenu());
  close();playerCommand("entry_shop");check("configured shop alias works without generic command permission",!closed());
  close();playerCommand("entry_private");check("alias still enforces target menu permission",closed());
  consoleCommand("zmenu open entry_internal "+x.getGameProfile().getName());check("console opens chosen menu for non-OP viewer",internalMenu());check("console open never grants OP",!server(()->x.hasPermissionLevel(4)));
  close();consoleCommand("zmenu shop example "+x.getGameProfile().getName());check("console opens chosen shop for viewer",!closed());
  close();consoleCommand("zmenu open entry_protected "+x.getGameProfile().getName());check("console still respects viewer target permission",closed());
  act(()->s.getPlayerManager().addToOperators(x.getGameProfile()));playerCommand("zmenu open entry_internal");check("administrator can open own target menu",internalMenu());act(()->s.getPlayerManager().removeFromOperators(x.getGameProfile()));close();
  obj(cfg,"aliases").getAsJsonObject("entry_public").addProperty("menu","entry_internal");Files.writeString(config,GSON.toJson(cfg));check("alias retarget reload succeeds",server(()->app.catalog.reload(s,true)).isEmpty());playerCommand("entry_public");check("existing alias resolves new target after reload",internalMenu());close();
  obj(cfg,"aliases").remove("entry_public");Files.writeString(config,GSON.toJson(cfg));act(()->app.catalog.reload(s,true));playerCommand("entry_public");check("removed alias cannot use a stale registered target",closed());
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("luckperms")){
   var user=net.luckperms.api.LuckPermsProvider.get().getUserManager().loadUser(x.getUuid()).join();
   var grant=net.luckperms.api.node.Node.builder(com.zorykai.menus.commands.MenuEntry.ADMIN).build();
   user.data().add(grant);user.getCachedData().invalidate();playerCommand("zmenu open entry_internal");check("explicit command permission allows own open without OP",internalMenu()&&!server(()->x.hasPermissionLevel(4)));close();
   user.data().remove(grant);user.getCachedData().invalidate();playerCommand("zmenu open entry_internal");check("revoked direct command permission blocks the next open",closed());
   user.data().add(net.luckperms.api.node.types.PrefixNode.builder("§4§lOWNER<bold>",100).build());user.getCachedData().invalidate();
   String rank=server(()->net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(com.zorykai.menus.placeholders.NativePlaceholders.resolve("luckperms:prefix",x,null)));
   check("real LuckPerms mixed prefix contains no literal bold markup",rank.contains("OWNER")&&!rank.contains("<bold>")&&!rank.contains("§"));
   check("real LuckPerms prefix preserves bold and red color",rank.contains("\"bold\":true")&&(rank.contains("dark_red")||rank.contains("#aa0000")));
  }
  playerCommand("zmenu recover");check("own transaction recovery remains available",closed());
  playerCommand("zmenu pokemon open "+x.getGameProfile().getName());check("Pokemon console-only opening remains protected",closed());consoleCommand("zmenu pokemon open "+x.getGameProfile().getName());check("existing trusted Pokemon console command remains functional",server(()->x.currentScreenHandler instanceof LockedMenu&&x.currentScreenHandler.getSlot(49).getStack().getItem()==Items.BARRIER));
 }catch(Throwable e){var row=new JsonObject();row.addProperty("fatal",e.toString());results.add(row);ZorykaiMenus.LOG.error("MENU ENTRY CHECK FAILED",e);}finally{try{Files.writeString(Path.of("menu-entry-results.json"),GSON.toJson(results));}catch(Exception e){throw new RuntimeException(e);}act(()->s.stop(false));}}
}
