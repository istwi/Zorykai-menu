package com.zorykai.menus.gui;
import com.google.gson.*;
import com.zorykai.menus.*;
import com.zorykai.menus.config.Catalog;
import com.zorykai.menus.util.ItemsJson;
import com.zorykai.menus.permissions.Permissions;
import com.zorykai.menus.placeholders.Texts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.item.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.sound.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.IntConsumer;
import static com.zorykai.menus.config.Json.*;
public final class Menus {
 public record Location(boolean shop,String id,int page){}
 private final ZorykaiMenus app;private final Map<UUID,Deque<Location>> history=new HashMap<>();private final Map<UUID,Location> current=new HashMap<>();
 public Menus(ZorykaiMenus a){app=a;}
 public void forget(UUID id){history.remove(id);current.remove(id);}
 private boolean access(ServerPlayerEntity p,Location location){
  var snap=app.catalog.get();var data=(location.shop?snap.shops():snap.menus()).get(location.id);
  return data!=null&&Permissions.has(p,str(snap.permissions(),"use","zorykaimenus.use"),true)
   &&Permissions.has(p,str(snap.permissions(),location.shop?"shop":"open","zorykaimenus."+(location.shop?"shop":"open")),true)&&allowed(p,data,"");
 }
 public boolean allowed(ServerPlayerEntity p,JsonObject j,String fallback){return bool(j,"available",true)&&Permissions.has(p,str(j,"permission",fallback),!j.has("permission"))&&Conditions.visible(p,j);}
 public void open(ServerPlayerEntity p,boolean shop,String id,int page,boolean push){
  var snap=app.catalog.get();var data=(shop?snap.shops():snap.menus()).get(id);
  if(data==null){app.message(p,"missing",Map.of());return;}
  if(!Permissions.has(p,str(snap.permissions(),"use","zorykaimenus.use"),true)||!Permissions.has(p,str(snap.permissions(),shop?"shop":"open",shop?"zorykaimenus.shop":"zorykaimenus.open"),true)||!allowed(p,data,"")){app.message(p,"denied",Map.of());return;}
  int pages=num(data,"pages",1);if(page<1||page>pages)return;
  if(push&&current.containsKey(p.getUuid())){var h=history.computeIfAbsent(p.getUuid(),k->new ArrayDeque<>());if(h.size()>=32)h.removeLast();h.push(current.get(p.getUuid()));}
  Location loc=new Location(shop,id,page);current.put(p.getUuid(),loc);
  LockedMenu menu=display(p,num(data,"size",54)/9,str(data,"title",id),null);
  BigDecimal[] balanceSnapshot={null};
  render(p,menu,data,loc,null,snap.revision());
  menu.refreshAction=()->{if(app.catalog.get().revision()==snap.revision()&&access(p,loc))render(p,menu,data,loc,balanceSnapshot[0],snap.revision());};
  app.economy.balance(p.getUuid()).whenComplete((balance,error)->p.getServer().execute(()->{
   if(p.currentScreenHandler==menu&&app.catalog.get().revision()==snap.revision()){balanceSnapshot[0]=error==null?balance:null;render(p,menu,data,loc,balanceSnapshot[0],snap.revision());}
  }));
  execute(p,arr(data,"openActions"),0,loc,snap.revision());
  menu.closeAction=()->execute(p,arr(data,"closeActions"),0,loc,snap.revision());
 }
 private void render(ServerPlayerEntity p,LockedMenu menu,JsonObject data,Location loc,BigDecimal balance,long revision){
  menu.reset();
  for(String kind:List.of("buttons","products"))for(var el:arr(data,kind).asList().stream().sorted(java.util.Comparator.comparingInt((JsonElement e)->num(e.getAsJsonObject(),"priority",0)).reversed()).toList()){
   var b=el.getAsJsonObject();if(!Catalog.pages(b,num(data,"pages",1)).contains(loc.page)||!allowed(p,b,"")||!Conditions.visible(p,b))continue;
   var lore=new ArrayList<>(strings(b,"lore"));if(kind.equals("products")){
    if(b.has("buy")&&!bool(b,"priceLoreProvided",false))lore.add("<aqua>Comprar: <white>$"+b.get("buy").getAsBigDecimal().toPlainString()+" <gray>por unidade · botão esquerdo");
    boolean dynamic=b.has("sell")&&com.zorykai.menus.market.MarketPolicy.of(data,b).isPresent();
    if(dynamic){
     var quote=app.pricing.quote(p,loc.id,data,b,true,1);var rule=com.zorykai.menus.market.MarketPolicy.of(data,b).orElseThrow();
     String percent=quote.originalUnit().signum()==0?"100":quote.total().multiply(new BigDecimal("100")).divide(quote.originalUnit(),2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
     lore.replaceAll(line->line.replace("{market_sell}",Texts.money(quote.total())).replace("{market_base}",Texts.money(quote.originalUnit())).replace("{market_percent}",percent));
     lore.addAll(List.of("<aqua><bold>Bolsa de valores</bold>","<gray>Preço base: <white>$"+Texts.money(quote.originalUnit()),"<green>Vender agora (1 un.): <white>$"+Texts.money(quote.total())+" <gray>· botão direito","<gray>Cotação: "+percent+"% da base · mínimo "+Texts.money(rule.minimumPercent())+"%","<gray>Mais vendas reduzem o preço.","<gray>Sem novas vendas, recupera aos poucos."));
    }else if(b.has("sell")&&!bool(b,"priceLoreProvided",false))lore.add("<green>Vender: <white>$"+b.get("sell").getAsBigDecimal().toPlainString()+" <gray>por unidade · botão direito");
   }
   if(kind.equals("products")&&b.has("buy")){
    var quote=app.pricing.quote(p,loc.id,data,b,false,1);
    if(quote.discountPercent().signum()>0)lore.addAll(List.of("<gray>Original: $"+Texts.money(quote.originalUnit()),"<aqua>Benefício VIP: "+Texts.money(quote.discountPercent())+"%","<green>Preço VIP: $"+Texts.money(quote.effectiveUnit()),"<gray>Economia/unidade: $"+Texts.money(quote.savings())));
   }
   var stack=icon(p,obj(b,"icon"),b.has("name")?b.get("name").getAsString():null,lore,balance);
   for(var sl:arr(b,"slots"))menu.button(sl.getAsInt(),stack.copy(),button->{
    if(app.catalog.get().revision()!=revision){p.closeHandledScreen();app.literal(p,"<yellow>A configuração mudou. Abra o menu novamente.");return;}
    if(!access(p,loc)||!allowed(p,b,"")||!Conditions.visible(p,b)){app.message(p,"denied",Map.of());return;}
    if(kind.equals("products")){boolean sell=button==1;if(b.has(sell?"sell":"buy"))confirm(p,loc,b,sell,1,revision);}
    else execute(p,arr(b,"actions"),button,loc,revision);
   });
  }
  menu.sendContentUpdates();
 }
 public LockedMenu display(ServerPlayerEntity p,int rows,String title,BigDecimal balance){
  LockedMenu[] holder=new LockedMenu[1];p.openHandledScreen(new SimpleNamedScreenHandlerFactory((sync,inv,player)->holder[0]=new LockedMenu(sync,inv,p,rows),Texts.text(title,p,balance)));return holder[0];
 }
 public ItemStack icon(ServerPlayerEntity p,JsonObject spec,String name,List<String> lore,BigDecimal balance){
  var rendered=ItemsJson.readIcon(p.getServer(),spec);ItemStack item=rendered.stack();
  if(rendered.fallback()){lore=new ArrayList<>(lore);lore.add("<gray>Ícone original indisponível: "+str(spec,"id",""));}
  if(bool(spec,"viewerHead",false))item.set(DataComponentTypes.PROFILE,new ProfileComponent(p.getGameProfile()));
  if(name!=null)item.set(DataComponentTypes.CUSTOM_NAME,Texts.text(name,p,balance));
  if(!lore.isEmpty())item.set(DataComponentTypes.LORE,new LoreComponent(lore.stream().map(v->Texts.text(v,p,balance)).toList()));
  return item;
 }
 public void plain(ServerPlayerEntity p,LockedMenu menu,int slot,Item item,String name,List<String> lore,IntConsumer callback,BigDecimal balance){
  var spec=new JsonObject();spec.addProperty("id",Registries.ITEM.getId(item).toString());menu.button(slot,icon(p,spec,name,lore,balance),callback);
 }
 public void execute(ServerPlayerEntity p,JsonArray actions,int button,Location location,long revision){
  if(app.catalog.get().revision()!=revision)return;
  if(!access(p,location)){if(!actions.isEmpty())app.message(p,"denied",Map.of());return;}
  for(var element:actions){var a=element.getAsJsonObject();String click=str(a,"click","ANY");if(click.equals("LEFT")&&button!=0||click.equals("RIGHT")&&button!=1)continue;
   String type=str(a,"type","");boolean privileged=type.equals("SERVER_COMMAND");
   if(!Permissions.has(p,str(a,"permission",privileged?"zorykaimenus.actions.server":""),!privileged&&!a.has("permission"))){app.message(p,"denied",Map.of());return;}
   switch(type){
    case "OPEN_MENU","OPEN_SHOP" -> open(p,type.equals("OPEN_SHOP"),str(a,"id",""),1,true);
    case "OPEN_POKEMON_SELL" -> app.pokemonSales.open(p);
    case "CLOSE_MENU" -> p.closeHandledScreen();
    case "BACK" -> back(p);
    case "NEXT_PAGE" -> open(p,location.shop,location.id,location.page+1,false);
    case "PREVIOUS_PAGE" -> open(p,location.shop,location.id,location.page-1,false);
    case "SEND_MESSAGE" -> app.literal(p,str(a,"message",""));
    case "PLAY_SOUND" -> sound(p,str(a,"sound","minecraft:ui.button.click"));
    default -> {try{com.zorykai.menus.api.ZorykaiMenusAPI.executeAction(type,new com.zorykai.menus.api.ZorykaiMenusAPI.ActionContext(p,location.id,location.shop,location.page,button,GSON.toJson(a)));}catch(RuntimeException|LinkageError failure){app.LOG.warn("Ação de extensão recusada: "+type,failure);app.literal(p,"<red>Ação indisponível ou sem permissão.");}}
    case "PLAYER_COMMAND","SERVER_COMMAND" -> {
     if(privileged&&!bool(app.catalog.get().config(),"allowServerCommands",false)){app.literal(p,"<red>Ações de console desativadas na configuração.");return;}
     for(String command:strings(a,"commands")){
      String cmd=Texts.replace(command,p,null).replaceFirst("^/","");
      if(cmd.indexOf('\n')>=0||cmd.indexOf('\r')>=0||cmd.indexOf(0)>=0)return;
      p.getServer().getCommandManager().executeWithPrefix(privileged?p.getServer().getCommandSource():p.getCommandSource(),cmd);
     }
    }
   }
  }
 }
 public void back(ServerPlayerEntity p){var h=history.get(p.getUuid());if(h==null||h.isEmpty()){open(p,false,str(app.catalog.get().config(),"mainMenu","main"),1,false);return;}Location l=h.pop();open(p,l.shop,l.id,l.page,false);}
 public void sound(ServerPlayerEntity p,String id){Identifier key=Identifier.tryParse(id);if(key==null||!Registries.SOUND_EVENT.containsId(key))return;p.playSoundToPlayer(Registries.SOUND_EVENT.get(key),SoundCategory.MASTER,0.5f,1f);}
 public void confirm(ServerPlayerEntity p,Location parent,JsonObject product,boolean sell,int quantity,long revision){
  if(app.catalog.get().revision()!=revision)return;
  if(!Permissions.has(p,str(app.catalog.get().permissions(),sell?"sell":"buy","zorykaimenus."+(sell?"sell":"buy")),true)||!allowed(p,product,"")){app.message(p,"denied",Map.of());return;}
  int q=Math.max(1,Math.min(app.pricing.limit(product),quantity));
  var frozen=app.pricing.quote(p,parent.id,app.catalog.get().shops().get(parent.id),product,sell,q);
  LockedMenu m=display(p,6,sell?"<aqua>Confirmar venda":"<aqua>Confirmar compra",null);
  renderConfirm(p,m,parent,product,sell,q,revision,null,frozen);
  app.economy.balance(p.getUuid()).whenComplete((balance,error)->p.getServer().execute(()->{if(p.currentScreenHandler==m&&app.catalog.get().revision()==revision)renderConfirm(p,m,parent,product,sell,q,revision,error==null?balance:null,frozen);}));
 }
 private void renderConfirm(ServerPlayerEntity p,LockedMenu m,Location parent,JsonObject product,boolean sell,int q,long revision,BigDecimal balance,com.zorykai.menus.pricing.PriceEngine.Quote frozen){
  var shop=app.catalog.get().shops().get(parent.id);
  var quote=frozen;
  boolean market=sell&&com.zorykai.menus.market.MarketPolicy.of(shop,product).isPresent();
  BigDecimal unit=quote.effectiveUnit(),total=quote.total();
  var item=ItemsJson.read(p.getServer(),obj(product,"item"));int available=com.zorykai.menus.shops.InventoryPlan.available(p.getInventory().main,item);
  List<String> lore=new ArrayList<>(List.of("<gray>Quantidade: <white>"+q,(market?"<gray>Preço médio por unidade: <white>$":"<gray>Preço unitário: <white>$")+Texts.money(unit),"<aqua>Total: <white>$"+Texts.money(total),"<gray>Saldo atual: <white>$"+Texts.money(balance)));
  if(market)lore.addAll(List.of("<aqua>Bolsa de valores · cotação global nesta loja","<gray>O total inclui a queda causada por este lote.","<gray>Se a cotação mudar, reabra a confirmação."));
  if(!sell)lore.addAll(List.of("<gray>Preço original/unidade: $"+Texts.money(quote.originalUnit()),"<aqua>Desconto: "+Texts.money(quote.discountPercent())+"%","<green>Economia total: $"+Texts.money(quote.savings())));
  lore.add(sell?"<gray>Disponíveis: <white>"+available:"<gray>Saldo estimado: <white>$"+Texts.money(balance==null?null:balance.subtract(total)));
  m.button(13,icon(p,obj(product,"icon"),product.has("name")?product.get("name").getAsString():null,lore,balance),null);
  int[] deltas={-64,-10,-1,1,10,64};int[] slots={19,20,21,23,24,25};
  for(int i=0;i<6;i++){int delta=deltas[i];plain(p,m,slots[i],delta<0?Items.RED_DYE:Items.LIME_DYE,"<white>"+(delta>0?"+":"")+delta,List.of("<gray>Alterar quantidade"),b->confirm(p,parent,product,sell,q+delta,revision),balance);}
  int[] quick={1,16,32,64};for(int i=0;i<4;i++){int n=quick[i];plain(p,m,29+i,Items.PAPER,"<aqua>"+n+" unidade(s)",List.of(),b->confirm(p,parent,product,sell,n,revision),balance);}
  if(sell)plain(p,m,34,Items.CHEST,"<aqua>Vender todos os elegíveis",List.of("<gray>Somente este produto, com componentes idênticos.","<gray>Disponíveis: "+available),b->confirm(p,parent,product,true,available,revision),balance);
  if(!sell)plain(p,m,34,Items.CHEST,"<aqua><bold>COMPRAR MÁXIMO",List.of("<gray>Considera saldo, desconto e espaço.","<gray>A compra ainda exige confirmação."),b->selectMaximum(p,m,parent,product,revision),balance);
  plain(p,m,45,Items.ARROW,"<aqua>Voltar",List.of(),b->open(p,true,parent.id,parent.page,false),balance);
  plain(p,m,49,Items.LIME_CONCRETE,"<green>Confirmar "+(sell?"venda":"compra"),List.of("<gray>"+q+" unidade(s) · $"+Texts.money(total)),b->app.trades.submit(p,parent.id,str(product,"id",""),sell,q,revision,quote.total()),balance);
  plain(p,m,53,Items.RED_CONCRETE,"<red>Cancelar",List.of(),b->p.closeHandledScreen(),balance);m.sendContentUpdates();
 }
 private void selectMaximum(ServerPlayerEntity p,LockedMenu screen,Location parent,JsonObject product,long revision){
  app.economy.balance(p.getUuid()).whenComplete((balance,error)->p.getServer().execute(()->{
   if(p.currentScreenHandler!=screen||app.catalog.get().revision()!=revision)return;
   try{
    if(error!=null)throw new IllegalStateException("saldo indisponível");
    if(!access(p,parent)||!allowed(p,product,"")||!Conditions.visible(p,product)||!Permissions.has(p,str(app.catalog.get().permissions(),"buy","zorykaimenus.buy"),true))throw new IllegalArgumentException("sem permissão ou condição indisponível");
    var shop=app.catalog.get().shops().get(parent.id);var unit=ItemsJson.read(p.getServer(),obj(product,"item"));
    int limit=app.pricing.limit(product);int capacity=com.zorykai.menus.shops.InventoryPlan.capacity(p.getInventory().main,unit,limit);
    if(capacity==0)throw new IllegalArgumentException("inventário cheio");
    int max=com.zorykai.menus.pricing.PriceEngine.maximum(balance,capacity,limit,n->app.pricing.quote(p,parent.id,shop,product,false,n).total());
    if(max==0)throw new IllegalArgumentException("saldo insuficiente para uma unidade");
    confirm(p,parent,product,false,max,revision);
   }catch(Exception failure){app.message(p,"failure",Map.of("reason",failure.getMessage()==null?"cálculo indisponível":failure.getMessage()));}
  }));
 }
}
