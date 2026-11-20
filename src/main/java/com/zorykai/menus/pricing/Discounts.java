package com.zorykai.menus.pricing;
import com.google.gson.*;
import java.math.*;
import java.util.*;
import java.util.function.Predicate;
import static com.zorykai.menus.config.Json.*;
/** Read-only rule evaluation. A specific rule overrides only when explicitly requested. */
public final class Discounts {
 private Discounts(){}
 public static void validate(JsonObject config){
  if(bool(config,"stackDiscounts",false))throw new IllegalArgumentException("stackDiscounts: acumulação não permitida; use false");
  BigDecimal cap=config.has("maxDiscountPercent")?config.get("maxDiscountPercent").getAsBigDecimal():new BigDecimal("50");
  percent(cap,new BigDecimal("100"),"maxDiscountPercent");
  RoundingMode mode=RoundingMode.valueOf(str(config,"roundingMode","HALF_UP"));if(mode==RoundingMode.UNNECESSARY)throw new IllegalArgumentException("roundingMode: selecione um modo que permita arredondamento");
  validateRule(config,cap,"groups");
  for(String scope:List.of("shops","categories","products"))for(var e:obj(config,scope).entrySet())validateRule(e.getValue().getAsJsonObject(),cap,scope+"."+e.getKey());
 }
 public static void validateRule(JsonObject rule,BigDecimal cap,String path){
  if(rule.has("disablePermission")&&str(rule,"disablePermission","").isBlank())throw new IllegalArgumentException(path+".disablePermission: não pode ser vazia");
  for(var element:arr(rule,"groups")){var group=element.getAsJsonObject();if(str(group,"permission","").isBlank())throw new IllegalArgumentException(path+".permission: obrigatória");
   if(!group.has("discountPercent"))throw new IllegalArgumentException(path+".discountPercent: obrigatório");percent(group.get("discountPercent").getAsBigDecimal(),cap,path+".discountPercent");}
 }
 private static void percent(BigDecimal value,BigDecimal cap,String field){if(value.signum()<0||value.compareTo(new BigDecimal("100"))>0||value.compareTo(cap)>0)throw new IllegalArgumentException(field+": intervalo0.."+cap.toPlainString());}
 public static BigDecimal applicable(JsonObject config,String shopId,JsonObject shop,JsonObject product,Predicate<String> granted){
  if(!bool(config,"enabled",true))return BigDecimal.ZERO;
  List<JsonObject> rules=new ArrayList<>();rules.add(config);
  rules.add(obj(obj(config,"shops"),shopId));rules.add(obj(shop,"discount"));
  String category=str(product,"category",str(shop,"category",""));if(!category.isBlank())rules.add(obj(obj(config,"categories"),category));
  rules.add(obj(obj(config,"products"),shopId+"/"+str(product,"id","")));rules.add(obj(product,"discount"));
  BigDecimal best=BigDecimal.ZERO;
  for(var rule:rules){if(!bool(rule,"enabled",true)||rule.has("disablePermission")&&granted.test(str(rule,"disablePermission","")))return BigDecimal.ZERO;if(bool(rule,"override",false))best=BigDecimal.ZERO;
   for(var element:arr(rule,"groups")){var group=element.getAsJsonObject();if(granted.test(str(group,"permission","")))best=best.max(group.get("discountPercent").getAsBigDecimal());}}
  BigDecimal cap=config.has("maxDiscountPercent")?config.get("maxDiscountPercent").getAsBigDecimal():new BigDecimal("50");return best.min(cap);
 }
}
