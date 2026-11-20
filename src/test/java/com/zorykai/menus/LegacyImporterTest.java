package com.zorykai.menus;
import com.zorykai.menus.migration.LegacyImporter;
import static com.zorykai.menus.config.Json.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
class LegacyImporterTest {
 private final Path input=Path.of("migration-input");
 @Test void preservesEveryOriginalPriceIdComponentAndSlot()throws Exception{
  org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(input),"migration-input não fornecido");
  var result=new LegacyImporter().convert(input);assertEquals(32,result.shops().size());assertEquals(15,result.menus().size());int count=0;
  try(var files=Files.list(input.resolve("skiesshop/shops"))){for(var f:files.filter(p->p.toString().endsWith(".json")).toList()){
   var original=parse(Files.readString(f));var converted=result.shops().get(f.getFileName().toString().replaceFirst("\\.json$",""));
   for(var e:obj(original,"entries").entrySet()){
    var old=e.getValue().getAsJsonObject();var product=arr(converted,"products").asList().stream().map(v->v.getAsJsonObject()).filter(v->str(v,"id","").equals(e.getKey())).findFirst().orElseThrow();
    assertEquals(str(old,"item",""),str(obj(product,"item"),"id",""));assertEquals(num(old,"slot",-1),arr(product,"slots").get(0).getAsInt());
    if(old.has("components"))assertEquals(old.get("components"),obj(product,"item").get("components"));
    for(String price:java.util.List.of("buy","sell"))if(old.has(price))assertEquals(0,obj(old,price).get("price").getAsBigDecimal().compareTo(product.get(price).getAsBigDecimal()));count++;
   }
  }}assertEquals(510,count);
 }
 @Test void conversionIsRepeatableAndConditionsFailClosed()throws Exception{
  org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(input),"migration-input não fornecido");var a=new LegacyImporter().convert(input);var b=new LegacyImporter().convert(input);assertEquals(a.menus(),b.menus());assertEquals(a.shops(),b.shops());assertEquals(0,a.report().get("disabledMolangMenus").getAsInt());assertEquals(24,a.menus().values().stream().flatMap(j->arr(j,"buttons").asList().stream()).filter(e->e.getAsJsonObject().has("partyShinySlot")).count());assertFalse(a.report().get("completeReplacement").getAsBoolean());
 }
 @Test void removesConfirmedObsoleteShopLinkWithoutChangingSource()throws Exception{
  org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(input),"migration-input não fornecido");
  Path original=input.resolve("skiesshop/shops/loja_vip.json.json");String before=Files.readString(original);
  var result=new LegacyImporter().convert(input);
  assertFalse(GSON.toJson(result.shops()).contains("loja_blocos"));
  assertTrue(before.contains("loja_blocos"));assertEquals(before,Files.readString(original));
  assertTrue(arr(result.report(),"migrationNotes").asList().stream().anyMatch(e->e.getAsString().contains("/tentavip open <nome>")));
  assertFalse(arr(result.report(),"pending").asList().stream().anyMatch(e->e.getAsString().contains("example_categories")||e.getAsString().contains("loja_blocos")));
 }
}
