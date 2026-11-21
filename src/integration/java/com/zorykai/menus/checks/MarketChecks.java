package com.zorykai.menus.checks;
import com.zorykai.menus.*;
import com.zorykai.menus.market.*;
import com.zorykai.menus.gui.*;
import com.zorykai.menus.shops.*;
import com.zorykai.menus.api.ZorykaiMenusAPI;
import com.zorykai.menus.economy.impactor.ImpactorGateway;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.*;
import net.minecraft.server.PlayerManager;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.*;
import net.minecraft.component.DataComponentTypes;
import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.math.*;
import java.nio.file.*;
import static com.zorykai.menus.config.Json.*;

/** Real Minecraft inventory, menus and Impactor payments; no graphical clients. */
public final class MarketChecks implements ModInitializer {
 ZorykaiMenus app;ServerPlayerEntity x,y;boolean started;int ticks,checks;final JsonArray results=new JsonArray();
 @Override public void onInitialize(){if(!Boolean.getBoolean("zorykai.marketTest"))return;ServerTickEvents.END_SERVER_TICK.register(s->{if(!started&&++ticks==20){started=true;app=ZorykaiMenus.instance;new Thread(()->run(s),"market-checks").start();}});}
 <T>T server(Supplier<T> call){var future=new CompletableFuture<T>();app.server.execute(()->{try{future.complete(call.get());}catch(Throwable error){future.completeExceptionally(error);}});return future.join();}
 void act(Runnable call){server(()->{call.run();return true;});}
 void check(String name,boolean success){var j=new JsonObject();j.addProperty("name",name);j.addProperty("passed",success);results.add(j);checks++;if(!success)throw new AssertionError(name);ZorykaiMenus.LOG.info("MARKET PASS {}",name);}
 ServerPlayerEntity player(MinecraftServer server,String name)throws Exception {
  var p=new ServerPlayerEntity(server,server.getOverworld(),new GameProfile(UUID.randomUUID(),name),SyncedClientOptions.createDefault());p.networkHandler=FakePlayer.get(server.getOverworld(),new GameProfile(UUID.randomUUID(),"ConnectionProbe")).networkHandler;p.networkHandler.player=p;
  for(var f:PlayerManager.class.getDeclaredFields())if(f.getGenericType() instanceof ParameterizedType t&&t.getRawType()==Map.class&&Arrays.equals(t.getActualTypeArguments(),new Type[]{UUID.class,ServerPlayerEntity.class})){f.setAccessible(true);((Map<UUID,ServerPlayerEntity>)f.get(server.getPlayerManager())).put(p.getUuid(),p);break;}
  server.getPlayerManager().getPlayerList().add(p);return p;
 }
 JsonObject product(){return arr(app.catalog.get().shops().get("market_checks"),"products").get(0).getAsJsonObject();}
 BigDecimal quote(ServerPlayerEntity p,int q){return server(()->ZorykaiMenusAPI.effectivePrice(p,"market_checks","cactus",ZorykaiMenusAPI.Direction.SELL,q).total());}
 BigDecimal balance(ServerPlayerEntity p){return app.economy.balance(p.getUuid()).join();}
 void waitDone()throws Exception{for(int i=0;i<200;i++){if(server(()->{try{return app.trades.pending()==0;}catch(Exception e){throw new RuntimeException(e);}}))return;Thread.sleep(25);}throw new IllegalStateException("transaction timeout");}
 void click(ServerPlayerEntity p,int slot,int button)throws Exception{act(()->p.currentScreenHandler.onSlotClick(slot,button,SlotActionType.PICKUP,p));Thread.sleep(60);}
 void openConfirm(ServerPlayerEntity p,int q){act(()->{p.closeHandledScreen();app.menus.confirm(p,new Menus.Location(true,"market_checks",1),product(),true,q,app.catalog.get().revision());});}
 void run(MinecraftServer s){
  try {
   String shop="""
    {"id":"market_checks","size":27,"title":"<aqua>Bolsa","market":{"enabled":true,"minimumPercent":25,"dropPercent":1,"volumePerDrop":64,"recoveryVolume":128,"recoverySeconds":3600},"products":[{"id":"cactus","slots":[11],"name":"<green>Cacto","item":{"id":"minecraft:cactus"},"icon":{"id":"minecraft:cactus"},"sell":"1.00","buy":"2.00"},{"id":"fixed","slots":[12],"market":{"enabled":false},"item":{"id":"minecraft:stone"},"icon":{"id":"minecraft:stone"},"sell":"1.00"}]}
    """;
   Files.writeString(ZorykaiMenus.ROOT.resolve("shops/market_checks.json"),shop);check("new shop valid",server(()->app.catalog.reload(s,true)).isEmpty());
   x=server(()->{try{return player(s,"SellerX");}catch(Exception e){throw new RuntimeException(e);}});y=server(()->{try{return player(s,"SellerY");}catch(Exception e){throw new RuntimeException(e);}});
   act(()->{x.getInventory().setStack(0,new ItemStack(Items.CACTUS,64));y.getInventory().setStack(0,new ItemStack(Items.CACTUS,64));});
   BigDecimal beforeX=balance(x);check("base quote1",quote(x,1).compareTo(new BigDecimal("1.00"))==0);check("batch64 includes own downward pressure",quote(x,64).compareTo(new BigDecimal("63.68"))==0);
   act(()->app.menus.open(x,true,"market_checks",1,false));check("shop shows live market lore",server(()->((LockedMenu)x.currentScreenHandler).getInventory().getStack(11).get(DataComponentTypes.LORE).lines().stream().anyMatch(t->t.getString().contains("Bolsa de valores"))));
   openConfirm(y,64);openConfirm(x,64);check("confirmation shows batch total",server(()->((LockedMenu)x.currentScreenHandler).getInventory().getStack(13).get(DataComponentTypes.LORE).lines().stream().anyMatch(t->t.getString().contains("63.68"))));
   click(x,49,0);waitDone();check("real menu sale paid exact total",balance(x).subtract(beforeX).compareTo(new BigDecimal("63.68"))==0);check("real inventory removed64",server(()->InventoryPlan.available(x.getInventory().main,new ItemStack(Items.CACTUS)))==0);
   check("global price falls for another player",quote(y,1).compareTo(new BigDecimal("0.99"))==0);
   BigDecimal beforeY=balance(y);click(y,49,0);waitDone();check("stale other-player confirmation rejected without loss",balance(y).compareTo(beforeY)==0&&server(()->InventoryPlan.available(y.getInventory().main,new ItemStack(Items.CACTUS)))==64);
   check("product override remains static",server(()->ZorykaiMenusAPI.effectivePrice(y,"market_checks","fixed",ZorykaiMenusAPI.Direction.SELL,64).total()).compareTo(new BigDecimal("64.00"))==0);
   check("buy price unchanged",server(()->ZorykaiMenusAPI.effectivePrice(y,"market_checks","cactus",ZorykaiMenusAPI.Direction.BUY,1).total()).compareTo(new BigDecimal("2.00"))==0);
   // Gate an actual provider deposit so a simultaneous seller cannot reuse its quote.
   var original=app.economy;var delayed=new CreditGate();
   act(()->{app.economy=delayed;x.getInventory().setStack(0,new ItemStack(Items.CACTUS,64));app.trades.submit(x,"market_checks","cactus",true,3,app.catalog.get().revision());});check("provider persisted gated deposit",delayed.saved.get(10,TimeUnit.SECONDS));
   act(()->app.trades.submit(y,"market_checks","cactus",true,3,app.catalog.get().revision()));
   check("same product in-flight lock preserves second inventory",server(()->InventoryPlan.available(y.getInventory().main,new ItemStack(Items.CACTUS)))==64);
   delayed.release.complete(null);waitDone();act(()->app.economy=original);delayed.close();check("non-terminating average price accepted",server(()->InventoryPlan.available(x.getInventory().main,new ItemStack(Items.CACTUS)))==61);
   // Provider really saved a payment, but its response is lost: resolve must count volume once, never pay again.
   var uncertain=new CreditGate();BigDecimal beforeUncertain=balance(x);BigDecimal due=quote(x,3);BigDecimal pressureBefore=quote(y,64);
   act(()->{app.economy=uncertain;app.trades.submit(x,"market_checks","cactus",true,3,app.catalog.get().revision());});check("ambiguous provider deposit really saved",uncertain.saved.get(10,TimeUnit.SECONDS));
   uncertain.release.completeExceptionally(new IllegalStateException("expected lost provider confirmation"));act(()->app.economy=original);uncertain.close();
   check("ambiguous payment does not alter market yet",quote(y,64).compareTo(pressureBefore)==0);
   String transaction=server(()->{try{return app.trades.status(x.getUuid()).split(" · ")[0];}catch(Exception failure){throw new RuntimeException(failure);}});
   act(()->s.getCommandManager().executeWithPrefix(s.getCommandSource(),"zmenu resolve "+x.getUuid()+" "+transaction+" applied"));waitDone();
   check("admin financial attestation counts confirmed sale",quote(y,64).compareTo(pressureBefore)<0);check("attestation never credits twice",balance(x).compareTo(beforeUncertain.add(due))==0);
   act(()->s.getCommandManager().executeWithPrefix(s.getCommandSource(),"zmenu resolve "+x.getUuid()+" "+transaction+" applied"));check("repeated attestation cannot duplicate payment",balance(x).compareTo(beforeUncertain.add(due))==0);
   // Rejected payment returns the original items and never lowers market state.
   var failing=new RejectedCredit();BigDecimal pre=quote(y,64);act(()->{app.economy=failing;app.trades.submit(y,"market_checks","cactus",true,64,app.catalog.get().revision());});waitDone();act(()->app.economy=original);failing.close();
   check("rejected credit returns items",server(()->InventoryPlan.available(y.getInventory().main,new ItemStack(Items.CACTUS)))==64);check("rejected credit leaves balance intact",balance(y).compareTo(beforeY)==0);check("rejected credit adds no pressure",quote(y,64).compareTo(pre)==0);
   // Declining observer runs after preparation, but never changes money/pressure/items.
   var cancel=server(()->ZorykaiMenusAPI.beforeSale(event->false));BigDecimal preCancel=quote(y,64);act(()->app.trades.submit(y,"market_checks","cactus",true,64,app.catalog.get().revision()));waitDone();cancel.close();
   check("cancelled sale leaves pressure and items",quote(y,64).compareTo(preCancel)==0&&server(()->InventoryPlan.available(y.getInventory().main,new ItemStack(Items.CACTUS)))==64);
   Files.writeString(ZorykaiMenus.ROOT.resolve("shops/market_checks.json"),shop.replace("\"recoverySeconds\":3600","\"recoverySeconds\":2"));check("short recovery test rule valid",server(()->app.catalog.reload(s,true)).isEmpty());
   Thread.sleep(2200);check("idle gradual recovery restores base",quote(y,1).compareTo(new BigDecimal("1.00"))==0);
   act(()->app.trades.submit(y,"market_checks","cactus",true,64,app.catalog.get().revision()));waitDone();check("another seller lowers shared price",quote(x,1).compareTo(new BigDecimal("0.99"))==0);
   act(()->s.getCommandManager().executeWithPrefix(s.getCommandSource(),"zmenu market reset market_checks cactus"));check("admin reset restores base",quote(x,1).compareTo(new BigDecimal("1.00"))==0);
   BigDecimal beforeBuy=balance(y);act(()->app.trades.submit(y,"market_checks","cactus",false,2,app.catalog.get().revision()));waitDone();
   check("real purchase still debits and delivers",balance(y).compareTo(beforeBuy.subtract(new BigDecimal("4.00")))==0&&server(()->InventoryPlan.available(y.getInventory().main,new ItemStack(Items.CACTUS)))==2);
   BigDecimal beforeFixed=balance(y);act(()->{y.getInventory().setStack(1,new ItemStack(Items.STONE,16));app.trades.submit(y,"market_checks","fixed",true,16,app.catalog.get().revision());});waitDone();
   check("real fixed-price sale preserved",balance(y).compareTo(beforeFixed.add(new BigDecimal("16.00")))==0&&server(()->InventoryPlan.available(y.getInventory().main,new ItemStack(Items.STONE)))==0);
   check("purchase and fixed sale do not change market",quote(x,1).compareTo(new BigDecimal("1.00"))==0);
   // Broken rules must not replace the live catalog.
   long revision=server(()->app.catalog.get().revision());Files.writeString(ZorykaiMenus.ROOT.resolve("shops/market_checks.json"),shop.replace("\"minimumPercent\":25","\"minimumPercent\":-1"));check("invalid market reload preserves snapshot",!server(()->app.catalog.reload(s,true)).isEmpty()&&server(()->app.catalog.get().revision())==revision);Files.writeString(ZorykaiMenus.ROOT.resolve("shops/market_checks.json"),shop);
   check("configuration errors persist in config log",Files.readString(ZorykaiMenus.ROOT.resolve("logs/erros.log")).contains("minimumPercent"));
  }catch(Throwable error){var entry=new JsonObject();entry.addProperty("fatal",error.toString());results.add(entry);ZorykaiMenus.LOG.error("MARKET FAIL",error);}
  finally{try{Files.writeString(Path.of("market-results.json"),GSON.toJson(results));}catch(Exception e){ZorykaiMenus.LOG.error("Market report",e);}act(()->s.stop(false));}
 }
 static final class CreditGate extends ImpactorGateway {
  final CompletableFuture<Boolean> saved=new CompletableFuture<>();final CompletableFuture<Void> release=new CompletableFuture<>();
  CreditGate(){super(()->"impactor:dollars");}
  @Override public CompletableFuture<Boolean> change(UUID id,String key,BigDecimal amount,boolean deposit){var result=super.change(id,key,amount,deposit);if(!deposit)return result;return result.whenComplete((ok,error)->{if(error!=null)saved.completeExceptionally(error);else saved.complete(ok);}).thenCompose(ok->release.thenApply(v->ok));}
 }
 static final class RejectedCredit extends ImpactorGateway {
  RejectedCredit(){super(()->"impactor:dollars");}
  @Override public CompletableFuture<Boolean> change(UUID id,String key,BigDecimal amount,boolean deposit){return deposit?CompletableFuture.completedFuture(false):super.change(id,key,amount,false);}
 }
}
