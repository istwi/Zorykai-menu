package com.zorykai.menus.shops;

import java.io.IOException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DurableInventoryTest {
 private static NbtList inventory(int... slots) {
  var result = new NbtList();
  for (int slot : slots) {
   var entry = new NbtCompound();
   entry.putByte("Slot", (byte) slot);
   entry.putString("id", "minecraft:stone");
   result.add(entry);
  }
  return result;
 }

 @Test void comparesMainInventoryBySlotAndIgnoresEquipment() throws IOException {
  assertEquals(DurableInventory.mainSlots(inventory(0, 10, 103)),
      DurableInventory.mainSlots(inventory(150, 10, 0, 103)));
  assertNotEquals(DurableInventory.mainSlots(inventory(0, 10)),
      DurableInventory.mainSlots(inventory(0, 11)));
  assertThrows(IOException.class, () -> DurableInventory.mainSlots(inventory(0, 0)));
 }
}
