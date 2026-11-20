package com.zorykai.menus.util;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.MinecraftServer;
import static com.zorykai.menus.config.Json.*;
public final class ItemsJson {
 public static ItemStack read(MinecraftServer server,JsonObject spec){
  JsonObject nativeJson=new JsonObject();nativeJson.addProperty("id",str(spec,"id","minecraft:stone"));nativeJson.addProperty("count",num(spec,"count",1));
  if(spec.has("components"))nativeJson.add("components",spec.get("components"));
  var result=ItemStack.CODEC.parse(RegistryOps.of(JsonOps.INSTANCE,server.getRegistryManager()),nativeJson).getOrThrow();
  if(result.isEmpty()||result.getCount()>result.getMaxCount())throw new IllegalArgumentException("Item vazio ou contagem inválida: "+nativeJson);
  return result;
 }
 public record Icon(ItemStack stack,boolean fallback){}
 public static Icon readIcon(MinecraftServer server,JsonObject spec){
  try{return new Icon(read(server,spec),false);}catch(RuntimeException invalid){
   if(!spec.has("fallback"))throw invalid;
   return new Icon(read(server,obj(spec,"fallback")),true);
  }
 }
 public static JsonElement write(MinecraftServer server,ItemStack stack){
  if(stack.isEmpty())return JsonNull.INSTANCE;
  return ItemStack.CODEC.encodeStart(RegistryOps.of(JsonOps.INSTANCE,server.getRegistryManager()),stack).getOrThrow();
 }
 public static ItemStack restore(MinecraftServer s,JsonElement j){return j.isJsonNull()?ItemStack.EMPTY:ItemStack.CODEC.parse(RegistryOps.of(JsonOps.INSTANCE,s.getRegistryManager()),j).getOrThrow();}
}
