package com.zorykai.menus.checks;
import com.zorykai.menus.*;
import com.zorykai.menus.pokemon.*;
import com.zorykai.menus.shops.*;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.*;
import net.minecraft.server.network.*;
import net.minecraft.item.*;
import java.util.*;
import java.nio.file.*;
import java.math.*;
import static com.zorykai.menus.config.Json.*;
public final class PcSaleChecks extends PokemonSaleChecks {
 @Override public void onInitialize(){if(!Boolean.getBoolean("zorykai.pcTest"))return;ServerTickEvents.END_SERVER_TICK.register(s->{if(!started&&++ticks==20){started=true;app=ZorykaiMenus.instance;new Thread(()->run(s),"pc-sale-checks").start();}});}
 Object pc(ServerPlayerEntity p){return invoke(nativeStorage,"getPC",p);}
 Object addPc(ServerPlayerEntity p,String properties){Object mon=add(p,properties);invoke(party(p),"remove",mon);if(!(Boolean)invoke(pc(p),"add",mon))throw new IllegalStateException("PC full");return mon;}
 @Override PokemonStorage.Mon first(ServerPlayerEntity p){return server(()->{try{return app.pokemonSales.storage.pcList(p).getFirst();}catch(Exception e){throw new RuntimeException(e);}});}
 @Override void seed(ServerPlayerEntity p){act(()->{invoke(pc(p),"clearPC");invoke(party(p),"clearParty");add(p,"pikachu level=20");addPc(p,"magikarp level=15");});}
 @Override void run(MinecraftServer s){try{
  var field=ZorykaiMenus.class.getDeclaredField("journal");field.setAccessible(true);journal=(Journal)field.get(app);original=app.economy;
  var c=Class.forName("com.cobblemon.mod.common.Cobblemon");nativeStorage=invoke(c.getField("INSTANCE").get(null),"getStorage");
  x=server(()->{try{return player(s,"PcSeller");}catch(Exception e){throw new RuntimeException(e);}});y=server(()->{try{return player(s,"PcOther");}catch(Exception e){throw new RuntimeException(e);}});
  seed(x);seed(y);var initial=first(x);

  act(()->app.pokemonSales.open(x));check("PC menu uses six rows",server(()->x.currentScreenHandler.getSlot(49)!=null));check("native Pokemon model in menu",server(()->net.minecraft.registry.Registries.ITEM.getId(x.currentScreenHandler.getSlot(0).getStack().getItem()).toString().equals("cobblemon:pokemon_model")));
  click(x,0,0);check("model also appears in confirmation",server(()->net.minecraft.registry.Registries.ITEM.getId(x.currentScreenHandler.getSlot(13).getStack().getItem()).toString().equals("cobblemon:pokemon_model")));click(x,15,0);check("cancel leaves PC Pokemon untouched",present(x,initial.uuid()));
  BigDecimal before=balance(x),due=server(()->{try{return app.pokemonSales.quote(x,initial).total();}catch(Exception e){throw new RuntimeException(e);}});act(()->app.pokemonSales.confirm(x,initial.uuid()));click(x,11,0);waitDone();check("successful menu sale returns to refreshed PC listing",server(()->x.currentScreenHandler instanceof com.zorykai.menus.gui.LockedMenu&&x.currentScreenHandler.getSlot(49).getStack().getItem()==Items.BARRIER));check("sold Pokemon model disappears from refreshed menu",server(()->x.currentScreenHandler.getSlot(0).getStack().isEmpty()));check("PC sale pays exact quote once",balance(x).compareTo(before.add(due))==0);check("sold PC UUID absent",!present(x,initial.uuid()));check("party remains intact",count(x)==1);sell(x,initial);check("sold UUID cannot sell twice",balance(x).compareTo(before.add(due))==0);
  seed(x);var mon=first(x);var rejected=new RejectedCredit();act(()->app.economy=rejected);sell(x,mon);waitDone();check("rejected payment returns Pokemon to PC",server(()->{try{return app.pokemonSales.storage.pcList(x).stream().anyMatch(v->v.uuid().equals(mon.uuid()));}catch(Exception e){throw new RuntimeException(e);}}));check("PC refund preserves complete metadata",parse(mon.snapshot()).equals(parse(first(x).snapshot())));act(()->app.economy=original);rejected.close();
  seed(x);var stale=first(x);var foreign=first(y);sell(x,foreign);check("another owner's PC UUID cannot sell",present(y,foreign.uuid()));
  act(()->{for(int i=0;i<46;i++)addPc(x,"magikarp level=5");app.pokemonSales.open(x);});check("PC menu has next page button",server(()->x.currentScreenHandler.getSlot(53).getStack().getItem())==Items.ARROW);click(x,53,0);check("second PC page contains native model",server(()->net.minecraft.registry.Registries.ITEM.getId(x.currentScreenHandler.getSlot(0).getStack().getItem()).toString().equals("cobblemon:pokemon_model")));
  // Force 10 successful rare history entries, then try a real eleventh sale.
  seed(x);var priceField=PokemonSales.class.getDeclaredField("prices");priceField.setAccessible(true);var rarityPrices=(PokemonPrices)priceField.get(app.pokemonSales);var liveRoutes=server(()->{try{return app.pokemonSales.storage.liveBuckets();}catch(Exception e){throw new RuntimeException(e);}});String rareSpecies=List.of("absol","axew","bagon","gible","dratini").stream().filter(v->rarityPrices.dailyLimited(v,liveRoutes)).findFirst().orElseThrow();
  act(()->{invoke(pc(x),"clearPC");addPc(x,rareSpecies+" level=10");});var rare=first(x);
  act(()->{try{for(int i=0;i<10;i++){var tx=new JsonObject();tx.addProperty("id",UUID.randomUUID().toString());tx.addProperty("player",x.getUuidAsString());tx.addProperty("pokemon",UUID.randomUUID().toString());tx.addProperty("dailyLimited",true);journal.put(tx);journal.finishPokemon(tx,"sold");}}catch(Exception e){throw new RuntimeException(e);}});
  before=balance(x);sell(x,rare);check("eleventh rare sale blocked before PC removal/payment",present(x,rare.uuid())&&balance(x).compareTo(before)==0&&pending(x)==null);
  act(()->addPc(x,"magikarp level=5"));var common=server(()->{try{return app.pokemonSales.storage.pcList(x).stream().filter(v->v.species().endsWith(":magikarp")).findFirst().orElseThrow();}catch(Exception e){throw new RuntimeException(e);}});sell(x,common);waitDone();check("common still sells after rare quota",!present(x,common.uuid()));
  check("icons never enter real inventory",server(()->x.getInventory().main.stream().allMatch(ItemStack::isEmpty)));
 }catch(Throwable e){var entry=new JsonObject();entry.addProperty("fatal",e.toString());results.add(entry);ZorykaiMenus.LOG.error("PC CHECKS FAIL",e);}finally{try{Files.writeString(Path.of("pc-sale-results.json"),GSON.toJson(results));}catch(Exception e){throw new RuntimeException(e);}act(()->s.stop(false));}}
}
