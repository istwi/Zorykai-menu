package com.zorykai.menus.shops;
import net.minecraft.item.ItemStack;
import java.util.*;
public final class InventoryPlan {
 public static List<ItemStack> copy(List<ItemStack> in){return new ArrayList<>(in.stream().map(ItemStack::copy).toList());}
 public static int available(List<ItemStack> inventory,ItemStack unit){int n=0;for(var stack:inventory)if(ItemStack.areItemsAndComponentsEqual(stack,unit))n=Math.addExact(n,stack.getCount());return n;}
 public static boolean same(List<ItemStack> a,List<ItemStack> b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++)if(!ItemStack.areEqual(a.get(i),b.get(i)))return false;return true;}
 public static List<ItemStack> add(List<ItemStack> inventory,ItemStack unit,int quantity){
  var result=copy(inventory);int left=quantity;
  for(var stack:result)if(!stack.isEmpty()&&ItemStack.areItemsAndComponentsEqual(stack,unit)){int n=Math.min(left,stack.getMaxCount()-stack.getCount());stack.increment(n);left-=n;}
  for(int i=0;i<result.size()&&left>0;i++)if(result.get(i).isEmpty()){int n=Math.min(left,unit.getMaxCount());result.set(i,unit.copyWithCount(n));left-=n;}
  return left==0?result:null;
 }
 public static List<ItemStack> remove(List<ItemStack> inventory,ItemStack unit,int quantity){
  if(available(inventory,unit)<quantity)return null;var result=copy(inventory);int left=quantity;
  for(int i=0;i<result.size()&&left>0;i++){var stack=result.get(i);if(ItemStack.areItemsAndComponentsEqual(stack,unit)){int n=Math.min(left,stack.getCount());stack.decrement(n);left-=n;if(stack.isEmpty())result.set(i,ItemStack.EMPTY);}}
  return result;
 }
}
