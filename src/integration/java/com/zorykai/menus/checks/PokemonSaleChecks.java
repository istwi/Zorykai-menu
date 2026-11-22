package com.zorykai.menus.checks;

import com.zorykai.menus.*;
import com.zorykai.menus.pokemon.*;
import com.zorykai.menus.shops.*;
import com.zorykai.menus.economy.impactor.ImpactorGateway;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.*;
import java.util.*;
import java.util.concurrent.*;
import java.math.*;
import java.nio.file.*;
import static com.zorykai.menus.config.Json.*;

/** Isolated real Cobblemon stores and Impactor payments; simulated players, not a visual client. */
public final class PokemonSaleChecks extends MarketChecks {
 Object nativeStorage;Class<?> monType;Journal journal;ImpactorGateway original;
 @Override public void onInitialize(){if(!Boolean.getBoolean("zorykai.pokemonTest"))return;ServerTickEvents.END_SERVER_TICK.register(s->{if(!started&&++ticks==20){started=true;app=ZorykaiMenus.instance;new Thread(()->run(s),"pokemon-sale-checks").start();}});}
 Object invoke(Object target,String method,Object...args){try{for(var m:target.getClass().getMethods())if(m.getName().equals(method)&&m.getParameterCount()==args.length){boolean ok=true;for(int i=0;i<args.length;i++){Class<?> p=m.getParameterTypes()[i];if(p==int.class)p=Integer.class;if(p==boolean.class)p=Boolean.class;if(args[i]!=null&&!p.isInstance(args[i]))ok=false;}if(ok)return m.invoke(target,args);}throw new NoSuchMethodException(method);}catch(Exception e){throw new RuntimeException(e);}}
 Object party(ServerPlayerEntity p){return invoke(nativeStorage,"getParty",p);}
 Object add(ServerPlayerEntity p,String properties){try{var type=Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonProperties");Object companion=type.getField("Companion").get(null);Object parsed=invoke(companion,"parse",properties);Object mon=invoke(parsed,"create");if(!(Boolean)invoke(party(p),"add",mon))throw new IllegalStateException("Fixture party full");return mon;}catch(Exception e){throw new RuntimeException(e);}}
 void seed(ServerPlayerEntity p){act(()->{invoke(party(p),"clearParty");add(p,"magikarp level=15");add(p,"pikachu level=20");});}
 PokemonStorage.Mon first(ServerPlayerEntity p){return server(()->{try{return app.pokemonSales.storage.partyList(p).getFirst();}catch(Exception e){throw new RuntimeException(e);}});}
 int count(ServerPlayerEntity p){return server(()->{try{return app.pokemonSales.storage.partyList(p).size();}catch(Exception e){throw new RuntimeException(e);}});}
 boolean present(ServerPlayerEntity p,UUID uuid){return server(()->{try{return app.pokemonSales.storage.anywhere(p,uuid);}catch(Exception e){throw new RuntimeException(e);}});}
 void sell(ServerPlayerEntity p,PokemonStorage.Mon mon){act(()->{try{var price=app.pokemonSales.quote(p,mon).total();app.pokemonSales.submit(p,mon,price,app.catalog.get().revision());}catch(Exception e){throw new RuntimeException(e);}});}
 void recover(ServerPlayerEntity p){act(()->{try{app.recover(p);}catch(Exception e){throw new RuntimeException(e);}});}
 JsonObject pending(ServerPlayerEntity p){return server(()->{try{return journal.get(p.getUuid());}catch(Exception e){throw new RuntimeException(e);}});}
 void waitStage(ServerPlayerEntity p,String stage)throws Exception{for(int i=0;i<200;i++){var tx=pending(p);if(tx!=null&&str(tx,"stage","").equals(stage))return;Thread.sleep(25);}throw new IllegalStateException("stage timeout "+stage);}
 @Override void run(MinecraftServer s){
  try{
   var field=ZorykaiMenus.class.getDeclaredField("journal");field.setAccessible(true);journal=(Journal)field.get(app);original=app.economy;
   Class<?> c=Class.forName("com.cobblemon.mod.common.Cobblemon");nativeStorage=invoke(c.getField("INSTANCE").get(null),"getStorage");monType=Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
   x=server(()->{try{return player(s,"PokemonSeller");}catch(Exception e){throw new RuntimeException(e);}});y=server(()->{try{return player(s,"OtherOwner");}catch(Exception e){throw new RuntimeException(e);}});
   seed(x);seed(y);final var initial=first(x);
   check("Magikarp common fishing reference is exactly 250",server(()->{try{return app.pokemonSales.quote(x,initial).total();}catch(Exception e){throw new RuntimeException(e);}}).compareTo(new BigDecimal("250"))==0);
   if(Boolean.getBoolean("zorykai.liveDatapackTest")){
    var routes=server(()->{try{return app.pokemonSales.storage.liveBuckets();}catch(Exception e){throw new RuntimeException(e);}});
    check("active custom datapack common route is visible",routes.get("beldum").equals("common"));
    var liveMon=new PokemonStorage.Mon(0,UUID.randomUUID(),"cobblemon:beldum","Beldum","Normal",false,10,"test","{}");
    check("active easier route lowers Beldum price to 250",server(()->{try{return app.pokemonSales.quote(x,liveMon).total();}catch(Exception e){throw new RuntimeException(e);}}).compareTo(new BigDecimal("250"))==0);
    var legend=new PokemonStorage.Mon(0,UUID.randomUUID(),"cobblemon:arceus","Arceus","Normal",false,80,"test","{}");
    check("CCC rare legendary route enables configured ultra-rare valuation",server(()->{try{return app.pokemonSales.quote(x,legend).total();}catch(Exception e){throw new RuntimeException(e);}}).compareTo(new BigDecimal("7500"))==0);
   }
   BigDecimal start=balance(x);act(()->s.getCommandManager().executeWithPrefix(x.getCommandSource(),"zmenu pokemon"));click(x,10,0);check("party click opens a real confirmation inventory",server(()->x.currentScreenHandler.getSlot(13).getStack().getItem())==Items.PAPER);
   click(x,15,0);check("cancel never removes Pokemon or pays money",present(x,initial.uuid())&&balance(x).compareTo(start)==0);
   act(()->app.pokemonSales.confirm(x,initial.uuid()));click(x,11,0);waitDone();check("confirmed sale credits real Impactor balance once",balance(x).compareTo(start.add(new BigDecimal("250")))==0);check("sold UUID is absent from both real party and PC",!present(x,initial.uuid()));check("unsold Pokemon remains in its native party",count(x)==1);
   sell(x,initial);Thread.sleep(100);check("replaying a sold UUID never pays again",balance(x).compareTo(start.add(new BigDecimal("250")))==0);
   seed(x);var mon=first(x);var gate=new CreditGate();act(()->app.economy=gate);start=balance(x);var offered=mon;sell(x,offered);check("real deposit is saved before delayed response",gate.saved.get(10,TimeUnit.SECONDS));
   sell(x,offered);check("shared journal blocks repeated Pokemon sale while payment is pending",pending(x)!=null&&balance(x).compareTo(start.add(new BigDecimal("250")))==0);gate.release.complete(null);waitDone();act(()->app.economy=original);gate.close();
   seed(x);offered=first(x);String originalPokemon=offered.snapshot();var rejected=new RejectedCredit();act(()->app.economy=rejected);start=balance(x);sell(x,offered);waitDone();check("known rejected payment returns the same UUID without credit",present(x,offered.uuid())&&balance(x).compareTo(start)==0);
   final var restoredId=offered.uuid();String returned=server(()->{try{return app.pokemonSales.storage.partyList(x).stream().filter(v->v.uuid().equals(restoredId)).findFirst().orElseThrow().snapshot();}catch(Exception e){throw new RuntimeException(e);}});check("refund restores complete native Pokemon metadata",parse(originalPokemon).equals(parse(returned)));act(()->app.economy=original);rejected.close();
   seed(x);offered=first(x);var lost=new LostCredit();act(()->app.economy=lost);start=balance(x);sell(x,offered);waitStage(x,"POKEMON_CREDIT_INTENT");check("lost acknowledgement actually credited Impactor",lost.deposited.get(10,TimeUnit.SECONDS));Thread.sleep(150);recover(x);Thread.sleep(150);check("ambiguous payment is not automatically repeated",balance(x).compareTo(start.add(new BigDecimal("250")))==0&&pending(x)!=null);
   final var appliedTx=pending(x);act(()->{try{app.resolve(x.getUuid(),str(appliedTx,"id",""),true);}catch(Exception e){throw new RuntimeException(e);}});check("admin applied attestation closes sale without second payment",pending(x)==null&&!present(x,offered.uuid())&&balance(x).compareTo(start.add(new BigDecimal("250")))==0);act(()->app.economy=original);lost.close();
   seed(x);offered=first(x);var noCredit=new MissingCredit();act(()->app.economy=noCredit);start=balance(x);sell(x,offered);waitStage(x,"POKEMON_CREDIT_INTENT");Thread.sleep(100);final var unresolved=pending(x);act(()->{try{app.resolve(x.getUuid(),str(unresolved,"id",""),false);app.recover(x);}catch(Exception e){throw new RuntimeException(e);}});waitDone();check("admin not-applied attestation returns Pokemon without creating money",present(x,offered.uuid())&&balance(x).compareTo(start)==0);act(()->app.economy=original);noCredit.close();
   seed(x);act(()->invoke(invoke(party(x),"get",0),"swapHeldItem",new ItemStack(Items.DIAMOND),false,false));offered=first(x);start=balance(x);sell(x,offered);Thread.sleep(100);check("held item prevents sale and remains equipped",present(x,offered.uuid())&&balance(x).compareTo(start)==0&&server(()->!((ItemStack)invoke(invoke(party(x),"get",0),"getHeldItem$common")).isEmpty()));
   seed(x);act(()->invoke(invoke(party(x),"get",0),"setTradeable",false));offered=first(x);sell(x,offered);check("non-tradeable Pokemon cannot be sold",present(x,offered.uuid()));
   seed(x);var foreign=first(y);start=balance(x);sell(x,foreign);Thread.sleep(100);check("another player's UUID cannot be sold",present(y,foreign.uuid())&&balance(x).compareTo(start)==0);
   act(()->invoke(party(x),"clearParty"));act(()->add(x,"magikarp level=15"));offered=first(x);sell(x,offered);check("last party Pokemon is preserved",present(x,offered.uuid()));
   seed(x);offered=first(x);long revision=server(()->app.catalog.get().revision());var oldOffer=offered;BigDecimal oldPrice=server(()->{try{return app.pokemonSales.quote(x,oldOffer).total();}catch(Exception e){throw new RuntimeException(e);}});act(()->app.catalog.reload(s,true));start=balance(x);act(()->app.pokemonSales.submit(x,oldOffer,oldPrice,revision));check("stale configuration confirmation cannot sell or pay",present(x,offered.uuid())&&balance(x).compareTo(start)==0);
   // On-disk evidence independent from the live cache and archive with full snapshot.
   seed(x);offered=first(x);var sold=offered;sell(x,sold);waitDone();check("native party file is durable before sale completion",!present(x,sold.uuid()));
   check("no Pokemon sale changed normal item inventory",server(()->x.getInventory().main.stream().allMatch(ItemStack::isEmpty)));
   var report=new JsonObject();report.addProperty("player",x.getUuidAsString());report.addProperty("soldPokemon",sold.uuid().toString());report.addProperty("balance",balance(x).toPlainString());Files.writeString(Path.of("pokemon-sale-restart-witness.json"),GSON.toJson(report));
  }catch(Throwable error){var entry=new JsonObject();entry.addProperty("fatal",error.toString());results.add(entry);ZorykaiMenus.LOG.error("POKEMON SALES FAIL",error);}
  finally{try{Files.writeString(Path.of("pokemon-sale-results.json"),GSON.toJson(results));}catch(Exception e){ZorykaiMenus.LOG.error("Pokemon report",e);}act(()->s.stop(false));}
 }
 static final class LostCredit extends ImpactorGateway {
  final CompletableFuture<Boolean> deposited=new CompletableFuture<>();LostCredit(){super(()->"impactor:dollars");}
  @Override public CompletableFuture<Boolean> change(UUID id,String currency,BigDecimal amount,boolean deposit){return super.change(id,currency,amount,deposit).thenCompose(ok->{deposited.complete(ok);return CompletableFuture.failedFuture(new java.io.IOException("TEST lost provider response"));});}
 }
 static final class MissingCredit extends ImpactorGateway {
  MissingCredit(){super(()->"impactor:dollars");}
  @Override public CompletableFuture<Boolean> change(UUID id,String currency,BigDecimal amount,boolean deposit){return CompletableFuture.failedFuture(new java.io.IOException("TEST no deposit acknowledgement"));}
 }
}
