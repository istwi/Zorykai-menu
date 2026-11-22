package com.zorykai.menus.checks;

import com.zorykai.menus.*;
import com.zorykai.menus.shops.Journal;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.*;
import net.minecraft.server.network.*;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.*;
import java.util.*;
import java.nio.file.*;
import java.math.*;
import java.util.concurrent.*;
import static com.zorykai.menus.config.Json.*;

/** Second server process, same on-disk Pokemon stores, wallet and SQLite journal. */
public final class PokemonRestartChecks extends MarketChecks {
 Journal journal;PokemonSaleChecks helper;
 @Override public void onInitialize(){if(!Boolean.getBoolean("zorykai.pokemonRestart"))return;ServerTickEvents.END_SERVER_TICK.register(s->{if(!started&&++ticks==20){started=true;app=ZorykaiMenus.instance;new Thread(()->run(s),"pokemon-restart-checks").start();}});}
 JsonArray ids(){return server(()->{try{var a=new JsonArray();for(var m:app.pokemonSales.storage.partyList(x))a.add(m.slot()+":"+m.uuid());return a;}catch(Exception e){throw new RuntimeException(e);}});}
 JsonObject tx(com.zorykai.menus.pokemon.PokemonStorage.Mon mon,String stage){var t=new JsonObject();t.addProperty("kind","pokemon_sale");t.addProperty("id",UUID.randomUUID().toString());t.addProperty("player",x.getUuidAsString());t.addProperty("pokemon",mon.uuid().toString());t.addProperty("snapshot",mon.snapshot());t.addProperty("fingerprint",mon.fingerprint());t.addProperty("currency","impactor:dollars");t.addProperty("total","250.00");t.addProperty("stage",stage);return t;}
 ServerPlayerEntity restorePlayer(MinecraftServer s,UUID id)throws Exception{
  var p=new ServerPlayerEntity(s,s.getOverworld(),new GameProfile(id,"PokemonSeller"),SyncedClientOptions.createDefault());p.networkHandler=FakePlayer.get(s.getOverworld(),new GameProfile(UUID.randomUUID(),"RestartConnection")).networkHandler;p.networkHandler.player=p;
  for(var f:PlayerManager.class.getDeclaredFields())if(f.getGenericType() instanceof ParameterizedType type&&type.getRawType()==Map.class&&Arrays.equals(type.getActualTypeArguments(),new Type[]{UUID.class,ServerPlayerEntity.class})){f.setAccessible(true);((Map<UUID,ServerPlayerEntity>)f.get(s.getPlayerManager())).put(id,p);break;}
  s.getPlayerManager().getPlayerList().add(p);return p;
 }
 @Override void run(MinecraftServer s){try{
  var witness=parse(Files.readString(Path.of("pokemon-sale-restart-witness.json")));UUID owner=UUID.fromString(str(witness,"player","")),sold=UUID.fromString(str(witness,"soldPokemon",""));
  var f=ZorykaiMenus.class.getDeclaredField("journal");f.setAccessible(true);journal=(Journal)f.get(app);
  x=server(()->{try{return restorePlayer(s,owner);}catch(Exception e){throw new RuntimeException(e);}});
  helper=new PokemonSaleChecks();helper.app=app;Class<?> c=Class.forName("com.cobblemon.mod.common.Cobblemon");helper.nativeStorage=helper.invoke(c.getField("INSTANCE").get(null),"getStorage");
  check("sold UUID stays absent after a real server restart",!server(()->{try{return app.pokemonSales.storage.anywhere(x,sold);}catch(Exception e){throw new RuntimeException(e);}}));
  BigDecimal paid=new BigDecimal(str(witness,"balance","0"));check("Impactor balance survives restart without duplicate payment",balance(x).compareTo(paid)==0);
  act(()->{try{app.recover(x);}catch(Exception e){throw new RuntimeException(e);}});check("recover after completed sale is a no-op",balance(x).compareTo(paid)==0);
  act(()->helper.add(x,"magikarp level=15"));var mon=server(()->{try{return app.pokemonSales.storage.partyList(x).stream().filter(v->v.species().endsWith(":magikarp")).findFirst().orElseThrow();}catch(Exception e){throw new RuntimeException(e);}});
  var removal=tx(mon,"POKEMON_REMOVE_INTENT");removal.add("beforeIds",ids());final var id=mon.uuid();act(()->{try{app.pokemonSales.storage.remove(x,id);}catch(Exception e){throw new RuntimeException(e);}});removal.add("afterIds",ids());
  act(()->{try{app.pokemonSales.storage.durable(x).get(5,TimeUnit.SECONDS);journal.put(removal);app.recover(x);}catch(Exception e){throw new RuntimeException(e);}});waitDone();check("durable removal intent resumes with one payment",balance(x).compareTo(paid.add(new BigDecimal("250")))==0);paid=balance(x);
  act(()->{try{app.recover(x);}catch(Exception e){throw new RuntimeException(e);}});check("repeated recovery never credits a second time",balance(x).compareTo(paid)==0);
  act(()->helper.add(x,"magikarp level=24 shiny=true"));var refundMon=server(()->{try{return app.pokemonSales.storage.partyList(x).stream().filter(v->v.species().endsWith(":magikarp")).findFirst().orElseThrow();}catch(Exception e){throw new RuntimeException(e);}});var refund=tx(refundMon,"POKEMON_RETURN_PENDING");
  act(()->{try{app.pokemonSales.storage.remove(x,refundMon.uuid());app.pokemonSales.storage.durable(x).get(5,TimeUnit.SECONDS);journal.put(refund);app.recover(x);}catch(Exception e){throw new RuntimeException(e);}});waitDone();
  var after=server(()->{try{return app.pokemonSales.storage.partyList(x).stream().filter(v->v.uuid().equals(refundMon.uuid())).findFirst().orElseThrow();}catch(Exception e){throw new RuntimeException(e);}});
  check("pending return restores shiny level and all serialized metadata",parse(after.snapshot()).equals(parse(refundMon.snapshot()))&&after.shiny()&&after.level()==24);check("pending return does not credit money",balance(x).compareTo(paid)==0);
 }catch(Throwable e){var j=new JsonObject();j.addProperty("fatal",e.toString());results.add(j);ZorykaiMenus.LOG.error("POKEMON RESTART FAIL",e);}
 finally{try{Files.writeString(Path.of("pokemon-restart-results.json"),GSON.toJson(results));}catch(Exception e){throw new RuntimeException(e);}act(()->s.stop(false));}}
}
