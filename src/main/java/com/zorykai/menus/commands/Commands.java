package com.zorykai.menus.commands;
import com.zorykai.menus.*;
import com.zorykai.menus.permissions.Permissions;
import com.zorykai.menus.migration.Migrator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.*;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import java.util.*;
import static net.minecraft.server.command.CommandManager.*;
import static com.zorykai.menus.config.Json.*;
public final class Commands {
 public static void register(ZorykaiMenus a,CommandDispatcher<ServerCommandSource> d){
  d.register(literal("zmenu").executes(c->{a.menus.open(c.getSource().getPlayerOrThrow(),false,str(a.catalog.get().config(),"mainMenu","main"),1,false);return 1;})
   .then(literal("open").then(argument("menu",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(a.catalog.get().menus().keySet(),b)).executes(c->{a.menus.open(c.getSource().getPlayerOrThrow(),false,StringArgumentType.getString(c,"menu"),1,false);return 1;})))
   .then(literal("shop").then(argument("shop",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(a.catalog.get().shops().keySet(),b)).executes(c->{a.menus.open(c.getSource().getPlayerOrThrow(),true,StringArgumentType.getString(c,"shop"),1,false);return 1;})))
   .then(literal("pokemon").executes(c->{feedback(c.getSource(),"Fale com o NPC de venda de Pokémon.");return 0;})
    .then(literal("sell").executes(c->{feedback(c.getSource(),"Fale com o NPC de venda de Pokémon.");return 0;}))
    .then(literal("open").requires(s->s.getEntity()==null&&s.hasPermissionLevel(4))
     .then(argument("player",net.minecraft.command.argument.EntityArgumentType.player()).executes(c->{a.pokemonSales.open(net.minecraft.command.argument.EntityArgumentType.getPlayer(c,"player"));return 1;}))))
   .then(literal("reload").requires(s->Permissions.admin(s,"zorykaimenus.reload")).executes(c->{var errors=a.catalog.reload(a.server,true);feedback(c.getSource(),errors.isEmpty()?"Configuração recarregada. Alterações em aliases exigem reinício.":"Configuração anterior preservada:\n"+String.join("\n",errors));return errors.isEmpty()?1:0;}))
   .then(literal("validate").requires(s->Permissions.admin(s,"zorykaimenus.validate")).executes(c->{var errors=a.catalog.reload(a.server,false);feedback(c.getSource(),errors.isEmpty()?"Configurações válidas.":String.join("\n",errors));return errors.isEmpty()?1:0;}))
   .then(literal("migrate").requires(s->Permissions.admin(s,"zorykaimenus.migrate")).executes(c->{try{feedback(c.getSource(),new Migrator(a).run());return 1;}catch(Exception e){feedback(c.getSource(),"Migração não aplicada: "+e.getMessage());ZorykaiMenus.LOG.error("Falha na migração",e);return 0;}}))
   .then(literal("status").executes(c->{try{String msg="ZorykaiMenus 1.3.0: "+a.catalog.get().menus().size()+" menus, "+a.catalog.get().shops().size()+" lojas";if(c.getSource().getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity p)msg+=" · "+a.trades.status(p.getUuid());if(Permissions.admin(c.getSource(),"zorykaimenus.status"))msg+=" · pendências globais: "+a.trades.pending();feedback(c.getSource(),msg);return 1;}catch(Exception e){feedback(c.getSource(),e.getMessage());return 0;}}))
   .then(literal("discount").requires(source->Permissions.admin(source,"zorykaimenus.discount.admin"))
    .then(literal("reload").executes(c->{var errors=a.catalog.reload(a.server,true);feedback(c.getSource(),errors.isEmpty()?"Descontos/configurações recarregados.":String.join("\n",errors));return errors.isEmpty()?1:0;}))
    .then(literal("list").executes(c->{feedback(c.getSource(),GSON.toJson(a.catalog.get().discounts()));return 1;}))
    .then(literal("check").then(argument("player",net.minecraft.command.argument.EntityArgumentType.player()).executes(c->{var player=net.minecraft.command.argument.EntityArgumentType.getPlayer(c,"player");var global=com.zorykai.menus.pricing.Discounts.applicable(a.catalog.get().discounts(),"",new com.google.gson.JsonObject(),new com.google.gson.JsonObject(),node->Permissions.explicit(player,node));feedback(c.getSource(),player.getGameProfile().getName()+": desconto geral "+global.toPlainString()+"%; regras específicas podem substituir por loja/categoria/produto.");return 1;}))))
   .then(literal("market").requires(source->Permissions.admin(source,"zorykaimenus.market.admin"))
    .then(literal("status").then(argument("shop",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(a.catalog.get().shops().keySet(),b)).then(argument("product",StringArgumentType.word()).executes(c->market(a,c,false)))))
    .then(literal("reset").then(argument("shop",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(a.catalog.get().shops().keySet(),b)).then(argument("product",StringArgumentType.word()).executes(c->market(a,c,true))))))
   .then(literal("recover").executes(c->{try{a.recover(c.getSource().getPlayerOrThrow());return 1;}catch(Exception e){feedback(c.getSource(),e.getMessage());return 0;}}))
   .then(literal("resolve").requires(s->Permissions.admin(s,"zorykaimenus.resolve")).then(argument("uuid",StringArgumentType.word()).then(argument("transaction",StringArgumentType.word()).then(argument("result",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(List.of("applied","not_applied"),b)).executes(c->{try{String result=StringArgumentType.getString(c,"result");if(!Set.of("applied","not_applied").contains(result))throw new IllegalArgumentException("Use applied ou not_applied após conferir o Impactor");feedback(c.getSource(),a.resolve(UUID.fromString(StringArgumentType.getString(c,"uuid")),StringArgumentType.getString(c,"transaction"),result.equals("applied")));return 1;}catch(Exception e){feedback(c.getSource(),e.getMessage());return 0;}}))))));
 }
 public static void aliases(ZorykaiMenus a,CommandDispatcher<ServerCommandSource> d){
  var aliases=obj(a.catalog.get().config(),"aliases").deepCopy();
  for(var entry:a.catalog.get().menus().entrySet())for(String alias:strings(entry.getValue(),"aliases"))if(!aliases.has(alias)){var value=new com.google.gson.JsonObject();value.addProperty("menu",entry.getKey());aliases.add(alias,value);}
  for(var entry:aliases.entrySet()){
   String alias=entry.getKey();if(!alias.matches("[a-z0-9_-]+")||d.getRoot().getChild(alias)!=null){ZorykaiMenus.LOG.warn("Alias {} não registrado: inválido ou conflito",alias);continue;}
   var spec=entry.getValue().getAsJsonObject();boolean shop=spec.has("shop");String id=str(spec,shop?"shop":"menu","");
   if(!(shop?a.catalog.get().shops():a.catalog.get().menus()).containsKey(id)){ZorykaiMenus.LOG.warn("Alias {} aponta para destino ausente {}",alias,id);continue;}
   d.register(literal(alias).executes(c->{a.menus.open(c.getSource().getPlayerOrThrow(),shop,id,1,false);return 1;}));
  }
  a.server.getPlayerManager().getPlayerList().forEach(a.server.getCommandManager()::sendCommandTree);
 }
 private static int market(ZorykaiMenus app,com.mojang.brigadier.context.CommandContext<ServerCommandSource> c,boolean reset){
  try{String shopId=StringArgumentType.getString(c,"shop"),productId=StringArgumentType.getString(c,"product");var shop=app.catalog.get().shops().get(shopId);if(shop==null)throw new IllegalArgumentException("Loja não encontrada");
   var product=arr(shop,"products").asList().stream().map(com.google.gson.JsonElement::getAsJsonObject).filter(p->str(p,"id","").equals(productId)).findFirst().orElseThrow(()->new IllegalArgumentException("Produto não encontrado"));
   var rule=com.zorykai.menus.market.MarketPolicy.of(shop,product).orElseThrow(()->new IllegalArgumentException("Bolsa não ativada neste produto"));
   if(reset){app.market.reset(shopId,productId);ZorykaiMenus.LOG.info("Mercado resetado: shop={}, product={}, operador={}",shopId,productId,c.getSource().getName());feedback(c.getSource(),"Cotação restaurada ao preço base.");}else feedback(c.getSource(),app.market.status(shopId,productId,rule));return 1;
  }catch(Exception failure){ZorykaiMenus.LOG.warn("Comando de mercado recusado: {}",failure.toString());feedback(c.getSource(),failure.getMessage());return 0;}
 }
 private static void feedback(ServerCommandSource s,String message){s.sendFeedback(()->Text.literal(message),false);}
}
