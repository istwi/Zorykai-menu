package com.zorykai.menus.shops;
import com.google.gson.*;
import com.zorykai.menus.*;
import com.zorykai.menus.gui.Menus;
import com.zorykai.menus.permissions.Permissions;
import com.zorykai.menus.placeholders.Texts;
import com.zorykai.menus.util.ItemsJson;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;
import java.math.*;
import static com.zorykai.menus.config.Json.*;
public final class Trades {
 private final ZorykaiMenus app;private final Journal journal;private final Set<UUID> running=new HashSet<>();
 public Trades(ZorykaiMenus a,Journal j){app=a;journal=j;}
 public int pending()throws Exception{return journal.count();}
 public void submit(ServerPlayerEntity p,String shopId,String productId,boolean sell,int quantity,long revision){
  submit(p,shopId,productId,sell,quantity,revision,null);
 }
 public void submit(ServerPlayerEntity p,String shopId,String productId,boolean sell,int quantity,long revision,java.math.BigDecimal expectedTotal){
  UUID id=p.getUuid();boolean ownsLock=false;try{
   if(running.contains(id)||journal.get(id)!=null){app.message(p,"pending",Map.of());return;}
   var snapshot=app.catalog.get();if(snapshot.revision()!=revision)throw new IllegalArgumentException("configuração alterada; abra novamente");
   var shop=snapshot.shops().get(shopId);if(shop==null)throw new IllegalArgumentException("loja indisponível");
   JsonObject product=null;for(var e:arr(shop,"products"))if(str(e.getAsJsonObject(),"id","").equals(productId)){product=e.getAsJsonObject();break;}
   if(product==null||!product.has(sell?"sell":"buy"))throw new IllegalArgumentException("produto indisponível");
   validateAccess(p,shop,product,sell);
   if(quantity<1||quantity>app.pricing.limit(product))throw new IllegalArgumentException("quantidade inválida");
   var unit=ItemsJson.read(app.server,obj(product,"item"));var plan=sell?InventoryPlan.remove(p.getInventory().main,unit,quantity):InventoryPlan.add(p.getInventory().main,unit,quantity);
   if(plan==null)throw new IllegalArgumentException(sell?"itens elegíveis insuficientes":"inventário cheio");
   var quote=app.pricing.quote(p,shopId,shop,product,sell,quantity);
   if(expectedTotal!=null&&quote.total().compareTo(expectedTotal)!=0)throw new IllegalArgumentException("preço ou desconto mudou; abra a confirmação novamente");
   var tx=new JsonObject();tx.addProperty("id",UUID.randomUUID().toString());tx.addProperty("player",id.toString());tx.addProperty("sell",sell);tx.addProperty("quantity",quantity);
   tx.addProperty("shop",shopId);tx.addProperty("product",productId);tx.addProperty("revision",revision);
   tx.addProperty("currency",str(snapshot.economy(),"currency","impactor:dollars"));tx.addProperty("total",quote.total().toPlainString());tx.addProperty("originalTotal",quote.originalTotal().toPlainString());tx.addProperty("discountPercent",quote.discountPercent().toPlainString());tx.add("item",ItemsJson.write(app.server,unit));
   stage(tx,"PREPARED");running.add(id);ownsLock=true;
   if(!TradeEvents.before(TradeEvents.snapshot(tx))){finish(tx,false,"operação cancelada por integração");return;}
   p.closeHandledScreen();
   if(sell){inventoryIntent(p,tx,plan,"REMOVE_INTENT");stage(tx,"ITEMS_REMOVED");credit(tx);}else debit(tx);
  }catch(Exception e){if(ownsLock)running.remove(id);failure(p,e.getMessage());app.LOG.warn("Transação recusada/preservada para {}: {}",id,e.toString());}
 }
 public void validateAccess(ServerPlayerEntity p,JsonObject shop,JsonObject product,boolean sell){
  var permissions=app.catalog.get().permissions();for(String key:List.of("use","shop",sell?"sell":"buy"))if(!Permissions.has(p,str(permissions,key,"zorykaimenus."+key),true))throw new IllegalArgumentException("sem permissão");
  if(!app.menus.allowed(p,shop,"")||!app.menus.allowed(p,product,"")||!com.zorykai.menus.gui.Conditions.visible(p,product))throw new IllegalArgumentException("sem permissão para o produto");
 }
 private void stage(JsonObject tx,String stage)throws Exception{tx.addProperty("stage",stage);journal.put(tx);}
 private void debit(JsonObject tx)throws Exception{
  stage(tx,"DEBIT_INTENT");UUID id=UUID.fromString(str(tx,"player",""));
  app.economy.change(id,str(tx,"currency",""),tx.get("total").getAsBigDecimal(),false).whenComplete((ok,error)->app.server.execute(()->{
   try{if(error!=null){uncertain(tx,error);return;}if(!ok){finish(tx,false,"saldo insuficiente ou débito recusado");return;}stage(tx,"DEBITED");deliver(tx);}catch(Exception e){uncertain(tx,e);}
  }));
 }
 private void deliver(JsonObject tx)throws Exception{
  UUID id=UUID.fromString(str(tx,"player",""));var p=app.server.getPlayerManager().getPlayer(id);
  boolean allowed=p!=null&&!p.isDisconnected()&&app.catalog.get().revision()==tx.get("revision").getAsLong();
  if(allowed)try{
   var shop=app.catalog.get().shops().get(str(tx,"shop",""));JsonObject product=null;for(var e:arr(shop,"products"))if(str(e.getAsJsonObject(),"id","").equals(str(tx,"product","")))product=e.getAsJsonObject();
   if(product==null)allowed=false;else {validateAccess(p,shop,product,false);var current=app.pricing.quote(p,str(tx,"shop",""),shop,product,false,num(tx,"quantity",1));if(current.total().compareTo(tx.get("total").getAsBigDecimal())!=0)allowed=false;}
  }catch(Exception e){allowed=false;}
  var item=ItemsJson.restore(app.server,tx.get("item"));var plan=allowed?InventoryPlan.add(p.getInventory().main,item,num(tx,"quantity",1)):null;
  if(plan==null){refund(tx);return;}
  inventoryIntent(p,tx,plan,"DELIVER_INTENT");finish(tx,true,"");
 }
 private void refund(JsonObject tx)throws Exception{
  stage(tx,"REFUND_INTENT");UUID id=UUID.fromString(str(tx,"player",""));
  app.economy.change(id,str(tx,"currency",""),tx.get("total").getAsBigDecimal(),true).whenComplete((ok,error)->app.server.execute(()->{
   try{if(error!=null){uncertain(tx,error);return;}if(ok)finish(tx,false,"compra cancelada; valor devolvido");else{stage(tx,"REFUND_PENDING");uncertain(tx,new IllegalStateException("estorno recusado; requer recuperação"));}}catch(Exception e){uncertain(tx,e);}
  }));
 }
 private void credit(JsonObject tx)throws Exception{
  stage(tx,"CREDIT_INTENT");UUID id=UUID.fromString(str(tx,"player",""));
  app.economy.change(id,str(tx,"currency",""),tx.get("total").getAsBigDecimal(),true).whenComplete((ok,error)->app.server.execute(()->{
   try{if(error!=null){uncertain(tx,error);return;}if(ok)finish(tx,true,"");else{stage(tx,"RETURN_PENDING");returnItems(tx);}}catch(Exception e){uncertain(tx,e);}
  }));
 }
 private void returnItems(JsonObject tx)throws Exception{
  UUID id=UUID.fromString(str(tx,"player",""));var p=app.server.getPlayerManager().getPlayer(id);
  var plan=p==null?null:InventoryPlan.add(p.getInventory().main,ItemsJson.restore(app.server,tx.get("item")),num(tx,"quantity",1));
  if(plan==null){running.remove(id);if(p!=null)failure(p,"crédito recusado; libere espaço e use /zmenu recover para receber os itens");return;}
  inventoryIntent(p,tx,plan,"RETURN_INTENT");finish(tx,false,"crédito recusado; itens devolvidos");
 }
 private JsonArray stacks(List<ItemStack> items){var out=new JsonArray();for(var stack:items)out.add(ItemsJson.write(app.server,stack));return out;}
 private List<ItemStack> restore(JsonArray values){var out=new ArrayList<ItemStack>();for(var j:values)out.add(ItemsJson.restore(app.server,j));return out;}
 private void inventoryIntent(ServerPlayerEntity p,JsonObject tx,List<ItemStack> after,String step)throws Exception{
  tx.add("before",stacks(p.getInventory().main));tx.add("after",stacks(after));stage(tx,step);DurableInventory.apply(p,after);
 }
 private void finish(JsonObject tx,boolean success,String reason)throws Exception{
  journal.done(tx);if(success)TradeEvents.confirmed(TradeEvents.snapshot(tx));UUID id=UUID.fromString(str(tx,"player",""));running.remove(id);var p=app.server.getPlayerManager().getPlayer(id);
  app.LOG.info("Operação {} concluída: player={}, venda={}, quantidade={}, total={}, sucesso={}",str(tx,"id",""),id,bool(tx,"sell",false),num(tx,"quantity",0),str(tx,"total",""),success);
  if(p!=null){if(success){app.message(p,"success",Map.of("quantity",str(tx,"quantity",""),"total",str(tx,"total","")));app.menus.sound(p,str(app.catalog.get().messages(),bool(tx,"sell",false)?"sellSound":"buySound","minecraft:entity.experience_orb.pickup"));}else failure(p,reason);}
 }
 private void uncertain(JsonObject tx,Throwable error){UUID id=UUID.fromString(str(tx,"player",""));running.remove(id);app.LOG.error("Operação {} preservada em {}. NÃO repetir débito/crédito sem conferência.",str(tx,"id",""),str(tx,"stage",""),error);var p=app.server.getPlayerManager().getPlayer(id);if(p!=null)app.message(p,"pending",Map.of());}
 private void failure(ServerPlayerEntity p,String reason){app.message(p,"failure",Map.of("reason",reason==null?"falha interna":reason));}
 public String status(UUID player)throws Exception{var tx=journal.get(player);return tx==null?"sem operação pendente":str(tx,"id","")+" · "+str(tx,"stage","");}
 public void recover(ServerPlayerEntity p){UUID id=p.getUuid();try{
  if(!running.add(id)){app.message(p,"pending",Map.of());return;}var tx=journal.get(id);if(tx==null){running.remove(id);app.literal(p,"<gray>Nenhuma operação pendente.");return;}
  switch(str(tx,"stage","")){
   case "PREPARED" -> finish(tx,false,"operação não iniciada, cancelada com segurança");
   case "DEBITED","REFUND_PENDING" -> refund(tx); // known debit: recovery returns money instead of surprising item delivery
   case "ITEMS_REMOVED" -> credit(tx);
   case "RETURN_PENDING" -> returnItems(tx);
   case "REMOVE_INTENT","DELIVER_INTENT","RETURN_INTENT" -> {
    var before=restore(arr(tx,"before"));var after=restore(arr(tx,"after"));
    if(InventoryPlan.same(p.getInventory().main,before)){DurableInventory.apply(p,after);}else if(InventoryPlan.same(p.getInventory().main,after)){DurableInventory.apply(p,after);}else throw new IllegalStateException("inventário alterado; requer conferência manual");
    if(str(tx,"stage","").equals("REMOVE_INTENT")){stage(tx,"ITEMS_REMOVED");credit(tx);}else finish(tx,str(tx,"stage","").equals("DELIVER_INTENT"),"itens devolvidos após recuperação");
   }
   default -> {running.remove(id);app.literal(p,"<yellow>Etapa financeira sem confirmação. Administrador deve conferir no Impactor. Operação: "+str(tx,"id",""));}
  }
 }catch(Exception e){running.remove(id);failure(p,e.getMessage());app.LOG.error("Falha na recuperação de "+id,e);}}
 /** Explicit admin attestation after checking the economy provider; never an automatic retry. */
 public String resolve(UUID player,String txId,boolean applied)throws Exception{
  if(running.contains(player))throw new IllegalStateException("operação em execução");var tx=journal.get(player);if(tx==null||!str(tx,"id","").equals(txId))throw new IllegalArgumentException("operação não encontrada");
  switch(str(tx,"stage","")){
   case "DEBIT_INTENT" -> {if(applied)stage(tx,"DEBITED");else journal.done(tx);}
   case "CREDIT_INTENT" -> {if(applied)journal.done(tx);else stage(tx,"RETURN_PENDING");}
   case "REFUND_INTENT" -> {if(applied)journal.done(tx);else stage(tx,"REFUND_PENDING");}
   default -> throw new IllegalArgumentException("etapa não permite atestado financeiro");
  }
  app.LOG.warn("Atestado administrativo: player={}, operação={}, aplicado={}",player,txId,applied);return "Atestado registrado. Jogador pode usar /zmenu recover.";
 }
}
