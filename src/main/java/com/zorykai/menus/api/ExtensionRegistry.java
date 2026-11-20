package com.zorykai.menus.api;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
/** Exact ownership: closing an old registration cannot remove a replacement. */
public final class ExtensionRegistry<T> {
 private final Map<String,T> entries=new ConcurrentHashMap<>();
 public AutoCloseable register(String id,T handler){
  if(id==null||!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("ID de extensão inválido: "+id);
  java.util.Objects.requireNonNull(handler,"handler");if(entries.putIfAbsent(id,handler)!=null)throw new IllegalArgumentException("Extensão já registrada: "+id);
  var closed=new java.util.concurrent.atomic.AtomicBoolean();return ()->{if(closed.compareAndSet(false,true))entries.remove(id,handler);};
 }
 public T get(String id){return entries.get(id);}
 public boolean contains(String id){return entries.containsKey(id);}
}
