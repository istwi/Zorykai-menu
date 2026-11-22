package com.zorykai.menus.config;
import com.google.gson.*;
import java.util.*;
import static com.zorykai.menus.config.Json.*;

/** A main menu is optional. Explicit alias priority never enables generic opening commands. */
public final class MenuConfiguration {
 private MenuConfiguration(){}
 public static String main(JsonObject config,Map<String,JsonObject> menus){
  if(!config.has("mainMenu")||config.get("mainMenu").isJsonNull())return null;
  String id=config.get("mainMenu").getAsString();return id.isBlank()||!menus.containsKey(id)?null:id;
 }
 public static void validate(JsonObject config){
  var seen=new HashSet<String>();
  for(var entry:arr(config,"aliasOverrides")){String name=entry.getAsString();if(!name.matches("[a-z0-9_-]+")||name.equals("zmenu")||!seen.add(name))throw new IllegalArgumentException("aliasOverrides: nome inválido, reservado ou duplicado: "+name);}
  for(var entry:obj(config,"aliases").entrySet()){
   if(!entry.getKey().matches("[a-z0-9_-]+")||!entry.getValue().isJsonObject())throw new IllegalArgumentException("aliases: nome/objeto inválido: "+entry.getKey());
   var spec=entry.getValue().getAsJsonObject();
   if(spec.has("override")&&(!spec.get("override").isJsonPrimitive()||!spec.getAsJsonPrimitive("override").isBoolean()))throw new IllegalArgumentException("aliases."+entry.getKey()+".override: use true/false");
   if(entry.getKey().equals("zmenu")&&bool(spec,"override",false))throw new IllegalArgumentException("O comando zmenu é reservado");
  }
 }
 public static boolean override(JsonObject config,String alias,JsonObject spec){return !alias.equals("zmenu")&&(bool(spec,"override",false)||strings(config,"aliasOverrides").contains(alias));}
}
