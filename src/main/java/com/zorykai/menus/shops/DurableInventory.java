package com.zorykai.menus.shops;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.WorldSavePath;
import java.util.*;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
public final class DurableInventory {
 private static final int MAIN_SLOTS = 36;

 public static void apply(ServerPlayerEntity p,List<ItemStack> plan)throws Exception{
  for(int i=0;i<MAIN_SLOTS;i++)p.getInventory().main.set(i,plan.get(i).copy());
  p.getInventory().markDirty();
  confirmCurrent(p);
  p.currentScreenHandler.sendContentUpdates();
 }

 static void confirmCurrent(ServerPlayerEntity p)throws IOException{
  var expected=mainSlots(p.getInventory().writeNbt(new NbtList()));
  p.getServer().getPlayerManager().savePlayerData(p);
  var file=p.getServer().getSavePath(WorldSavePath.PLAYERDATA).resolve(p.getUuidAsString()+".dat");
  var saved=NbtIo.readCompressed(file,NbtSizeTracker.ofUnlimitedBytes());
  if(saved==null || !(saved.get("Inventory") instanceof NbtList inventory))
   throw new IOException("Arquivo do jogador sem lista Inventory");
  var actual=mainSlots(inventory);
  if(!expected.equals(actual)){
   var differences=new ArrayList<String>();
   for(int slot=0;slot<MAIN_SLOTS;slot++){
    var memory=expected.get(slot);var disk=actual.get(slot);
    if(!Objects.equals(memory,disk))
     differences.add(slot+": memória="+describe(memory)+", disco="+describe(disk));
   }
   throw new IOException("Slots salvos divergentes (uma mesma ID/quantidade indica divergência nos dados do item): "+differences);
  }
  try(var channel=FileChannel.open(file,StandardOpenOption.WRITE)){channel.force(true);}
 }

 private static String describe(NbtCompound entry){
  return entry==null?"vazio":entry.getString("id")+" x"+entry.getInt("count");
 }

 static Map<Integer,NbtCompound> mainSlots(NbtList inventory)throws IOException{
  var slots=new HashMap<Integer,NbtCompound>();
  for(var tag:inventory){
   if(!(tag instanceof NbtCompound entry)||!entry.contains("Slot",NbtElement.BYTE_TYPE))
    throw new IOException("Registro de inventário inválido; journal preservado");
   int slot=Byte.toUnsignedInt(entry.getByte("Slot"));
   if(slot<MAIN_SLOTS && slots.putIfAbsent(slot,entry)!=null)
    throw new IOException("Slot duplicado na mochila: "+slot);
  }
  return slots;
 }
}
