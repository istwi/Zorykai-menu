package com.zorykai.menus.checks;
import com.zorykai.menus.*;
import com.zorykai.menus.api.ZorykaiMenusAPI;
import com.zorykai.menus.gui.*;
import com.zorykai.menus.shops.*;
import com.zorykai.menus.pricing.*;
import com.zorykai.menus.integrations.cobblemon.CobblemonAccess;
import com.zorykai.menus.placeholders.NativePlaceholders;
import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import net.impactdev.impactor.api.economy.EconomyService;
import net.kyori.adventure.key.Key;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.math.*;
import java.nio.file.*;
import static com.zorykai.menus.config.Json.*;
public final class UpgradeChecks implements ModInitializer {
 private ZorykaiMenus app;private ServerPlayerEntity player;private boolean running;private final JsonArray results=new JsonArray();
 private static final String SHOP="upgrade_checks";
 @Override public void onInitialize(){CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(CommandManager.literal("zupgrade").requires(s->s.hasPermissionLevel(4)).executes(c->{
  if(!Boolean.getBoolean("zorykai.integrationTest")||!c.getSource().getServer().getServerIp().equals("127.0.0.1")||running)return 0;
  app=ZorykaiMenus.instance;player=app.server.getPlayerManager().getPlayerList().getFirst();running=true;new Thread(this::run,"zorykai-upgrade-checks").start();return 1;
 })));}
 private <T>T server(Supplier<T> action){var future=new CompletableFuture<T>();app.server.execute(()->{try{future.complete(action.get());}catch(Throwable failure){future.completeExceptionally(failure);}});return future.join();}
 private void act(Runnable action){server(()->{action.run();return true;});}
 private void check(String name,boolean ok){var result=new JsonObject();result.addProperty("name",name);result.addProperty("passed",ok);results.add(result);ZorykaiMenus.LOG.info("UPGRADE {}: {}",ok?"PASS":"FAIL",name);if(!ok)throw new AssertionError(name);}
 private void permission(String node,boolean value){var user=net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(player.getUuid());user.data().clear(n->n.getKey().equals(node));user.data().add(net.luckperms.api.node.Node.builder(node).value(value).build());user.getCachedData().invalidate();}
 private void rank(String name){act(()->{for(String rank:List.of("vip","vipplus","mvp","mvpplus"))permission("zorykaimenus.discount."+rank,rank.equals(name));});}
 private void funds(String amount){var economy=EconomyService.instance();var currency=economy.currencies().currency(Key.key("impactor:dollars")).orElseThrow();var account=economy.account(currency,player.getUuid()).join();if(!account.set(new BigDecimal(amount)).successful())throw new IllegalStateException("funding failed");economy.save(account).join();}
 private BigDecimal balance(){return app.economy.balance(player.getUuid()).join();}
 private void clear(){act(()->{player.closeHandledScreen();player.getInventory().clear();});}
 private int count(ItemStack item){return server(()->InventoryPlan.available(player.getInventory().main,item));}
 private void waitDone()throws Exception{for(int i=0;i<200;i++){if(server(()->{try{return app.trades.pending()==0;}catch(Exception e){throw new RuntimeException(e);}}))return;Thread.sleep(50);}throw new IllegalStateException("transaction timeout");}
 private void click(int slot){act(()->player.currentScreenHandler.onSlotClick(slot,0,SlotActionType.PICKUP,player));try{Thread.sleep(75);}catch(Exception e){throw new RuntimeException(e);}}
 private ZorykaiMenusAPI.Price quote(String id,int quantity){return server(()->ZorykaiMenusAPI.effectivePrice(player,SHOP,id,ZorykaiMenusAPI.Direction.BUY,quantity));}
 private JsonObject product(String id){return arr(app.catalog.get().shops().get(SHOP),"products").asList().stream().map(JsonElement::getAsJsonObject).filter(p->str(p,"id","").equals(id)).findFirst().orElseThrow();}
 private void confirm(String id){act(()->app.menus.confirm(player,new Menus.Location(true,SHOP,1),product(id),false,1,app.catalog.get().revision()));}
 private boolean quantity(int expected){return server(()->((LockedMenu)player.currentScreenHandler).getInventory().getStack(13).get(DataComponentTypes.LORE).lines().stream().anyMatch(t->t.getString().equals("Quantidade: "+expected)));}
 private void maximum(String id)throws Exception{confirm(id);click(34);Thread.sleep(400);}
 private int exampleCounter(String field){try{return ((java.util.concurrent.atomic.AtomicInteger)Class.forName("com.zorykai.example.ExampleAddon").getField(field).get(null)).get();}catch(Exception e){throw new RuntimeException(e);}}
 private void fixtures()throws Exception{
  var shop=parse("{\"id\":\"upgrade_checks\",\"size\":27,\"title\":\"<aqua>Upgrade checks\",\"products\":[]}");var products=arr(shop,"products");
  String[] ids={"stone","no_discount","override","limited","free","cancelled","unavailable"};
  for(int i=0;i<ids.length;i++){var p=parse("{\"id\":\""+ids[i]+"\",\"slots\":["+i+"],\"item\":{\"id\":\"minecraft:stone\"},\"icon\":{\"id\":\"minecraft:stone\"},\"buy\":\"200\",\"sell\":\"10\"}");
   if(ids[i].equals("no_discount"))p.add("discount",parse("{\"enabled\":false}"));
   if(ids[i].equals("override"))p.add("discount",parse("{\"override\":true,\"groups\":[{\"permission\":\"zorykaimenus.discount.vip\",\"discountPercent\":3}]}"));
   if(ids[i].equals("free"))p.addProperty("buy","0");if(ids[i].equals("free")||ids[i].equals("limited"))p.addProperty("maxQuantity",10);
   if(ids[i].equals("unavailable"))p.addProperty("available",false);products.add(p);
  }
  Files.writeString(ZorykaiMenus.ROOT.resolve("shops/upgrade_checks.json"),GSON.toJson(shop));
  Files.writeString(ZorykaiMenus.ROOT.resolve("menus/upgrade_api.json"),"{\"id\":\"upgrade_api\",\"size\":9,\"title\":\"<aqua>API checks\",\"buttons\":[{\"id\":\"custom\",\"slots\":[0],\"icon\":{\"id\":\"minecraft:paper\"},\"name\":\"%zorykai_example:greeting%\",\"actions\":[{\"type\":\"zorykai_example:message\"}]},{\"id\":\"hidden\",\"slots\":[1],\"icon\":{\"id\":\"minecraft:diamond\"},\"conditions\":[{\"type\":\"zorykai_example:allowed\",\"deny\":true}]}]}");
  if(!server(()->app.catalog.reload(app.server,true)).isEmpty())throw new IllegalStateException("fixture validation failed");
 }
 private void run(){Path discount=ZorykaiMenus.ROOT.resolve("discounts.json");String original="";
  try{original=Files.readString(discount);fixtures();clear();rank("normal");funds("10000");ItemStack stone=new ItemStack(Items.STONE);
   check("normal operator has no implicit VIP",quote("stone",1).total().compareTo(new BigDecimal("200"))==0);
   String[] ranks={"vip","vipplus","mvp","mvpplus"};String[] prices={"190","180","170","160"};
   for(int i=0;i<ranks.length;i++){rank(ranks[i]);clear();funds("10000");var q=quote("stone",1);check("VIP quote "+ranks[i],q.total().compareTo(new BigDecimal(prices[i]))==0);act(()->app.trades.submit(player,SHOP,"stone",false,1,app.catalog.get().revision(),q.total()));waitDone();check("real discounted purchase "+ranks[i],count(stone)==1&&balance().compareTo(new BigDecimal("10000").subtract(q.total()))==0);}
   act(()->{for(String rank:ranks)permission("zorykaimenus.discount."+rank,true);});check("multiple permissions select20 without stacking",quote("stone",1).discountPercent().compareTo(new BigDecimal("20"))==0);check("product explicitly disables discount",quote("no_discount",1).total().compareTo(new BigDecimal("200"))==0);check("specific product overrides general",quote("override",1).discountPercent().compareTo(new BigDecimal("3"))==0);
   clear();funds("10000");rank("vip");confirm("stone");rank("normal");click(49);waitDone();check("removed VIP before confirmation rejects stale quote",count(stone)==0&&balance().compareTo(new BigDecimal("10000"))==0);
   rank("vip");var originalGateway=app.economy;var delayed=new DeferredDebit();
   act(()->{app.economy=delayed;app.trades.submit(player,SHOP,"stone",false,1,app.catalog.get().revision());});if(!delayed.saved.get(10,TimeUnit.SECONDS))throw new IllegalStateException("debit refused");
   act(()->{permission("zorykaimenus.discount.vip",false);app.economy=originalGateway;});delayed.release.complete(null);waitDone();delayed.close();check("removed VIP after debit safely refunds",count(stone)==0&&balance().compareTo(new BigDecimal("10000"))==0);
   clear();rank("vipplus");funds("9000");maximum("stone");check("maximum selects50 with real VIP and balance",quantity(50)&&count(stone)==0&&balance().compareTo(new BigDecimal("9000"))==0);
   act(()->{var screen=player.currentScreenHandler;screen.onSlotClick(49,0,SlotActionType.PICKUP,player);screen.onSlotClick(49,0,SlotActionType.PICKUP,player);});waitDone();check("maximum confirmed once despite duplicate click",count(stone)==50&&balance().signum()==0);
   clear();funds("9000");act(()->{for(int i=1;i<36;i++)player.getInventory().main.set(i,new ItemStack(Items.DIRT,64));player.getInventory().main.set(0,new ItemStack(Items.STONE,60));});maximum("stone");check("maximum accounts for partial stack capacity4",quantity(4));
   act(()->player.getInventory().main.set(0,new ItemStack(Items.STONE,64)));confirm("stone");var fullScreen=server(()->player.currentScreenHandler);click(34);Thread.sleep(300);check("full inventory does not select quantity",server(()->player.currentScreenHandler==fullScreen));
   clear();act(()->{for(int i=1;i<36;i++)player.getInventory().main.set(i,new ItemStack(Items.DIRT,64));player.getInventory().main.set(0,new ItemStack(Items.ENDER_PEARL,14));});
   check("native stack maximum16 respected",server(()->InventoryPlan.capacity(player.getInventory().main,new ItemStack(Items.ENDER_PEARL),2304)==2&&InventoryPlan.add(player.getInventory().main,new ItemStack(Items.ENDER_PEARL),2)!=null&&InventoryPlan.add(player.getInventory().main,new ItemStack(Items.ENDER_PEARL),3)==null));
   clear();funds("9000");maximum("limited");check("per product maximum10 respected",quantity(10));
   clear();funds("0");maximum("free");check("zero price selects product limit without division",quantity(10));click(49);waitDone();check("real free purchase changes only inventory",count(stone)==10&&balance().signum()==0);
   clear();funds("1");confirm("stone");var poorScreen=server(()->player.currentScreenHandler);click(34);Thread.sleep(300);check("maximum insufficient balance remains unconfirmed",server(()->player.currentScreenHandler==poorScreen)&&count(stone)==0);
   clear();funds("10000");int after=exampleCounter("PURCHASES");act(()->app.trades.submit(player,SHOP,"cancelled",false,1,app.catalog.get().revision()));waitDone();check("separate mod cancels before wallet or inventory changes",balance().compareTo(new BigDecimal("10000"))==0&&count(stone)==0&&exampleCounter("PURCHASES")==after);
   var observed=new java.util.concurrent.atomic.AtomicReference<ZorykaiMenusAPI.Trade>();AutoCloseable bad=server(()->ZorykaiMenusAPI.afterPurchase(event->{throw new IllegalStateException("expected external observer failure");}));AutoCloseable listener=server(()->ZorykaiMenusAPI.afterPurchase(observed::set));
   var q=quote("stone",2);act(()->app.trades.submit(player,SHOP,"stone",false,2,app.catalog.get().revision()));waitDone();check("post purchase event has confirmed immutable price data",observed.get()!=null&&observed.get().quantity()==2&&observed.get().total().compareTo(q.total())==0&&observed.get().originalTotal().compareTo(new BigDecimal("400"))==0&&count(stone)==2);
   server(()->{try{bad.close();listener.close();}catch(Exception e){throw new RuntimeException(e);}return true;});check("throwing observer does not duplicate or invalidate result",exampleCounter("PURCHASES")==after+1&&balance().compareTo(new BigDecimal("10000").subtract(q.total()))==0);
   int sales=exampleCounter("SALES");BigDecimal beforeSale=balance();
   AutoCloseable saleCancel=server(()->ZorykaiMenusAPI.beforeSale(event->false));act(()->app.trades.submit(player,SHOP,"stone",true,2,app.catalog.get().revision()));waitDone();
   check("before sale cancellation preserves items and balance",count(stone)==2&&balance().compareTo(beforeSale)==0&&exampleCounter("SALES")==sales);
   server(()->{try{saleCancel.close();}catch(Exception e){throw new RuntimeException(e);}return true;});act(()->app.trades.submit(player,SHOP,"stone",true,2,app.catalog.get().revision()));waitDone();check("sale event and full undiscounted price",exampleCounter("SALES")==sales+1&&balance().compareTo(beforeSale.add(new BigDecimal("20")))==0&&count(stone)==0);
   act(()->permission("zorykai.example.action",true));check("separate addon opens menu through API",server(()->ZorykaiMenusAPI.openMenu(player,"upgrade_api")));check("custom placeholder rendered",server(()->((LockedMenu)player.currentScreenHandler).getInventory().getStack(0).getName().getString().startsWith("Olá,")));check("custom condition hides button",server(()->((LockedMenu)player.currentScreenHandler).getInventory().getStack(1).isEmpty()));int actions=exampleCounter("ACTIONS");click(0);check("custom action executes once",exampleCounter("ACTIONS")==actions+1);act(()->permission("zorykai.example.action",false));click(0);check("custom action permission rechecked",exampleCounter("ACTIONS")==actions+1);
   check("public product queries preserve base price",server(()->ZorykaiMenusAPI.product(SHOP,"stone").orElseThrow().buy().orElseThrow().compareTo(new BigDecimal("200"))==0));check("separate API opens shop",server(()->ZorykaiMenusAPI.openShop(player,SHOP)));act(()->permission("zorykaimenus.open",false));check("API open respects core permissions",!server(()->ZorykaiMenusAPI.openMenu(player,"main")));act(()->permission("zorykaimenus.open",true));
   check("Cobblemon party snapshot valid and read only",server(()->{var a=CobblemonAccess.snapshot(player);var b=CobblemonAccess.snapshot(player);return a.available()&&a.count()+a.free()==6&&a.equals(b);}));check("Cobblemon placeholders resolve",server(()->NativePlaceholders.resolve("cobblemon:party_count",player,null)!=null&&NativePlaceholders.resolve("cobblemon:first_level",player,null)!=null));check("native party conditions",server(()->Conditions.visible(player,parse("{\"conditions\":[{\"type\":\"cobblemon:min_party\",\"min\":0}]}"))));
   String valid=Files.readString(discount);long revision=server(()->app.catalog.get().revision());Files.writeString(discount,"{\"maxDiscountPercent\":-1}");check("invalid discounts retain previous snapshot",!server(()->app.catalog.reload(app.server,true)).isEmpty()&&server(()->app.catalog.get().revision())==revision);Files.writeString(discount,valid);check("restore valid discounts",server(()->app.catalog.reload(app.server,true)).isEmpty());
   String errors=Files.readString(ZorykaiMenus.ROOT.resolve("logs/erros.log"));check("errors persist inside config with stack trace",errors.contains("expected external observer failure")&&errors.contains("Validação de configuração"));
  }catch(Throwable failure){var j=new JsonObject();j.addProperty("fatal",failure.toString());results.add(j);ZorykaiMenus.LOG.error("UPGRADE CHECK FAILED",failure);}finally{
   try{if(!original.isBlank())Files.writeString(discount,original);rank("normal");act(()->app.catalog.reload(app.server,true));Files.writeString(ZorykaiMenus.ROOT.resolve("upgrade-results.json"),GSON.toJson(results));}catch(Exception failure){ZorykaiMenus.LOG.error("upgrade report failure",failure);}running=false;
  }
 }
}
