package com.zorykai.menus.pricing;
import com.zorykai.menus.ZorykaiMenus;
import com.zorykai.menus.permissions.Permissions;
import net.minecraft.server.network.ServerPlayerEntity;
import com.google.gson.JsonObject;
import java.math.*;
import static com.zorykai.menus.config.Json.*;
public final class Pricing {
 private final ZorykaiMenus app;public Pricing(ZorykaiMenus a){app=a;}
 public PriceEngine.Quote quote(ServerPlayerEntity player,String shopId,JsonObject shop,JsonObject product,boolean sell,int quantity){
  if(!bool(shop,"available",true)||!bool(product,"available",true))throw new IllegalArgumentException("produto indisponível");
  var snapshot=app.catalog.get();BigDecimal percent=sell?BigDecimal.ZERO:Discounts.applicable(snapshot.discounts(),shopId,shop,product,node->Permissions.explicit(player,node));
  return PriceEngine.quote(product.get(sell?"sell":"buy").getAsBigDecimal(),percent,quantity,app.economy.decimals(),RoundingMode.valueOf(str(snapshot.discounts(),"roundingMode","HALF_UP")));
 }
 public int limit(JsonObject product){return Math.min(num(app.catalog.get().config(),"maxQuantity",2304),num(product,"maxQuantity",num(app.catalog.get().config(),"maxQuantity",2304)));}
}
