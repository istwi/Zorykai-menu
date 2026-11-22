package com.zorykai.menus.commands;
import com.mojang.brigadier.tree.*;
import java.util.*;

/** Replace all three Brigadier child indexes; addChild alone would merge old handlers/subcommands. */
public final class CommandPriority {
 private CommandPriority(){}
 @SuppressWarnings("unchecked")
 public static <S> void replace(RootCommandNode<S> root,String name,LiteralCommandNode<S> replacement){
  if(name.equals("zmenu")||!name.equals(replacement.getName()))throw new IllegalArgumentException("Nome de comando reservado ou divergente");
  var maps=new ArrayList<Map<String,?>>();
  try{
   for(String field:List.of("children","literals","arguments")){var f=CommandNode.class.getDeclaredField(field);f.setAccessible(true);maps.add((Map<String,?>)f.get(root));}
  }catch(ReflectiveOperationException|RuntimeException error){throw new IllegalStateException("Não foi possível assumir o alias "+name+"; comando existente preservado",error);}
  for(var map:maps)map.remove(name);
  root.addChild(replacement);
 }
}
