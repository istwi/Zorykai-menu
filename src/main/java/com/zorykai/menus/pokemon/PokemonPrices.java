package com.zorykai.menus.pokemon;

import com.google.gson.*;
import java.math.*;
import java.util.*;
import static com.zorykai.menus.config.Json.*;

/** Conservative acquisition price: the cheapest direct, fishing or ancestral route wins. */
public final class PokemonPrices {
 public record Quote(BigDecimal total,String bucket,String explanation){}
 private final JsonObject profiles;
 public PokemonPrices(JsonObject profiles){this.profiles=profiles.deepCopy();}
 public static void validate(JsonObject cfg){
  if(num(cfg,"schemaVersion",0)!=1)throw new IllegalArgumentException("pokemon-sales.schemaVersion: esperado 1");
  amount(cfg,"referencePrice",true);amount(cfg,"maxPrice",true);
  multiplier(cfg,"shinyMultiplier",1,100);multiplier(cfg,"evolutionMultiplier",1,10);
  if(num(cfg,"rareDailyLimit",10)<1||num(cfg,"rareDailyLimit",10)>100000)throw new IllegalArgumentException("rareDailyLimit inválido");
  java.time.ZoneId.of(str(cfg,"dailyTimezone","America/Sao_Paulo"));
  if(num(cfg,"keepParty",1)<1||num(cfg,"keepParty",1)>6)throw new IllegalArgumentException("keepParty: 1 a 6");
  if(num(cfg,"confirmationSeconds",60)<10||num(cfg,"confirmationSeconds",60)>300)throw new IllegalArgumentException("confirmationSeconds: 10 a 300");
  for(String bucket:List.of("common","uncommon","rare","ultra-rare"))amount(obj(cfg,"bucketMultipliers"),bucket,true);
  for(var e:obj(cfg,"speciesPrices").entrySet()){id(e.getKey());if(e.getValue().getAsBigDecimal().signum()<=0)throw new IllegalArgumentException("Preço por espécie deve ser positivo");amount(obj(cfg,"speciesPrices"),e.getKey(),true);}
  for(String species:strings(cfg,"blockedSpecies"))id(species);
 }
 private static void id(String s){if(!s.matches("[a-z0-9_.-]+(?::[a-z0-9_./-]+)?"))throw new IllegalArgumentException("ID de espécie inválido: "+s);}
 private static BigDecimal amount(JsonObject cfg,String key,boolean positive){
  BigDecimal n=cfg.get(key).getAsBigDecimal();if(n.precision()>20||Math.max(0,n.scale())>8||(positive?n.signum()<=0:n.signum()<0))throw new IllegalArgumentException(key+": valor inválido");return n;
 }
 private static void multiplier(JsonObject c,String key,int min,int max){BigDecimal n=amount(c,key,true);if(n.compareTo(BigDecimal.valueOf(min))<0||n.compareTo(BigDecimal.valueOf(max))>0)throw new IllegalArgumentException(key+": fora do intervalo");}
 public Quote quote(JsonObject cfg,String species,boolean shiny,int decimals,Map<String,String> liveBuckets){
  String key=species.replaceFirst("^cobblemon:","");
  if(!bool(cfg,"enabled",false))throw new IllegalArgumentException("Venda de Pokémon desativada");
  if(strings(cfg,"blockedSpecies").contains(key)||strings(cfg,"blockedSpecies").contains(species))throw new IllegalArgumentException("Esta espécie não pode ser vendida");
  BigDecimal base;String bucket,reason;
  var fixed=obj(cfg,"speciesPrices");String override=fixed.has(species)?species:key;
  if(fixed.has(override)){base=fixed.get(override).getAsBigDecimal();bucket="manual";reason="Preço definido pelo servidor";}
  else{
   var route=cheapest(cfg,key,liveBuckets,new HashSet<>());
   if(route==null)throw new IllegalArgumentException("Espécie sem referência de obtenção; administrador pode definir um preço no JSON");
   base=route.total;bucket=route.bucket;reason=route.explanation;
  }
  if(shiny){base=base.multiply(cfg.get("shinyMultiplier").getAsBigDecimal());reason+=" · shiny ×"+cfg.get("shinyMultiplier").getAsString();}
  BigDecimal total=base.min(cfg.get("maxPrice").getAsBigDecimal()).setScale(decimals,RoundingMode.DOWN);
  if(total.signum()<=0)throw new IllegalArgumentException("Preço arredondado para zero");return new Quote(total,bucket,reason);
 }
 public String speciesBucket(String species,Map<String,String> live){
  String key=species.replaceFirst("^cobblemon:","");var rank=Map.of("common",0,"uncommon",1,"rare",2,"ultra-rare",3);
  String a=profiles.has(key)?str(profiles.getAsJsonObject(key),"bucket",""):"",b=live.getOrDefault(key,"");
  if(!rank.containsKey(a))return rank.containsKey(b)?b:"unknown";
  return rank.containsKey(b)&&rank.get(b)<rank.get(a)?b:a;
 }
 public boolean dailyLimited(String species,Map<String,String> live){return !Set.of("common","uncommon").contains(speciesBucket(species,live));}
 private Quote cheapest(JsonObject cfg,String key,Map<String,String> live,Set<String> seen){
  if(!seen.add(key))return null;JsonObject profile=profiles.has(key)?profiles.getAsJsonObject(key):new JsonObject();
  Quote best=null;var weights=obj(cfg,"bucketMultipliers");
  for(String bucket:List.of(str(profile,"bucket",""),live.getOrDefault(key,""))){
   if(!weights.has(bucket))continue;
   var amount=cfg.get("referencePrice").getAsBigDecimal().multiply(weights.get(bucket).getAsBigDecimal());
   if(best==null||amount.compareTo(best.total)<0)best=new Quote(amount,bucket,(bool(profile,"fishing",false)?"Spawn/pesca: ":"Obtenção: ")+bucket);
  }
  for(String parent:strings(profile,"parents")){
   var inherited=cheapest(cfg,parent,live,new HashSet<>(seen));if(inherited==null)continue;
   BigDecimal cap=inherited.total.multiply(cfg.get("evolutionMultiplier").getAsBigDecimal());
   if(best==null||cap.compareTo(best.total)<0)best=new Quote(cap,inherited.bucket,"Rota de evolução a partir de "+parent);
  }
  return best;
 }
}
