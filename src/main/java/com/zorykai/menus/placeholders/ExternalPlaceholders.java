package com.zorykai.menus.placeholders;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/** Optional Text Placeholder API adapter; this class is loaded only when the provider mod is installed. */
public final class ExternalPlaceholders {
 private ExternalPlaceholders(){}
 public static Component resolve(String token,ServerPlayerEntity player){
  if(!player.getServer().isOnThread())return null;
  int split=token.indexOf(' ');String name=split<0?token:token.substring(0,split);String argument=split<0?null:token.substring(split+1);
  Identifier id=Identifier.tryParse(name);if(id==null)return null;
  if(id.toString().equals("shirokaidex:progress")&&!Placeholders.getPlaceholders().containsKey(id)){
   Text registered=read(Identifier.of("shirokaidex:registered"),argument,player),total=read(Identifier.of("shirokaidex:total"),argument,player);
   if(registered==null||total==null)return null;
   try{int count=Integer.parseInt(registered.getString()),maximum=Integer.parseInt(total.getString());if(count<0||maximum<0||count>maximum)return null;return Component.text(count+"/"+maximum);}catch(NumberFormatException invalid){return null;}
  }
  Text value=read(id,argument,player);return value==null?null:GsonComponentSerializer.gson().deserialize(Text.Serialization.toJsonString(value,player.getRegistryManager()));
 }
 private static Text read(Identifier id,String argument,ServerPlayerEntity player){
  if(!Placeholders.getPlaceholders().containsKey(id))return null;
  var result=Placeholders.parsePlaceholder(id,argument,PlaceholderContext.of(player));
  return result.isValid()?result.text():null;
 }
}
