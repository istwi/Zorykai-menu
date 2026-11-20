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
   .then(literal("reload").requires(s->Permissions.admin(s,"zorykaimenus.reload")).executes(c->{var errors=a.catalog.reload(a.server,true);feedback(c.getSource(),errors.isEmpty()?"Configuração recarregada. Alterações em aliases exigem reinício.":"Configuração anterior preservada:\n"+String.join("\n",errors));return errors.isEmpty()?1:0;}))
   .then(literal("validate").requires(s->Permissions.admin(s,"zorykaimenus.validate")).executes(c->{var errors=a.catalog.reload(a.server,false);feedback(c.getSource(),errors.isEmpty()?"Configurações válidas.":String.join("\n",errors));return errors.isEmpty()?1:0;}))
   .then(literal("migrate").requires(s->Permissions.admin(s,"zorykaimenus.migrate")).executes(c->{try{feedback(c.getSource(),new Migrator(a).run());return 1;}catch(Exception e){feedback(c.getSource(),"Migração não aplicada: "+e.getMessage());ZorykaiMenus.LOG.error("Falha na migração",e);return 0;}}))
   .then(literal("status").executes(c->{try{String msg="ZorykaiMenus 1.0.0: "+a.catalog.get().menus().size()+" menus, "+a.catalog.get().shops().size()+" lojas";if(c.getSource().getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity p)msg+=" · "+a.trades.status(p.getUuid());if(Permissions.admin(c.getSource(),"zorykaimenus.status"))msg+=" · pendências globais: "+a.trades.pending();feedback(c.getSource(),msg);return 1;}catch(Exception e){feedback(c.getSource(),e.getMessage());return 0;}}))
   .then(literal("recover").executes(c->{a.trades.recover(c.getSource().getPlayerOrThrow());return 1;}))
   .then(literal("resolve").requires(s->Permissions.admin(s,"zorykaimenus.resolve")).then(argument("uuid",StringArgumentType.word()).then(argument("transaction",StringArgumentType.word()).then(argument("result",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(List.of("applied","not_applied"),b)).executes(c->{try{String result=StringArgumentType.getString(c,"result");if(!Set.of("applied","not_applied").contains(result))throw new IllegalArgumentException("Use applied ou not_applied após conferir o Impactor");feedback(c.getSource(),a.trades.resolve(UUID.fromString(StringArgumentType.getString(c,"uuid")),StringArgumentType.getString(c,"transaction"),result.equals("applied")));return 1;}catch(Exception e){feedback(c.getSource(),e.getMessage());return 0;}}))))));
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
 private static void feedback(ServerCommandSource s,String message){s.sendFeedback(()->Text.literal(message),false);}
}
