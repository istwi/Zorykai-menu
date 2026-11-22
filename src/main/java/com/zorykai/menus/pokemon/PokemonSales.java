package com.zorykai.menus.pokemon;

import com.google.gson.*;
import com.zorykai.menus.ZorykaiMenus;
import com.zorykai.menus.gui.LockedMenu;
import com.zorykai.menus.permissions.Permissions;
import com.zorykai.menus.shops.Journal;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static com.zorykai.menus.config.Json.*;

/** Own-PC, one-at-a-time sale with durable Pokemon escrow before payment. */
public final class PokemonSales {
 private final ZorykaiMenus app;private final Journal journal;public final PokemonStorage storage=new PokemonStorage();
 private record ReturnMenu(ServerPlayerEntity player,int page){}
 private final Map<String,ReturnMenu> returnMenus=new HashMap<>();
 private final PokemonPrices prices;private final Set<UUID> running=new HashSet<>();private Map<String,String> live=Map.of();private long liveRevision=-1;
 public PokemonSales(ZorykaiMenus app,Journal journal)throws Exception{
  this.app=app;this.journal=journal;
  try(var stream=PokemonSales.class.getResourceAsStream("/defaults/pokemon/species-profiles.json")){if(stream==null)throw new IllegalStateException("Referência de espécies ausente");prices=new PokemonPrices(parse(new String(stream.readAllBytes(),StandardCharsets.UTF_8)));}
 }
 private JsonObject cfg(){return obj(app.catalog.get().config(),"pokemonSales");}
 private void access(ServerPlayerEntity p){
  if(!bool(cfg(),"enabled",false))throw new IllegalStateException("Venda de Pokémon desativada");
  if(!Permissions.has(p,str(cfg(),"permission","zorykaimenus.pokemon.sell"),true))throw new IllegalStateException("Sem permissão para vender Pokémon");
  if(!storage.available())throw new IllegalStateException("Cobblemon não está instalado");
 }
 public PokemonPrices.Quote quote(ServerPlayerEntity p,PokemonStorage.Mon mon)throws Exception{
  access(p);if(liveRevision!=app.catalog.get().revision()){
   try{live=storage.liveBuckets();}catch(Exception e){live=Map.of();app.LOG.warn("Rotas ativas indisponíveis; usando referência conservadora de espécies",e);}
   liveRevision=app.catalog.get().revision();
  }
  return prices.quote(cfg(),mon.species(),mon.shiny(),app.economy.decimals(),live);
 }
 private static String safe(String text){return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().escapeTags(text);}
 public void open(ServerPlayerEntity p){open(p,0);}
 private void open(ServerPlayerEntity p,int requested){try{
  access(p);if(journal.get(p.getUuid())!=null){notice(p,"Há uma operação pendente. Use /zmenu recover.");return;}
  var mons=storage.pcList(p);int pages=Math.max(1,(mons.size()+44)/45),page=Math.max(0,Math.min(requested,pages-1));
  LockedMenu menu=app.menus.display(p,6,"<dark_green>Vender Pokémon do PC · "+(page+1)+"/"+pages,null);
  for(int i=page*45;i<Math.min(mons.size(),(page+1)*45);i++){
   var mon=mons.get(i);var lore=new ArrayList<String>();lore.add("<gray>Espécie: "+safe(mon.species()));lore.add("<gray>Nível: "+mon.level()+" · Shiny: "+(mon.shiny()?"Sim":"Não"));boolean allowed=true;
   try{storage.pcEligible(p,mon.uuid());var q=quote(p,mon);lore.add("<green>Venda: "+q.total().toPlainString());lore.add("<gray>"+safe(q.explanation()));if(prices.dailyLimited(mon.species(),live)){int remaining=remaining(p);lore.add("<gray>Vendas raras restantes hoje: "+remaining);if(remaining<=0)throw new IllegalStateException("Limite diário de vendas raras atingido");}lore.add("<yellow>Clique para revisar a venda");}
   catch(Exception e){allowed=false;lore.add("<red>"+safe(e.getMessage()));}
   var icon=storage.model(p,mon.uuid());decorate(p,icon,"<white>"+safe(mon.name()),lore);boolean enabled=allowed;
   menu.button(i-page*45,icon,enabled?button->confirm(p,mon.uuid(),page):null);
  }
  if(page>0)app.menus.plain(p,menu,45,Items.ARROW,"<yellow>Anterior",List.of(),b->open(p,page-1),null);
  if(page+1<pages)app.menus.plain(p,menu,53,Items.ARROW,"<yellow>Próxima",List.of(),b->open(p,page+1),null);
  app.menus.plain(p,menu,49,Items.BARRIER,"<red>Fechar",List.of("<gray>Nenhum Pokémon é vendido ao fechar."),b->p.closeHandledScreen(),null);
 }catch(Exception e){notice(p,e.getMessage());}}
 private void decorate(ServerPlayerEntity p,net.minecraft.item.ItemStack icon,String name,List<String> lore){icon.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,com.zorykai.menus.placeholders.Texts.text(name,p,null));icon.set(net.minecraft.component.DataComponentTypes.LORE,new net.minecraft.component.type.LoreComponent(lore.stream().map(v->com.zorykai.menus.placeholders.Texts.text(v,p,null)).toList()));}
 private int remaining(ServerPlayerEntity p)throws Exception{var zone=java.time.ZoneId.of(str(cfg(),"dailyTimezone","America/Sao_Paulo"));var day=java.time.LocalDate.now(zone);return Math.max(0,num(cfg(),"rareDailyLimit",10)-journal.pokemonSoldBetween(p.getUuid(),day.atStartOfDay(zone).toInstant().toEpochMilli(),day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()));}
 private void quota(ServerPlayerEntity p,PokemonStorage.Mon mon)throws Exception{if(prices.dailyLimited(mon.species(),live)&&remaining(p)<=0)throw new IllegalStateException("Limite diário de vendas raras atingido");}
 public void confirm(ServerPlayerEntity p,UUID uuid){confirm(p,uuid,0);}
 private void confirm(ServerPlayerEntity p,UUID uuid,int page){try{
  access(p);var mon=storage.pcEligible(p,uuid);var q=quote(p,mon);quota(p,mon);long revision=app.catalog.get().revision();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(num(cfg(),"confirmationSeconds",60));
  LockedMenu menu=app.menus.display(p,3,"<dark_green>Confirmar venda do Pokémon",null);
  var model=storage.model(p,uuid);
  app.menus.plain(p,menu,13,Items.PAPER,"<white>"+safe(mon.name()),List.of("<gray>Espécie: "+safe(mon.species()),"<gray>Nível: "+mon.level()+" · Shiny: "+(mon.shiny()?"Sim":"Não"),"<green>Você receberá: "+q.total().toPlainString(),"<gray>"+safe(q.explanation()),"<red>O Pokémon será entregue ao servidor.","<gray>A venda não pode ser desfeita pelo jogador."),null,null);
  model.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,menu.getSlot(13).getStack().get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME));model.set(net.minecraft.component.DataComponentTypes.LORE,menu.getSlot(13).getStack().get(net.minecraft.component.DataComponentTypes.LORE));menu.button(13,model,null);
  boolean[] consumed={false};
  app.menus.plain(p,menu,11,Items.LIME_CONCRETE,"<green>Confirmar venda",List.of("<gray>Receber "+q.total().toPlainString()),b->{if(consumed[0]||p.currentScreenHandler!=menu)return;consumed[0]=true;p.closeHandledScreen();if(System.nanoTime()>deadline){notice(p,"Confirmação expirada; abra novamente.");return;}submit(p,mon,q.total(),revision,page);},null);
  app.menus.plain(p,menu,15,Items.RED_CONCRETE,"<red>Cancelar",List.of(),b->open(p,page),null);
 }catch(Exception e){notice(p,e.getMessage());}}
 public void submit(ServerPlayerEntity p,PokemonStorage.Mon offer,BigDecimal expected,long revision){submit(p,offer,expected,revision,-1);}
 private void submit(ServerPlayerEntity p,PokemonStorage.Mon offer,BigDecimal expected,long revision,int returnPage){
  JsonObject tx=null;try{
   access(p);if(!running.add(p.getUuid())){notice(p,"Aguarde a operação atual");return;}
   if(journal.get(p.getUuid())!=null)throw new IllegalStateException("Operação pendente; use /zmenu recover");
   checkOffer(p,offer,expected,revision);
   var mon=storage.pcEligible(p,offer.uuid());
   tx=new JsonObject();tx.addProperty("source","pc");tx.addProperty("dailyLimited",prices.dailyLimited(mon.species(),live));tx.addProperty("speciesBucket",prices.speciesBucket(mon.species(),live));tx.addProperty("kind","pokemon_sale");tx.addProperty("id",UUID.randomUUID().toString());tx.addProperty("player",p.getUuidAsString());tx.addProperty("pokemon",mon.uuid().toString());tx.addProperty("snapshot",mon.snapshot());tx.addProperty("fingerprint",mon.fingerprint());tx.addProperty("currency",str(app.catalog.get().economy(),"currency","impactor:dollars"));tx.addProperty("total",expected.toPlainString());tx.add("beforeIds",partyIds(p));stage(tx,"POKEMON_PREPARED");
   if(returnPage>=0)returnMenus.put(str(tx,"id",""),new ReturnMenu(p,returnPage));
   final JsonObject sale=tx;
   // Read-only economy preflight. Pending is already journaled, excluding simultaneous item trades.
   app.economy.balance(p.getUuid()).whenComplete((balance,error)->app.server.execute(()->{
    try{
     if(error!=null||app.server.getPlayerManager().getPlayer(p.getUuid())!=p){finish(sale,false,"Venda cancelada antes da retirada do Pokémon");return;}
     checkOffer(p,offer,expected,revision);stage(sale,"POKEMON_REMOVE_INTENT");storage.removePc(p,offer.uuid());sale.add("afterIds",partyIds(p));stage(sale,"POKEMON_REMOVE_INTENT");
     // Wait only for bounded local disk I/O, not network calls. This prevents PC/party transfers
     // from racing the native serialized party file while the actual removal is being persisted.
     persist(p,sale);stage(sale,"POKEMON_REMOVED");credit(sale);
    }catch(Exception failure){uncertain(sale,failure);}
   }));
  }catch(Exception e){if(tx!=null)uncertain(tx,e);else{running.remove(p.getUuid());notice(p,e.getMessage());}}
 }
 private void checkOffer(ServerPlayerEntity p,PokemonStorage.Mon offer,BigDecimal expected,long revision)throws Exception{
  access(p);if(app.catalog.get().revision()!=revision)throw new IllegalStateException("Configuração alterada; revise a venda novamente");
  var current=storage.pcEligible(p,offer.uuid());
  quota(p,current);
  if(!current.fingerprint().equals(offer.fingerprint())||quote(p,current).total().compareTo(expected)!=0)throw new IllegalStateException("Pokémon ou preço mudou; revise a venda novamente");
 }
 private JsonArray partyIds(ServerPlayerEntity p)throws Exception{var list=new JsonArray();for(var mon:storage.pcList(p))list.add(mon.slot()+":"+mon.uuid());return list;}
 private JsonArray recoveryIds(ServerPlayerEntity p,JsonObject tx)throws Exception{if(str(tx,"source","").equals("pc"))return partyIds(p);var ids=new JsonArray();for(var mon:storage.partyList(p))ids.add(mon.slot()+":"+mon.uuid());return ids;}
 private void persist(ServerPlayerEntity p,JsonObject tx)throws Exception{if(str(tx,"source","").equals("pc"))storage.durablePc(p).get(5,TimeUnit.SECONDS);else storage.durable(p).get(5,TimeUnit.SECONDS);}
 private void stage(JsonObject tx,String stage)throws Exception{tx.addProperty("stage",stage);journal.put(tx);}
 private void credit(JsonObject tx)throws Exception{
  stage(tx,"POKEMON_CREDIT_INTENT");UUID player=UUID.fromString(str(tx,"player",""));
  app.economy.change(player,str(tx,"currency",""),tx.get("total").getAsBigDecimal(),true).whenComplete((ok,error)->app.server.execute(()->{
   try{if(error!=null){uncertain(tx,error);return;}if(Boolean.TRUE.equals(ok))finish(tx,true,"Venda concluída: "+str(tx,"total","")+" creditados.");else{stage(tx,"POKEMON_RETURN_PENDING");returnPokemon(tx);}}
   catch(Exception e){uncertain(tx,e);}
  }));
 }
 private void returnPokemon(JsonObject tx)throws Exception{
  UUID player=UUID.fromString(str(tx,"player","")),uuid=UUID.fromString(str(tx,"pokemon",""));var p=app.server.getPlayerManager().getPlayer(player);
  if(p==null){running.remove(player);return;}
  if(!storage.anywhere(p,uuid)){if(str(tx,"source","").equals("pc"))storage.restorePc(p,uuid,str(tx,"snapshot",""));else storage.restore(p,uuid,str(tx,"snapshot",""));}
  persist(p,tx);finish(tx,false,"Pagamento recusado; Pokémon devolvido.");
 }
 private void finish(JsonObject tx,boolean sold,String message)throws Exception{
  journal.finishPokemon(tx,sold?"sold":"cancelled");UUID player=UUID.fromString(str(tx,"player",""));running.remove(player);app.LOG.info("Venda Pokémon {}: player={}, pokemon={}, total={}, sold={}",str(tx,"id",""),player,str(tx,"pokemon",""),str(tx,"total",""),sold);
  var back=returnMenus.remove(str(tx,"id",""));var p=app.server.getPlayerManager().getPlayer(player);if(p!=null)notice(p,message);
  if(sold&&back!=null&&p==back.player()&&p.currentScreenHandler==p.playerScreenHandler)open(p,back.page());
 }
 private void uncertain(JsonObject tx,Throwable error){returnMenus.remove(str(tx,"id",""));UUID id=UUID.fromString(str(tx,"player",""));running.remove(id);app.LOG.error("Venda Pokémon {} preservada em {}. Conferir antes de repetir pagamento.",str(tx,"id",""),str(tx,"stage",""),error);var p=app.server.getPlayerManager().getPlayer(id);if(p!=null)notice(p,"Operação preservada. Use /zmenu recover; se houver pagamento sem confirmação, a staff precisa conferir no Impactor.");}
 private void notice(ServerPlayerEntity p,String text){app.literal(p,"<gray>"+safe(text==null?"Operação indisponível":text));}
 public boolean owns(JsonObject tx){return tx!=null&&str(tx,"kind","").equals("pokemon_sale");}
 public void recover(ServerPlayerEntity p){try{
  if(!running.add(p.getUuid())){notice(p,"Operação em andamento");return;}var tx=journal.get(p.getUuid());if(!owns(tx)){running.remove(p.getUuid());return;}
  switch(str(tx,"stage","")){
   case "POKEMON_PREPARED"->finish(tx,false,"Venda não iniciada; Pokémon mantido.");
   case "POKEMON_REMOVE_INTENT"->{
    UUID uuid=UUID.fromString(str(tx,"pokemon",""));boolean fromPc=str(tx,"source","").equals("pc");boolean inParty=(fromPc?storage.pcList(p):storage.partyList(p)).stream().anyMatch(m->m.uuid().equals(uuid));
    if(inParty){if(!recoveryIds(p,tx).equals(arr(tx,"beforeIds")))throw new IllegalStateException("Equipe alterada; revisão necessária");if(fromPc)storage.removePc(p,uuid);else storage.remove(p,uuid);}
    else if(storage.anywhere(p,uuid)||!tx.has("afterIds")||!recoveryIds(p,tx).equals(arr(tx,"afterIds")))throw new IllegalStateException("Localização do Pokémon não comprovada; revisão necessária");
    persist(p,tx);stage(tx,"POKEMON_REMOVED");credit(tx);
   }
   case "POKEMON_REMOVED"->{if(storage.anywhere(p,UUID.fromString(str(tx,"pokemon",""))))throw new IllegalStateException("Pokémon reapareceu no armazenamento; pagamento não repetido");credit(tx);}
   case "POKEMON_RETURN_PENDING"->returnPokemon(tx);
   default->{running.remove(p.getUuid());notice(p,"Pagamento sem confirmação. Staff deve conferir no Impactor e usar /zmenu resolve com o ID "+str(tx,"id",""));}
  }
 }catch(Exception e){running.remove(p.getUuid());notice(p,e.getMessage());app.LOG.error("Recuperação de venda Pokémon recusada",e);}}
 public String resolve(UUID player,String id,boolean applied)throws Exception{
  if(running.contains(player))throw new IllegalStateException("Operação em andamento");var tx=journal.get(player);
  if(!owns(tx)||!str(tx,"id","").equals(id)||!str(tx,"stage","").equals("POKEMON_CREDIT_INTENT"))throw new IllegalArgumentException("Atestado exige uma etapa de pagamento Pokémon sem confirmação");
  if(applied)finish(tx,true,"Pagamento confirmado pela staff.");else stage(tx,"POKEMON_RETURN_PENDING");
  app.LOG.warn("Atestado venda Pokémon: player={}, tx={}, applied={}",player,id,applied);return "Atestado registrado; o jogador pode usar /zmenu recover.";
 }
}
