package com.zorykai.menus.market;
import com.google.gson.*;
import java.math.*;
import java.util.*;
import static com.zorykai.menus.config.Json.*;

/** Opt-in, inherited shop/product policy. Volumes are item units, never stacks. */
public record MarketPolicy(BigDecimal minimumPercent,BigDecimal dropPercent,int volumePerDrop,int recoveryVolume,int recoverySeconds) {
 private static final Set<String> KEYS=Set.of("enabled","minimumPercent","dropPercent","volumePerDrop","recoveryVolume","recoverySeconds");
 public static void validate(JsonObject json){
  for(var e:json.entrySet())if(!KEYS.contains(e.getKey()))throw new IllegalArgumentException("market: campo desconhecido "+e.getKey());
  if(json.has("enabled")&&(!json.get("enabled").isJsonPrimitive()||!json.getAsJsonPrimitive("enabled").isBoolean()))throw new IllegalArgumentException("market.enabled: use true/false");
  parse(json);
 }
 public static MarketPolicy parse(JsonObject json){
  BigDecimal min=number(json,"minimumPercent","25"),drop=number(json,"dropPercent","1");
  int volume=integer(json,"volumePerDrop",64),recover=integer(json,"recoveryVolume",64),seconds=integer(json,"recoverySeconds",300);
  if(min.signum()<=0||min.compareTo(new BigDecimal("100"))>0||drop.signum()<=0||drop.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("market: minimumPercent/dropPercent precisam estar em (0,100]");
  if(volume<1||volume>1000000000||recover<1||recover>1000000000||seconds<1||seconds>2592000)throw new IllegalArgumentException("market: volume 1..1000000000; recoverySeconds 1..2592000");
  return new MarketPolicy(min,drop,volume,recover,seconds);
 }
 private static BigDecimal number(JsonObject j,String k,String fallback){var n=j.has(k)?j.get(k).getAsBigDecimal():new BigDecimal(fallback);if(n.precision()>16||Math.max(0,n.scale())>8)throw new IllegalArgumentException("market."+k+": precisão inválida");return n;}
 private static int integer(JsonObject j,String k,int fallback){return j.has(k)?j.get(k).getAsBigDecimal().intValueExact():fallback;}
 public static Optional<MarketPolicy> of(JsonObject shop,JsonObject product){
  var merged=obj(shop,"market").deepCopy();for(var e:obj(product,"market").entrySet())merged.add(e.getKey(),e.getValue());
  return bool(merged,"enabled",false)?Optional.of(parse(merged)):Optional.empty();
 }
 public JsonObject json(){var j=new JsonObject();j.addProperty("enabled",true);j.addProperty("minimumPercent",minimumPercent);j.addProperty("dropPercent",dropPercent);j.addProperty("volumePerDrop",volumePerDrop);j.addProperty("recoveryVolume",recoveryVolume);j.addProperty("recoverySeconds",recoverySeconds);return j;}
 public BigDecimal cap(){return new BigDecimal("100").subtract(minimumPercent).multiply(BigDecimal.valueOf(volumePerDrop)).divide(dropPercent,MathContext.DECIMAL128);}
}
