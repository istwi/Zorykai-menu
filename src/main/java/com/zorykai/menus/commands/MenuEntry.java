package com.zorykai.menus.commands;

import com.zorykai.menus.ZorykaiMenus;
import com.zorykai.menus.permissions.Permissions;
import com.google.gson.JsonObject;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import static com.zorykai.menus.config.Json.*;

/** Command entry restrictions do not grant viewer permissions or change internal menu navigation. */
public final class MenuEntry {
 public static final String ADMIN="zorykaimenus.command.open";
 private MenuEntry(){}
 public static boolean administrative(ServerCommandSource s){return Permissions.admin(s,ADMIN);}
 public static boolean console(ServerCommandSource s){return s.getEntity()==null&&s.hasPermissionLevel(4);}
 public static int own(ZorykaiMenus app,ServerCommandSource s,boolean shop,String id){
  if(!administrative(s)){s.sendFeedback(()->net.minecraft.text.Text.literal("Use os aliases de menu liberados pelo servidor ou fale com o NPC."),false);return 0;}
  var p=s.getPlayer();if(p==null){s.sendError(net.minecraft.text.Text.literal("Informe o jogador: zmenu "+(shop?"shop":"open")+" <id> <jogador>"));return 0;}
  app.menus.open(p,shop,id,1,false);return 1;
 }
 public static JsonObject alias(ZorykaiMenus app,String alias){
  var snap=app.catalog.get();var aliases=obj(snap.config(),"aliases");
  if(aliases.has(alias))return aliases.getAsJsonObject(alias);
  for(var e:snap.menus().entrySet())if(strings(e.getValue(),"aliases").contains(alias)){var spec=new JsonObject();spec.addProperty("menu",e.getKey());return spec;}
  return null;
 }
 public static boolean aliasAllowed(ZorykaiMenus app,String name,ServerCommandSource source){
  if(!(source.getEntity() instanceof ServerPlayerEntity p))return false;
  var spec=alias(app,name);if(spec==null)return false;
  boolean shop=spec.has("shop");String id=str(spec,shop?"shop":"menu","");
  return (shop?app.catalog.get().shops():app.catalog.get().menus()).containsKey(id)
   &&Permissions.has(p,str(spec,"permission",""),!spec.has("permission"));
 }
 public static int openAlias(ZorykaiMenus app,String name,ServerCommandSource source){
  if(!aliasAllowed(app,name,source))return 0;
  var spec=alias(app,name);boolean shop=spec.has("shop");
  app.menus.open(source.getPlayer(),shop,str(spec,shop?"shop":"menu",""),1,false);return 1;
 }
}
