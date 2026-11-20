package com.zorykai.menus.integrations.cobblemon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.zorykai.menus.ZorykaiMenus;
import static com.zorykai.menus.config.Json.*;
/** Optional, read-only API adapter. No Cobblemon class is linked when the mod is absent. */
public final class CobblemonAccess {
 public record Party(boolean available,int count,int free,String firstName,int firstLevel,int occupiedMask,int shinyMask){
  public boolean occupied(int slot){return slot>=0&&slot<6&&(occupiedMask&(1<<slot))!=0;}
  public boolean shiny(int slot){return occupied(slot)&&(shinyMask&(1<<slot))!=0;}
 }
 private static final Party MISSING=new Party(false,0,0,"",0,0,0);
 private CobblemonAccess(){}
 public static String fallback(){var app=ZorykaiMenus.instance;return app==null?"N/D":str(obj(app.catalog.get().config(),"cobblemon"),"fallback","N/D");}
 public static boolean loaded(){var app=ZorykaiMenus.instance;return FabricLoader.getInstance().isModLoaded("cobblemon")&&(app==null||bool(obj(app.catalog.get().config(),"cobblemon"),"enabled",true));}
 public static Party snapshot(ServerPlayerEntity player){
  if(!loaded()||player.getServer()==null||!player.getServer().isOnThread())return MISSING;
  try{
   Class<?> main=Class.forName("com.cobblemon.mod.common.Cobblemon");var instance=main.getField("INSTANCE").get(null);var storage=main.getMethod("getStorage").invoke(instance);
   var party=storage.getClass().getMethod("getParty",ServerPlayerEntity.class).invoke(storage,player);var get=party.getClass().getMethod("get",int.class);
   int occupied=0,shiny=0,count=0,level=0;String name="";
   for(int slot=0;slot<6;slot++){var pokemon=get.invoke(party,slot);if(pokemon==null)continue;occupied|=1<<slot;count++;
    if((Boolean)pokemon.getClass().getMethod("getShiny").invoke(pokemon))shiny|=1<<slot;
    if(count==1){name=((Text)pokemon.getClass().getMethod("getDisplayName",boolean.class).invoke(pokemon,false)).getString();level=(Integer)pokemon.getClass().getMethod("getLevel").invoke(pokemon);}
   }
   return new Party(true,count,6-count,name,level,occupied,shiny);
  }catch(ReflectiveOperationException|LinkageError failure){ZorykaiMenus.LOG.debug("API Cobblemon indisponível",failure);return MISSING;}
 }
}
