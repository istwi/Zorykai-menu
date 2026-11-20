package com.zorykai.menus.shops;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.WorldSavePath;
import java.util.*;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
public final class DurableInventory {
 public static void apply(ServerPlayerEntity p,List<ItemStack> plan)throws Exception{
  for(int i=0;i<36;i++)p.getInventory().main.set(i,plan.get(i).copy());p.getInventory().markDirty();p.currentScreenHandler.sendContentUpdates();
  var expected=p.getInventory().writeNbt(new NbtList());
  p.getServer().getPlayerManager().savePlayerData(p);
  var file=p.getServer().getSavePath(WorldSavePath.PLAYERDATA).resolve(p.getUuidAsString()+".dat");
  var saved=NbtIo.readCompressed(file,NbtSizeTracker.ofUnlimitedBytes());
  if(!expected.equals(saved.getList("Inventory",10)))throw new java.io.IOException("Inventário salvo diverge; journal preservado");
  try(var channel=FileChannel.open(file,StandardOpenOption.WRITE)){channel.force(true);}
 }
}
