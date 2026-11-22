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

/** Own-party, one-at-a-time sale with durable Pokemon escrow before payment. */
public final class PokemonSales {
 private final ZorykaiMenus app;private final Journal journal;public final PokemonStorage storage=new PokemonStorage();
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
 public void open(ServerPlayerEntity p){try{
  access(p);if(journal.get(p.getUuid())!=null){notice(p,"Há uma operação pendente. Use /zmenu recover.");return;}
  var mons=storage.partyList(p);LockedMenu menu=app.menus.display(p,4,"<dark_green>Vender Pokémon",null);
  int[] slots={10,11,12,14,15,16};
  for(var mon:mons){
   var lore=new ArrayList<String>();lore.add("<gray>Espécie: "+safe(mon.species()));lore.add("<gray>Nível: "+mon.level()+" · Shiny: "+(mon.shiny()?"Sim":"Não"));
   boolean allowed=true;
   try{storage.eligible(p,mon.uuid(),num(cfg(),"keepParty",1));var q=quote(p,mon);lore.add("<green>Venda: "+q.total().toPlainString());lore.add("<gray>"+safe(q.explanation()));lore.add("<yellow>Clique para revisar a venda");}
   catch(Exception e){allowed=false;lore.add("<red>"+safe(e.getMessage()));}
   final boolean enabled=allowed;
   app.menus.plain(p,menu,slots[mon.slot()],enabled?Items.LIME_DYE:Items.GRAY_DYE,"<white>"+safe(mon.name()),lore,enabled?button->confirm(p,mon.uuid()):null,null);
  }
  app.menus.plain(p,menu,31,Items.BARRIER,"<red>Fechar",List.of("<gray>Nenhum Pokémon é vendido ao fechar."),b->p.closeHandledScreen(),null);
 }catch(Exception e){notice(p,e.getMessage());}}
 public void confirm(ServerPlayerEntity p,UUID uuid){try{
  access(p);var mon=storage.eligible(p,uuid,num(cfg(),"keepParty",1));var q=quote(p,mon);long revision=app.catalog.get().revision();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(num(cfg(),"confirmationSeconds",60));
  LockedMenu menu=app.menus.display(p,3,"<dark_green>Confirmar venda do Pokémon",null);
  app.menus.plain(p,menu,13,Items.PAPER,"<white>"+safe(mon.name()),List.of("<gray>Espécie: "+safe(mon.species()),"<gray>Nível: "+mon.level()+" · Shiny: "+(mon.shiny()?"Sim":"Não"),"<green>Você receberá: "+q.total().toPlainString(),"<gray>"+safe(q.explanation()),"<red>O Pokémon será entregue ao servidor.","<gray>A venda não pode ser desfeita pelo jogador."),null,null);
  boolean[] consumed={false};
  app.menus.plain(p,menu,11,Items.LIME_CONCRETE,"<green>Confirmar venda",List.of("<gray>Receber "+q.total().toPlainString()),b->{if(consumed[0]||p.currentScreenHandler!=menu)return;consumed[0]=true;p.closeHandledScreen();if(System.nanoTime()>deadline){notice(p,"Confirmação expirada; abra novamente.");return;}submit(p,mon,q.total(),revision);},null);
  app.menus.plain(p,menu,15,Items.RED_CONCRETE,"<red>Cancelar",List.of(),b->open(p),null);
 }catch(Exception e){notice(p,e.getMessage());}}
 public void submit(ServerPlayerEntity p,PokemonStorage.Mon offer,BigDecimal expected,long revision){
  JsonObject tx=null;try{
   access(p);if(!running.add(p.getUuid())){notice(p,"Aguarde a operação atual");return;}
   if(journal.get(p.getUuid())!=null)throw new IllegalStateException("Operação pendente; use /zmenu recover");
   checkOffer(p,offer,expected,revision);
   var mon=storage.eligible(p,offer.uuid(),num(cfg(),"keepParty",1));
   tx=new JsonObject();tx.addProperty("kind","pokemon_sale");tx.addProperty("id",UUID.randomUUID().toString());tx.addProperty("player",p.getUuidAsString());tx.addProperty("pokemon",mon.uuid().toString());tx.addProperty("snapshot",mon.snapshot());tx.addProperty("fingerprint",mon.fingerprint());tx.addProperty("currency",str(app.catalog.get().economy(),"currency","impactor:dollars"));tx.addProperty("total",expected.toPlainString());tx.add("beforeIds",partyIds(p));stage(tx,"POKEMON_PREPARED");
   final JsonObject sale=tx;
   // Read-only economy preflight. Pending is already journaled, excluding simultaneous item trades.
   app.economy.balance(p.getUuid()).whenComplete((balance,error)->app.server.execute(()->{
    try{
     if(error!=null||app.server.getPlayerManager().getPlayer(p.getUuid())!=p){finish(sale,false,"Venda cancelada antes da retirada do Pokémon");return;}
     checkOffer(p,offer,expected,revision);stage(sale,"POKEMON_REMOVE_INTENT");storage.remove(p,offer.uuid());sale.add("afterIds",partyIds(p));stage(sale,"POKEMON_REMOVE_INTENT");
     // Wait only for bounded local disk I/O, not network calls. This prevents PC/party transfers
     // from racing the native serialized party file while the actual removal is being persisted.
     persist(p);stage(sale,"POKEMON_REMOVED");credit(sale);
    }catch(Exception failure){uncertain(sale,failure);}
   }));
  }catch(Exception e){if(tx!=null)uncertain(tx,e);else{running.remove(p.getUuid());notice(p,e.getMessage());}}
 }
 private void checkOffer(ServerPlayerEntity p,PokemonStorage.Mon offer,BigDecimal expected,long revision)throws Exception{
  access(p);if(app.catalog.get().revision()!=revision)throw new IllegalStateException("Configuração alterada; revise a venda novamente");
  var current=storage.eligible(p,offer.uuid(),num(cfg(),"keepParty",1));
  if(!current.fingerprint().equals(offer.fingerprint())||quote(p,current).total().compareTo(expected)!=0)throw new IllegalStateException("Pokémon ou preço mudou; revise a venda novamente");
 }
 private JsonArray partyIds(ServerPlayerEntity p)throws Exception{var list=new JsonArray();for(var mon:storage.partyList(p))list.add(mon.slot()+":"+mon.uuid());return list;}
 private void persist(ServerPlayerEntity p)throws Exception{storage.durable(p).get(5,TimeUnit.SECONDS);}
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
  if(!storage.anywhere(p,uuid))storage.restore(p,uuid,str(tx,"snapshot",""));
  persist(p);finish(tx,false,"Pagamento recusado; Pokémon devolvido.");
 }
 private void finish(JsonObject tx,boolean sold,String message)throws Exception{
  journal.finishPokemon(tx,sold?"sold":"cancelled");UUID player=UUID.fromString(str(tx,"player",""));running.remove(player);app.LOG.info("Venda Pokémon {}: player={}, pokemon={}, total={}, sold={}",str(tx,"id",""),player,str(tx,"pokemon",""),str(tx,"total",""),sold);
  var p=app.server.getPlayerManager().getPlayer(player);if(p!=null)notice(p,message);
 }
 private void uncertain(JsonObject tx,Throwable error){UUID id=UUID.fromString(str(tx,"player",""));running.remove(id);app.LOG.error("Venda Pokémon {} preservada em {}. Conferir antes de repetir pagamento.",str(tx,"id",""),str(tx,"stage",""),error);var p=app.server.getPlayerManager().getPlayer(id);if(p!=null)notice(p,"Operação preservada. Use /zmenu recover; se houver pagamento sem confirmação, a staff precisa conferir no Impactor.");}
 private void notice(ServerPlayerEntity p,String text){app.literal(p,"<gray>"+safe(text==null?"Operação indisponível":text));}
 public boolean owns(JsonObject tx){return tx!=null&&str(tx,"kind","").equals("pokemon_sale");}
 public void recover(ServerPlayerEntity p){try{
  if(!running.add(p.getUuid())){notice(p,"Operação em andamento");return;}var tx=journal.get(p.getUuid());if(!owns(tx)){running.remove(p.getUuid());return;}
  switch(str(tx,"stage","")){
   case "POKEMON_PREPARED"->finish(tx,false,"Venda não iniciada; Pokémon mantido.");
   case "POKEMON_REMOVE_INTENT"->{
    UUID uuid=UUID.fromString(str(tx,"pokemon",""));boolean inParty=storage.partyList(p).stream().anyMatch(m->m.uuid().equals(uuid));
    if(inParty){if(!partyIds(p).equals(arr(tx,"beforeIds")))throw new IllegalStateException("Equipe alterada; revisão necessária");storage.remove(p,uuid);}
    else if(storage.anywhere(p,uuid)||!tx.has("afterIds")||!partyIds(p).equals(arr(tx,"afterIds")))throw new IllegalStateException("Localização do Pokémon não comprovada; revisão necessária");
    persist(p);stage(tx,"POKEMON_REMOVED");credit(tx);
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
