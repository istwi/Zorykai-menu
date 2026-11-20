package com.zorykai.menus.api;
import com.zorykai.menus.ZorykaiMenus;
import com.zorykai.menus.permissions.Permissions;
import com.zorykai.menus.pricing.*;
import com.google.gson.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.*;
import static com.zorykai.menus.config.Json.*;
/** Public facade over the existing services. Server operations require the server thread. */
public final class ZorykaiMenusAPI {
 public enum Direction { BUY,SELL }
 public record ActionContext(ServerPlayerEntity player,String menuId,boolean shop,int page,int mouseButton,String definitionJson){}
 @FunctionalInterface public interface Action {void execute(ActionContext context);}
 @FunctionalInterface public interface Placeholder {Text resolve(ServerPlayerEntity player,String argument);}
 @FunctionalInterface public interface Condition {boolean test(ServerPlayerEntity player,String definitionJson);}
 public record Product(String shopId,String productId,String category,Optional<BigDecimal> buy,Optional<BigDecimal> sell,String itemJson){}
 public record Price(BigDecimal originalUnit,BigDecimal discountPercent,BigDecimal effectiveUnit,int quantity,BigDecimal originalTotal,BigDecimal total,BigDecimal savings){}
 public record Trade(UUID transactionId,UUID playerId,String shopId,String productId,Direction direction,int quantity,BigDecimal originalUnit,BigDecimal originalTotal,BigDecimal discountPercent,BigDecimal effectiveUnit,BigDecimal total,String currency){}
 private record RegisteredAction(String permission,Action action){}
 private static final ExtensionRegistry<RegisteredAction> ACTIONS=new ExtensionRegistry<>();
 private static final ExtensionRegistry<Placeholder> PLACEHOLDERS=new ExtensionRegistry<>();
 private static final ExtensionRegistry<Condition> CONDITIONS=new ExtensionRegistry<>();
 private ZorykaiMenusAPI(){}
 public static boolean ready(){var app=ZorykaiMenus.instance;return app!=null&&app.server!=null&&app.trades!=null;}
 private static ZorykaiMenus runtime(){if(!ready())throw new IllegalStateException("ZorykaiMenus ainda não está pronto");var app=ZorykaiMenus.instance;if(!app.server.isOnThread())throw new IllegalStateException("Chamada exige thread principal do servidor");return app;}
 private static void registrationThread(){if(ready())runtime();}
 private static void extensionId(String id){if(id!=null&&(id.startsWith("cobblemon:")||id.startsWith("minecraft:")||id.startsWith("zorykaimenus:")))throw new IllegalArgumentException("Use o namespace do seu mod, sem substituir recursos nativos");}
 public static boolean menuExists(String id){return runtime().catalog.get().menus().containsKey(id);}
 public static boolean shopExists(String id){return runtime().catalog.get().shops().containsKey(id);}
 public static boolean openMenu(ServerPlayerEntity player,String id){return open(player,id,false);}
 public static boolean openShop(ServerPlayerEntity player,String id){return open(player,id,true);}
 private static boolean open(ServerPlayerEntity player,String id,boolean shop){
  var app=runtime();if(player.getServer()!=app.server||player.isDisconnected())return false;var snapshot=app.catalog.get();var data=(shop?snapshot.shops():snapshot.menus()).get(id);
  if(data==null||!app.menus.allowed(player,data,"")||!Permissions.has(player,str(snapshot.permissions(),"use","zorykaimenus.use"),true)||!Permissions.has(player,str(snapshot.permissions(),shop?"shop":"open","zorykaimenus."+(shop?"shop":"open")),true))return false;
  var previous=player.currentScreenHandler;app.menus.open(player,shop,id,1,true);return player.currentScreenHandler!=previous;
 }
 public static AutoCloseable registerAction(String id,String permission,Action action){registrationThread();extensionId(id);if(permission==null||permission.isBlank())throw new IllegalArgumentException("Permissão da ação obrigatória");return ACTIONS.register(id,new RegisteredAction(permission,Objects.requireNonNull(action)));}
 public static AutoCloseable registerPlaceholder(String id,Placeholder placeholder){registrationThread();extensionId(id);return PLACEHOLDERS.register(id,placeholder);}
 public static AutoCloseable registerCondition(String id,Condition condition){registrationThread();extensionId(id);return CONDITIONS.register(id,condition);}
 public static boolean actionRegistered(String id){return ACTIONS.contains(id);}
 public static boolean conditionRegistered(String id){return CONDITIONS.contains(id);}
 public static void executeAction(String id,ActionContext context){
  runtime();var registration=ACTIONS.get(id);if(registration==null)throw new IllegalArgumentException("Ação não registrada: "+id);
  if(!Permissions.has(context.player(),registration.permission,false))throw new IllegalArgumentException("Sem permissão para ação: "+registration.permission);
  registration.action.execute(context);
 }
 public static Text resolvePlaceholder(String token,ServerPlayerEntity player){
  int split=token.indexOf(' ');var registration=PLACEHOLDERS.get(split<0?token:token.substring(0,split));if(registration==null)return null;
  runtime();try{return registration.resolve(player,split<0?"":token.substring(split+1));}catch(RuntimeException|LinkageError failure){ZorykaiMenus.LOG.warn("Placeholder de extensão falhou: "+token,failure);return null;}
 }
 public static boolean testCondition(String id,ServerPlayerEntity player,String json){runtime();var registration=CONDITIONS.get(id);if(registration==null)return false;try{return registration.test(player,json);}catch(RuntimeException|LinkageError failure){ZorykaiMenus.LOG.warn("Condição de extensão falhou: "+id,failure);return false;}}
 public static Optional<Product> product(String shopId,String productId){
  var app=runtime();var shop=app.catalog.get().shops().get(shopId);if(shop==null)return Optional.empty();
  for(var element:arr(shop,"products")){var p=element.getAsJsonObject();if(str(p,"id","").equals(productId))return Optional.of(new Product(shopId,productId,str(p,"category",str(shop,"category","")),p.has("buy")?Optional.of(p.get("buy").getAsBigDecimal()):Optional.empty(),p.has("sell")?Optional.of(p.get("sell").getAsBigDecimal()):Optional.empty(),GSON.toJson(obj(p,"item"))));}return Optional.empty();
 }
 public static Price effectivePrice(ServerPlayerEntity player,String shopId,String productId,Direction direction,int quantity){
  var app=runtime();var shop=app.catalog.get().shops().get(shopId);if(shop==null)throw new IllegalArgumentException("Loja não encontrada");
  for(var entry:arr(shop,"products")){var p=entry.getAsJsonObject();if(str(p,"id","").equals(productId)){
   app.trades.validateAccess(player,shop,p,direction==Direction.SELL);if(quantity<1||quantity>app.pricing.limit(p)||!p.has(direction==Direction.SELL?"sell":"buy"))throw new IllegalArgumentException("Produto/quantidade indisponível");
   var q=app.pricing.quote(player,shopId,shop,p,direction==Direction.SELL,quantity);return new Price(q.originalUnit(),q.discountPercent(),q.effectiveUnit(),q.quantity(),q.originalTotal(),q.total(),q.savings());
  }}throw new IllegalArgumentException("Produto não encontrado");
 }
 public static BigDecimal discount(ServerPlayerEntity player,String shopId,String productId){return effectivePrice(player,shopId,productId,Direction.BUY,1).discountPercent();}
 public static AutoCloseable beforePurchase(Predicate<Trade> listener){registrationThread();return com.zorykai.menus.shops.TradeEvents.listenBefore(Direction.BUY,listener);}
 public static AutoCloseable beforeSale(Predicate<Trade> listener){registrationThread();return com.zorykai.menus.shops.TradeEvents.listenBefore(Direction.SELL,listener);}
 public static AutoCloseable afterPurchase(Consumer<Trade> listener){registrationThread();return com.zorykai.menus.shops.TradeEvents.listenAfter(Direction.BUY,listener);}
 public static AutoCloseable afterSale(Consumer<Trade> listener){registrationThread();return com.zorykai.menus.shops.TradeEvents.listenAfter(Direction.SELL,listener);}
}
