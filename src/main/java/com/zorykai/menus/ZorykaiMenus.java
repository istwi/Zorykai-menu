package com.zorykai.menus;
import com.zorykai.menus.config.*;
import com.zorykai.menus.gui.Menus;
import com.zorykai.menus.shops.*;
import com.zorykai.menus.economy.impactor.ImpactorGateway;
import com.zorykai.menus.commands.Commands;
import com.zorykai.menus.placeholders.Texts;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.nio.file.*;
import java.util.*;
import static com.zorykai.menus.config.Json.*;
public final class ZorykaiMenus implements ModInitializer {
 public static final Path ROOT=FabricLoader.getInstance().getConfigDir().resolve("zorykaimenus");
 public static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("zorykaimenus");
 public final Catalog catalog=new Catalog(ROOT);public MinecraftServer server;public Menus menus;public ImpactorGateway economy;public Trades trades;private Journal journal;
 @Override public void onInitialize(){
  try{Catalog.seed(ROOT);}catch(Exception e){throw new IllegalStateException("Falha ao criar configurações",e);}
  CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->Commands.register(this,dispatcher));
  ServerLifecycleEvents.SERVER_STARTED.register(s->{
   server=s;var errors=catalog.reload(s,true);if(!errors.isEmpty())throw new IllegalStateException("Configuração inválida:\n"+String.join("\n",errors));
   try{journal=new Journal(ROOT.resolve("internal/pending.sqlite"));economy=new ImpactorGateway(()->str(catalog.get().economy(),"currency","impactor:dollars"));menus=new Menus(this);trades=new Trades(this,journal);Commands.aliases(this,s.getCommandManager().getDispatcher());
    LOG.info("ZorykaiMenus 1.0.0: {} menus, {} lojas",catalog.get().menus().size(),catalog.get().shops().size());
   }catch(Exception e){throw new IllegalStateException("Falha ao iniciar ZorykaiMenus",e);}
  });
  ServerPlayConnectionEvents.JOIN.register((handler,sender,s)->{try{if(journal.get(handler.player.getUuid())!=null)trades.recover(handler.player);}catch(Exception e){LOG.error("Recuperação indisponível",e);}});
  ServerPlayConnectionEvents.DISCONNECT.register((handler,s)->{if(menus!=null)menus.forget(handler.player.getUuid());});
  ServerLifecycleEvents.SERVER_STOPPED.register(s->{if(economy!=null)economy.close();try{if(journal!=null)journal.close();}catch(Exception e){LOG.error("Falha ao fechar journal",e);}});
 }
 public void literal(ServerPlayerEntity p,String text){p.sendMessage(Texts.text(text,p,null),false);}
 public void message(ServerPlayerEntity p,String key,Map<String,String> replacements){String message=str(catalog.get().messages(),"prefix","")+str(catalog.get().messages(),key,key);for(var e:replacements.entrySet())message=message.replace("{"+e.getKey()+"}",e.getValue());literal(p,message);}
}
