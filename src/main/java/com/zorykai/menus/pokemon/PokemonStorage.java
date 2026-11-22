package com.zorykai.menus.pokemon;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.nbt.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.io.File;
import static com.zorykai.menus.config.Json.*;

/** Optional Cobblemon 1.8.3 adapter. No Cobblemon classes are linked without that mod. */
public final class PokemonStorage {
 public record Mon(int slot,UUID uuid,String species,String name,String form,boolean shiny,int level,String fingerprint,String snapshot){}
 private static Object call(Object target,String name,Object...args)throws Exception{
  Class<?> type=target instanceof Class<?> c?c:target.getClass();
  for(var m:type.getMethods()){
   if(!m.getName().equals(name)||m.getParameterCount()!=args.length)continue;
   boolean ok=true;Class<?>[] params=m.getParameterTypes();
   for(int i=0;i<args.length;i++)if(args[i]!=null&&!wrap(params[i]).isInstance(args[i])){ok=false;break;}
   if(ok)try{return m.invoke(Modifier.isStatic(m.getModifiers())?null:target,args);}catch(InvocationTargetException e){throw e.getCause() instanceof Exception x?x:e;}
  }
  throw new NoSuchMethodException(type.getName()+"."+name);
 }
 private static Class<?> wrap(Class<?> c){if(c==int.class)return Integer.class;if(c==boolean.class)return Boolean.class;return c;}
 private static Object hidden(Object o,String field)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(field);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(field);}
 public boolean available(){return FabricLoader.getInstance().isModLoaded("cobblemon");}
 private Object manager()throws Exception{if(!available())throw new IllegalStateException("Cobblemon não está instalado");var c=Class.forName("com.cobblemon.mod.common.Cobblemon");return call(c.getField("INSTANCE").get(null),"getStorage");}
 private Object party(ServerPlayerEntity p)throws Exception{return call(manager(),"getParty",p);}
 private List<Object> stores(ServerPlayerEntity p)throws Exception{return List.of(party(p),call(manager(),"getPC",p));}
 public List<Mon> partyList(ServerPlayerEntity p)throws Exception{
  var store=party(p);List<Mon> out=new ArrayList<>();for(int i=0;i<6;i++){Object mon=call(store,"get",i);if(mon!=null)out.add(describe(p,mon,i));}return out;
 }
 private Mon describe(ServerPlayerEntity p,Object mon,int slot)throws Exception{
  UUID uuid=(UUID)call(mon,"getUuid");Object species=call(mon,"getSpecies");String id=call(species,"getResourceIdentifier").toString();
  String form=String.valueOf(call(call(mon,"getForm"),"getName"));boolean shiny=(Boolean)call(mon,"getShiny");int level=(Integer)call(mon,"getLevel");
  String snapshot=call(mon,"saveToJSON",p.getServer().getRegistryManager(),new JsonObject()).toString();
  String fingerprint=uuid+"|"+id+"|"+form+"|"+shiny+"|"+level;
  return new Mon(slot,uuid,id,((net.minecraft.text.Text)call(mon,"getDisplayName",false)).getString(),form,shiny,level,fingerprint,snapshot);
 }
 private Object findInParty(ServerPlayerEntity p,UUID uuid)throws Exception{for(Object mon:(Iterable<?>)party(p))if(call(mon,"getUuid").equals(uuid))return mon;return null;}
 public boolean anywhere(ServerPlayerEntity p,UUID uuid)throws Exception{for(Object s:stores(p))for(Object mon:(Iterable<?>)s)if(call(mon,"getUuid").equals(uuid))return true;return false;}
 public Mon eligible(ServerPlayerEntity p,UUID uuid,int keepParty)throws Exception{
  if(!p.isAlive()||p.isSpectator())throw new IllegalStateException("Você não pode vender nesse estado");
  Class<?> reg=Class.forName("com.cobblemon.mod.common.battles.BattleRegistry");Object registry;
  try{registry=reg.getField("INSTANCE").get(null);}catch(NoSuchFieldException e){registry=reg;}
  if(call(registry,"getBattleByParticipatingPlayer",p)!=null)throw new IllegalStateException("Termine a batalha antes de vender");
  var list=partyList(p);if(list.size()<=keepParty)throw new IllegalStateException("Mantenha pelo menos "+keepParty+" Pokémon na equipe");
  Object mon=findInParty(p,uuid);if(mon==null)throw new IllegalStateException("O Pokémon não está mais na sua equipe");
  if(!p.getUuid().equals(call(mon,"getOwnerUUID")))throw new IllegalStateException("Esse Pokémon não pertence à sua equipe");
  if(!(Boolean)call(mon,"getTradeable"))throw new IllegalStateException("Esse Pokémon não pode ser negociado");
  if(call(mon,"getEntity")!=null||call(mon,"getTetheringId")!=null)throw new IllegalStateException("Recolha o Pokémon antes de vender");
  if(!((net.minecraft.item.ItemStack)call(mon,"getHeldItem$common")).isEmpty())throw new IllegalStateException("Retire o item equipado antes de vender");
  Mon info=list.stream().filter(v->v.uuid.equals(uuid)).findFirst().orElseThrow();
  String lower=info.snapshot.toLowerCase(Locale.ROOT);
  if(info.species.endsWith(":egg")||lower.contains("egg=true")||lower.contains("is_egg=true")||lower.contains("breed_egg"))throw new IllegalStateException("Ovos não podem ser vendidos");
  backend(party(p)); // Capability checked before removal.
  return info;
 }
 private record Backend(Object factory,Object adapter,Executor executor){}
 private Backend backend(Object store)throws Exception{
  Class<?> file=Class.forName("com.cobblemon.mod.common.api.storage.factory.FileBackedPokemonStoreFactory");Class<?> adapterClass=Class.forName("com.cobblemon.mod.common.api.storage.adapter.flatfile.OneToOneFileStoreAdapter");
  for(Object f:(Iterable<?>)hidden(manager(),"factories"))if(file.isInstance(f)&&(Boolean)call(f,"isCached",store)){
   Object adapter=hidden(f,"adapter");if(!adapterClass.isInstance(adapter))continue;
   return new Backend(f,adapter,(Executor)hidden(f,"saveExecutor"));
  }
  throw new IllegalStateException("Venda exige armazenamento flat-file NBT/JSON padrão do Cobblemon 1.8.3");
 }
 public void remove(ServerPlayerEntity p,UUID uuid)throws Exception{
  Object mon=findInParty(p,uuid);if(mon==null)throw new IllegalStateException("Pokémon mudou de local");
  if(!(Boolean)call(party(p),"remove",mon))throw new IllegalStateException("Cobblemon recusou a retirada");
 }
 public boolean restore(ServerPlayerEntity p,UUID uuid,String snapshot)throws Exception{
  if(anywhere(p,uuid))return false;Object store=party(p);if((Integer)call(store,"occupied")>=6)throw new IllegalStateException("Libere uma vaga na equipe para recuperar seu Pokémon");
  Object mon=Class.forName("com.cobblemon.mod.common.pokemon.Pokemon").getConstructor().newInstance();call(mon,"loadFromJSON",p.getServer().getRegistryManager(),parse(snapshot));
  if(!call(mon,"getUuid").equals(uuid))throw new IllegalStateException("UUID do backup divergente");
  int slot=-1;for(int i=0;i<6;i++)if(call(store,"get",i)==null){slot=i;break;}
  if(slot<0)throw new IllegalStateException("Equipe cheia");call(store,"set",slot,mon);return true;
 }
 /** Snapshot on main thread; serialized writes queue behind native autosaves, then atomic replace+fsync. */
 public CompletableFuture<Void> durable(ServerPlayerEntity p)throws Exception{
  Object store=party(p);Backend b=backend(store);Object serialized=call(b.adapter,"serialize",store,p.getServer().getRegistryManager());UUID id=(UUID)call(store,"getUuid");
  File destination=(File)call(b.adapter,"getFile",store.getClass(),id);
  return CompletableFuture.runAsync(()->{
   Path temp=null;try{
    Path target=destination.toPath().toAbsolutePath();Files.createDirectories(target.getParent());temp=Files.createTempFile(target.getParent(),".zorykai-sale-",".pending");
    call(b.adapter,"save",temp.toFile(),serialized);
    if(Files.size(temp)==0)throw new java.io.IOException("Gravação de equipe vazia");
    if(serialized instanceof JsonObject json&&!JsonParser.parseString(Files.readString(temp)).equals(json))throw new java.io.IOException("Verificação JSON divergente");
    if(serialized instanceof NbtCompound nbt&&!NbtIo.readCompressed(temp,NbtSizeTracker.ofUnlimitedBytes()).equals(nbt))throw new java.io.IOException("Verificação NBT divergente");
    try(var c=FileChannel.open(temp,StandardOpenOption.WRITE)){c.force(true);}
    Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    try(var c=FileChannel.open(target.getParent(),StandardOpenOption.READ)){c.force(true);}
   }catch(Exception error){throw new CompletionException(error);}finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(Exception ignored){}}
  },b.executor);
 }
 public Map<String,String> liveBuckets()throws Exception{
  if(!available())return Map.of();var type=Class.forName("com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools");Object pool=type.getField("WORLD_SPAWN_POOL").get(null);
  var result=new HashMap<String,String>();var rank=Map.of("common",0,"uncommon",1,"rare",2,"ultra-rare",3);
  for(Object detail:(Iterable<?>)pool){
   String bucket=String.valueOf(call(detail,"getBucket"));if(!rank.containsKey(bucket)||((Number)call(detail,"getWeight")).doubleValue()<=0)continue;
   String kind=String.valueOf(call(detail,"getType"));List<Object> props=new ArrayList<>();
   if(kind.equals("pokemon"))props.add(call(detail,"getPokemon"));
   else if(kind.equals("pokemon-herd"))for(Object member:(Iterable<?>)call(detail,"getHerdablePokemon"))if(((Number)call(member,"getWeight")).doubleValue()>0)props.add(call(member,"getPokemon"));
   for(Object property:props){Object raw=call(property,"getSpecies");if(raw==null)continue;String key=raw.toString().replaceFirst("^cobblemon:","");result.merge(key,bucket,(a,b)->rank.get(a)<=rank.get(b)?a:b);}
  }
  return Map.copyOf(result);
 }
}
