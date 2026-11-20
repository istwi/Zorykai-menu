package com.zorykai.menus.gui;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.entity.player.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import java.util.*;
import java.util.function.IntConsumer;
/** Every client inventory mutation is ignored; only server-owned button callbacks execute. */
public final class LockedMenu extends GenericContainerScreenHandler {
 private final ServerPlayerEntity owner;private final int size;private long lastClick=-1;
 private final Map<Integer,IntConsumer> buttons=new HashMap<>();public Runnable closeAction;public Runnable refreshAction;private long lastRefresh=-1;
 private static final ScreenHandlerType<?>[] TYPES={ScreenHandlerType.GENERIC_9X1,ScreenHandlerType.GENERIC_9X2,ScreenHandlerType.GENERIC_9X3,ScreenHandlerType.GENERIC_9X4,ScreenHandlerType.GENERIC_9X5,ScreenHandlerType.GENERIC_9X6};
 public LockedMenu(int sync,PlayerInventory inv,ServerPlayerEntity p,int rows){super(TYPES[rows-1],sync,inv,new SimpleInventory(rows*9),rows);owner=p;size=rows*9;}
 public void reset(){buttons.clear();getInventory().clear();}
 public void button(int slot,ItemStack stack,IntConsumer callback){getInventory().setStack(slot,stack);if(callback!=null)buttons.put(slot,callback);}
 @Override public void sendContentUpdates(){
  long tick=owner.getServer().getTicks();
  if(refreshAction!=null&&owner.currentScreenHandler==this&&tick-lastRefresh>=20){lastRefresh=tick;refreshAction.run();}
  super.sendContentUpdates();
 }
 @Override public boolean canUse(PlayerEntity p){return p==owner;}
 @Override public ItemStack quickMove(PlayerEntity p,int slot){return ItemStack.EMPTY;}
 @Override public void onSlotClick(int slot,int button,SlotActionType type,PlayerEntity p){
  if(p!=owner||p.currentScreenHandler!=this||slot<0||slot>=size||button<0||button>1||type!=SlotActionType.PICKUP)return;
  long now=owner.getServer().getTicks();if(lastClick==now)return;lastClick=now;
  var handler=buttons.get(slot);if(handler!=null)handler.accept(button);
 }
 @Override public void onClosed(PlayerEntity p){super.onClosed(p);if(closeAction!=null){var action=closeAction;closeAction=null;action.run();}}
}
