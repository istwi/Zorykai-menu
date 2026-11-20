package com.zorykai.menus.permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
public final class Permissions {
 public static boolean has(ServerPlayerEntity p,String node,boolean fallback){
  if(node==null||node.isBlank())return true;
  if(FabricLoader.getInstance().isModLoaded("luckperms"))return Luck.check(p,node,fallback);
  return p.hasPermissionLevel(4)||fallback;
 }
 public static boolean admin(ServerCommandSource s,String node){return s.hasPermissionLevel(4)||s.getEntity() instanceof ServerPlayerEntity p&&has(p,node,false);}
 private static final class Luck {
  static boolean check(ServerPlayerEntity p,String n,boolean f){
   var u=net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(p.getUuid());
   if(u==null)return p.hasPermissionLevel(4)||f;
   var v=u.getCachedData().getPermissionData().checkPermission(n);
   return v==net.luckperms.api.util.Tristate.UNDEFINED?p.hasPermissionLevel(4)||f:v.asBoolean();
  }
 }
}
