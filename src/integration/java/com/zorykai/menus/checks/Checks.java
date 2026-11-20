package com.zorykai.menus.checks;
import com.zorykai.menus.*;
import com.zorykai.menus.gui.*;
import com.zorykai.menus.shops.*;
import com.zorykai.menus.util.*;
import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import net.impactdev.impactor.api.economy.EconomyService;
import net.kyori.adventure.key.Key;
import java.nio.file.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static com.zorykai.menus.config.Json.*;
public final class Checks implements ModInitializer {
 private ZorykaiMenus app;private ServerPlayerEntity p;private final JsonArray results=new JsonArray();private volatile boolean running;
 @Override public void onInitialize(){CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(CommandManager.literal("zcheck").requires(s->s.hasPermissionLevel(4)).executes(c->{
  if(!Boolean.getBoolean("zorykai.integrationTest")||!"127.0.0.1".equals(c.getSource().getServer().getServerIp()))throw new IllegalStateException("Only isolated loopback test server");
  if(running)return 0;running=true;app=(ZorykaiMenus)FabricLoader.getInstance().getEntrypoints("main",ModInitializer.class).stream().filter(v->v instanceof ZorykaiMenus).findFirst().orElseThrow();
  p=c.getSource().getServer().getPlayerManager().getPlayerList().getFirst();new Thread(this::run,"zorykai-real-checks").start();return 1;
 })));}
 private <T>T server(Supplier<T> call){var out=new CompletableFuture<T>();app.server.execute(()->{try{out.complete(call.get());}catch(Throwable e){out.completeExceptionally(e);}});return out.join();}
 private void act(Runnable call){server(()->{call.run();return true;});}
 private void check(String name,boolean ok){var j=new JsonObject();j.addProperty("name",name);j.addProperty("passed",ok);results.add(j);ZorykaiMenus.LOG.info("CHECK {}: {}",ok?"PASS":"FAIL",name);if(!ok)throw new AssertionError(name);}
 private BigDecimal balance(){return app.economy.balance(p.getUuid()).join();}
 private void funds(String amount){var eco=EconomyService.instance();var c=eco.currencies().currency(Key.key("impactor:dollars")).orElseThrow();var account=eco.account(c,p.getUuid()).join();if(!account.set(new BigDecimal(amount)).successful())throw new IllegalStateException("test fund setup failed");eco.save(account).join();}
 private void clear(){act(()->{p.closeHandledScreen();p.getInventory().clear();});}
 private int count(ItemStack item){return server(()->InventoryPlan.available(p.getInventory().main,item));}
 private void waitDone()throws Exception{for(int i=0;i<200;i++){if(server(()->{try{return app.trades.pending()==0;}catch(Exception e){throw new RuntimeException(e);}}))return;Thread.sleep(50);}throw new IllegalStateException("pending: "+server(()->{try{return app.trades.status(p.getUuid());}catch(Exception e){return e.toString();}}));}
 private void trade(boolean sell,int n){act(()->app.trades.submit(p,"example","stone",sell,n,app.catalog.get().revision()));}
 private void click(int slot,int button){act(()->p.currentScreenHandler.onSlotClick(slot,button,SlotActionType.PICKUP,p));try{Thread.sleep(70);}catch(Exception e){throw new RuntimeException(e);}}
 private void deferredDelivery(Runnable interruption,boolean disconnected)throws Exception{
  var original=app.economy;var delayed=new DeferredDebit();
  try{
   act(()->{app.economy=delayed;app.trades.submit(p,"example","stone",false,1,app.catalog.get().revision());});
   if(!delayed.saved.get(10,TimeUnit.SECONDS))throw new IllegalStateException("debit refused in fixture");
   act(interruption);
   if(disconnected){for(int i=0;i<100&&server(()->app.server.getPlayerManager().getPlayer(p.getUuid())!=null);i++)Thread.sleep(50);}
  }finally{act(()->app.economy=original);delayed.release.complete(null);waitDone();delayed.close();}
 }
 private void run(){try{
  clear();funds("10000");ItemStack stone=new ItemStack(Items.STONE);
  act(()->app.menus.open(p,false,"main",1,false));check("native menu 27 slots",server(()->p.currentScreenHandler instanceof LockedMenu m&&m.getInventory().size()==27));
  check("all six native inventory sizes",server(()->{for(int rows=1;rows<=6;rows++){var m=new LockedMenu(99,p.getInventory(),p,rows);if(m.getInventory().size()!=rows*9)return false;}return true;}));
  var before=server(()->InventoryPlan.copy(p.getInventory().main));
  act(()->{var m=p.currentScreenHandler;for(var action:SlotActionType.values())for(int button:new int[]{0,1,2,8,40}){m.onSlotClick(11,button,action,p);m.onSlotClick(27,button,action,p);m.onSlotClick(-999,button,action,p);}});
  check("all native inventory actions cannot extract icons",server(()->InventoryPlan.same(before,p.getInventory().main)&&p.currentScreenHandler.getCursorStack().isEmpty()));

  act(()->app.menus.open(p,false,"main",1,false));var permissionScreen=server(()->p.currentScreenHandler);
  act(()->app.server.getCommandManager().executeWithPrefix(app.server.getCommandSource(),"lp user "+p.getGameProfile().getName()+" permission set zorykaimenus.open false"));Thread.sleep(500);click(11,0);
  check("revoked menu permission rechecked on click",server(()->p.currentScreenHandler==permissionScreen));
  act(()->app.server.getCommandManager().executeWithPrefix(app.server.getCommandSource(),"lp user "+p.getGameProfile().getName()+" permission unset zorykaimenus.open"));Thread.sleep(500);
  act(()->app.menus.open(p,false,"main",1,false));click(26,0);check("next page",server(()->((LockedMenu)p.currentScreenHandler).getInventory().getStack(13).isOf(Items.BOOK)));click(18,0);check("previous page",server(()->((LockedMenu)p.currentScreenHandler).getInventory().getStack(11).isOf(Items.EMERALD)));
  click(11,0);check("open shop action",server(()->((LockedMenu)p.currentScreenHandler).getInventory().getStack(11).isOf(Items.STONE)));
  click(11,0);check("purchase confirmation 54 slots",server(()->p.currentScreenHandler instanceof LockedMenu m&&m.getInventory().size()==54));
  act(()->{for(var action:SlotActionType.values())if(action!=SlotActionType.PICKUP){for(int button:new int[]{0,1,2,8,40}){p.currentScreenHandler.onSlotClick(13,button,action,p);p.currentScreenHandler.onSlotClick(54,button,action,p);}}});
  check("active confirmation rejects shift drag swap throw clone",server(()->p.currentScreenHandler.getCursorStack().isEmpty()&&InventoryPlan.same(before,p.getInventory().main)));click(30,0);
  check("quick quantity 16 and exact total40",server(()->((LockedMenu)p.currentScreenHandler).getInventory().getStack(13).get(DataComponentTypes.LORE).lines().stream().anyMatch(v->v.getString().contains("40"))));click(49,0);waitDone();check("real Impactor purchase exact balance and16items",count(stone)==16&&balance().compareTo(new BigDecimal("9960"))==0);
  act(()->{app.trades.submit(p,"example","stone",false,2,app.catalog.get().revision());app.trades.submit(p,"example","stone",false,2,app.catalog.get().revision());});waitDone();check("duplicate simultaneous requests charged once",count(stone)==18&&balance().compareTo(new BigDecimal("9955"))==0);
  trade(true,10);waitDone();check("real sell exact credit and removal",count(stone)==8&&balance().compareTo(new BigDecimal("9967.50"))==0);
  act(()->app.menus.open(p,true,"example",1,false));click(11,1);check("right click opens sale confirmation",server(()->p.currentScreenHandler instanceof LockedMenu m&&m.getInventory().size()==54));click(34,0);click(49,0);waitDone();check("sell all eligible product",count(stone)==0&&balance().compareTo(new BigDecimal("9977.50"))==0);
  clear();BigDecimal money=balance();trade(true,1);waitDone();check("selling without items rejected",balance().compareTo(money)==0&&count(stone)==0);
  var named=new ItemStack(Items.STONE,8);named.set(DataComponentTypes.CUSTOM_NAME,Text.literal("Protected"));act(()->p.getInventory().main.set(0,named));trade(true,1);waitDone();check("native custom components protected from plain-item sale",count(named)==8&&balance().compareTo(money)==0);
  clear();act(()->{for(int i=0;i<36;i++)p.getInventory().main.set(i,new ItemStack(Items.DIRT,64));});trade(false,1);waitDone();check("full inventory rejects before withdrawal",balance().compareTo(money)==0);
  clear();funds("1");trade(false,1);waitDone();check("insufficient funds no delivery no debit",balance().compareTo(BigDecimal.ONE)==0&&count(stone)==0);
  funds("10000");long revision=server(()->app.catalog.get().revision());act(()->app.trades.submit(p,"example","stone",false,1,revision-1));waitDone();check("stale price revision rejected",balance().compareTo(new BigDecimal("10000"))==0&&count(stone)==0);
  act(()->app.server.getCommandManager().executeWithPrefix(app.server.getCommandSource(),"lp user "+p.getGameProfile().getName()+" permission set zorykaimenus.buy false"));Thread.sleep(500);trade(false,1);waitDone();check("LuckPerms denial at transaction execution",count(stone)==0&&balance().compareTo(new BigDecimal("10000"))==0);
  act(()->app.server.getCommandManager().executeWithPrefix(app.server.getCommandSource(),"lp user "+p.getGameProfile().getName()+" permission unset zorykaimenus.buy"));Thread.sleep(500);
  Path config=ZorykaiMenus.ROOT.resolve("menus/second.json");String original=Files.readString(config);Files.writeString(config,"{bad json");var errors=server(()->app.catalog.reload(app.server,true));check("invalid reload retains active snapshot",!errors.isEmpty()&&server(()->app.catalog.get().revision())==revision);Files.writeString(config,original);check("valid reload",server(()->app.catalog.reload(app.server,true)).isEmpty());
  check("registered Cobblemon item codec",server(()->ItemsJson.read(app.server,parse("{\"id\":\"cobblemon:poke_ball\"}")).getItem()!=Items.AIR));
  check("invalid registered id rejected",server(()->{try{ItemsJson.read(app.server,parse("{\"id\":\"cobblemon:not_a_real_item\"}"));return false;}catch(Exception e){return true;}}));

  funds("1000000");
  var ball=server(()->arr(app.catalog.get().shops().get("loja_pokebolas"),"products").asList().stream().map(e->e.getAsJsonObject()).filter(e->str(obj(e,"item"),"id","").equals("cobblemon:poke_ball")).findFirst().orElseThrow());
  var ballItem=server(()->ItemsJson.read(app.server,obj(ball,"item")));BigDecimal ballTotal=ball.get("buy").getAsBigDecimal().multiply(BigDecimal.valueOf(2));
  act(()->app.trades.submit(p,"loja_pokebolas",str(ball,"id",""),false,2,app.catalog.get().revision()));waitDone();check("migrated Cobblemon purchase and original price",count(ballItem)==2&&balance().compareTo(new BigDecimal("1000000").subtract(ballTotal))==0);
  clear();var tm=server(()->arr(app.catalog.get().shops().get("loja_tms_bug"),"products").get(0).getAsJsonObject());var tmItem=server(()->ItemsJson.read(app.server,obj(tm,"item")));BigDecimal previous=balance();
  act(()->app.trades.submit(p,"loja_tms_bug",str(tm,"id",""),false,1,app.catalog.get().revision()));waitDone();check("native Cobblemon TM component survives purchase",count(tmItem)==1&&balance().compareTo(previous.subtract(tm.get("buy").getAsBigDecimal()))==0);
  clear();funds("10000");
  Path ecoPath=ZorykaiMenus.ROOT.resolve("economy.json");String ecoOriginal=Files.readString(ecoPath);Files.writeString(ecoPath,"{\"currency\":\"impactor:checks_missing_currency\"}");check("fault injection config loaded",server(()->app.catalog.reload(app.server,true)).isEmpty());
  trade(false,1);waitDone();check("unregistered currency fails before any monetary intent",server(()->{try{return app.trades.pending()==0;}catch(Exception e){throw new RuntimeException(e);}})&&count(stone)==0);
  Files.writeString(ecoPath,ecoOriginal);check("economy restored",server(()->app.catalog.reload(app.server,true)).isEmpty());
  var originalGateway=app.economy;
  var losingResponse=new com.zorykai.menus.economy.impactor.ImpactorGateway(()->"impactor:dollars"){
   @Override public CompletableFuture<Boolean> change(UUID player,String key,BigDecimal amount,boolean deposit){return super.change(player,key,amount,deposit).thenApply(ok->{if(!deposit&&ok)throw new CompletionException(new java.io.IOException("injected lost response after real account save"));return ok;});}
  };
  act(()->app.economy=losingResponse);trade(false,1);Thread.sleep(800);
  check("lost response after real debit preserves pending",server(()->{try{return app.trades.pending()==1;}catch(Exception e){throw new RuntimeException(e);}})&&balance().compareTo(new BigDecimal("9997.50"))==0&&count(stone)==0);
  act(()->app.trades.recover(p));Thread.sleep(100);check("unknown finance not retried automatically",server(()->{try{return app.trades.pending()==1;}catch(Exception e){throw new RuntimeException(e);}}));
  String pending=server(()->{try{return app.trades.status(p.getUuid());}catch(Exception e){throw new RuntimeException(e);}});String txid=pending.split(" · ")[0];
  act(()->{app.economy=originalGateway;try{app.trades.resolve(p.getUuid(),txid,true);app.trades.recover(p);}catch(Exception e){throw new RuntimeException(e);}});losingResponse.close();waitDone();check("confirmed debit attestation safely refunds once",balance().compareTo(new BigDecimal("10000"))==0&&count(stone)==0);

  // Wait for actual persisted debit, then control interruption before releasing the response.
  clear();funds("10000");deferredDelivery(()->{if(!app.catalog.reload(app.server,true).isEmpty())throw new IllegalStateException("reload failed");},false);check("confirmed debit safely refunded after catalog change",balance().compareTo(new BigDecimal("10000"))==0&&count(stone)==0);
  deferredDelivery(()->{for(int i=0;i<36;i++)p.getInventory().main.set(i,new ItemStack(Items.DIRT,64));},false);check("capacity rechecked after async debit and refunded",balance().compareTo(new BigDecimal("10000"))==0&&count(stone)==0);
  clear();deferredDelivery(()->p.networkHandler.disconnect(Text.literal("isolated disconnect-during-purchase test")),true);check("real disconnect during purchase refunded without delivery",balance().compareTo(new BigDecimal("10000"))==0&&count(stone)==0);

 }catch(Throwable e){var j=new JsonObject();j.addProperty("fatal",e.toString());results.add(j);ZorykaiMenus.LOG.error("INTEGRATION CHECK FAILED",e);}finally{try{Files.writeString(ZorykaiMenus.ROOT.resolve("integration-results.json"),GSON.toJson(results));}catch(Exception e){ZorykaiMenus.LOG.error("test report write",e);}running=false;}}
}
