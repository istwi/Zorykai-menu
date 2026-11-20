package com.zorykai.menus.migration;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import static com.zorykai.menus.config.Json.*;
/** Reads configuration data only. No legacy classes, code or resources are loaded. */
public final class LegacyImporter {
 public record Result(Map<String,JsonObject> menus,Map<String,JsonObject> shops,JsonObject report){}
 private final List<String> pending=new ArrayList<>();private int products,prices;
 private static String id(Path p){return p.getFileName().toString().replaceFirst("\\.json$","");}
 private JsonObject read(Path p)throws Exception{return parse(Files.readString(p));}
 public Result convert(Path source)throws Exception{
  Map<String,JsonObject> menus=new TreeMap<>(),shops=new TreeMap<>();int disabledMenus=0;var notes=new JsonArray();
  Path shopDir=source.resolve("skiesshop/shops"),guiDir=source.resolve("skiesguis/guis");
  Path globalConfig=source.resolve("skiesshop/config.json");JsonObject globalData=Files.exists(globalConfig)?read(globalConfig):new JsonObject();
  if(!Files.isDirectory(shopDir)&&!Files.isDirectory(guiDir))throw new IllegalArgumentException("Coloque skiesshop/ e/ou skiesguis/ em config/zorykaimenus/migration/source/");
  if(Files.isDirectory(shopDir))try(var files=Files.list(shopDir)){for(Path f:files.filter(p->p.toString().endsWith(".json")).sorted().toList()){
   var old=read(f);var j=base(id(f),old);String type=str(old,"type","GENERIC_9x6");j.addProperty("size",Integer.parseInt(type.substring(type.length()-1))*9);
   JsonArray entries=new JsonArray();int pages=1;
   for(var en:obj(old,"entries").entrySet()){
    var v=en.getValue().getAsJsonObject();products++;if(!str(v,"type","ITEM").equals("ITEM")){pending.add(f+".entries."+en.getKey()+": tipo de produto não suportado");continue;}
    var b=new JsonObject();b.addProperty("id",en.getKey());b.add("item",item(v));var display=obj(v,"display");b.add("icon",item(display.has("item")?display:v));visual(display,b);b.add("slots",ints(num(v,"slot",0)));
    JsonArray bp=arr(v,"page");if(bp.isEmpty())bp=ints(1);b.add("pages",bp.deepCopy());for(var page:bp)pages=Math.max(pages,page.getAsInt());
    for(String side:List.of("buy","sell"))if(v.has(side)){
     var money=obj(v,side);String economy=str(money,"economy","IMPACTOR"),currency=str(money,"currency","impactor:dollars");
     if(!economy.equals("IMPACTOR")||!currency.equals("impactor:dollars")){pending.add(f+"."+en.getKey()+"."+side+": economia/moeda incompatível; produto desativado");b.addProperty("disabled",true);}
     // Keep the original JSON decimal token. No double conversion, rounding or inferred price.
     b.add(side,money.get("price").deepCopy());prices++;
    }
    var templates=obj(globalData,"entry_lore");String template=b.has("buy")?(b.has("sell")?"buy_sell":"buy"):"sell";
    if(templates.has(template)){
     var lore=new JsonArray();for(String line:strings(templates,template)){
      if(line.equals("%display_item_lore%")){for(var originalLine:arr(display,"lore"))lore.add(originalLine.deepCopy());continue;}
      for(String side:List.of("buy","sell"))if(b.has(side))line=line.replace("%"+side+"_price%",b.get(side).getAsBigDecimal().toPlainString()).replace("%"+side+"_price_currency%",str(obj(v,side),"name","Dollars"));
      lore.add(line);
     }b.add("lore",lore);b.addProperty("priceLoreProvided",true);
    }
    if(v.has("permission"))b.add("permission",v.get("permission").deepCopy());
    entries.add(b);
   }
   j.addProperty("pages",pages);j.add("products",entries);j.add("buttons",buttons(obj(old,"items"),false,f.toString()));
   j.add("openActions",actions(obj(old,"open_actions"),f+".open_actions"));j.add("closeActions",actions(obj(old,"close_actions"),f+".close_actions"));shops.put(id(f),j);
  }}
  if(Files.isDirectory(guiDir))try(var files=Files.list(guiDir)){for(Path f:files.filter(p->p.toString().endsWith(".json")).sorted().toList()){
   var old=read(f);String name=id(f);var j=base(name,old);j.addProperty("size",num(old,"size",6)*9);j.add("aliases",arr(old,"alias_commands").deepCopy());
   if(unsupportedMolang(old)){j.addProperty("enabled",false);disabledMenus++;pending.add(f+": menu desativado; expressão Molang não reconhecida");}
   j.add("buttons",buttons(obj(old,"items"),true,f.toString()));j.add("openActions",actions(obj(old,"open_actions"),f+".open_actions"));j.add("closeActions",actions(obj(old,"close_actions"),f+".close_actions"));
   String target=name.matches("[a-zA-Z0-9_.-]+")?name:name.replaceAll("[^a-zA-Z0-9_.-]","_");if(!target.equals(name)){pending.add("ID de arquivo normalizado explicitamente: "+name+" -> "+target);j.addProperty("id",target);}menus.put(target,j);
  }}
  // User confirmed that this is a leftover example link, not a shop to reconstruct.
  if(!shops.containsKey("loja_blocos"))for(var map:List.of(menus,shops))for(var menu:map.entrySet()){
   var retained=new JsonArray();for(var element:arr(menu.getValue(),"buttons")){
    var button=element.getAsJsonObject();var remaining=new JsonArray();boolean removed=false;
    for(var action:arr(button,"actions")){var a=action.getAsJsonObject();
     if(str(a,"type","").equals("OPEN_SHOP")&&str(a,"id","").equals("loja_blocos"))removed=true;else remaining.add(a);
    }
    if(removed){notes.add(menu.getKey()+"."+str(button,"id","")+": referência antiga a loja_blocos removida conforme confirmação do administrador");button.add("actions",remaining);}
    if(!removed||!remaining.isEmpty())retained.add(button);
   }menu.getValue().add("buttons",retained);
  }
  Set<String> placeholders=new TreeSet<>();for(var map:List.of(menus,shops))for(var j:map.values()){
   var m=java.util.regex.Pattern.compile("%[^%\\n]+%").matcher(GSON.toJson(j));while(m.find())placeholders.add(m.group());
   // Missing destinations are represented by inactive buttons, not executable broken actions.
   for(var el:arr(j,"buttons")){var button=el.getAsJsonObject();for(var action:arr(button,"actions")){var a=action.getAsJsonObject();String t=str(a,"type","");if(t.equals("OPEN_SHOP")&&!shops.containsKey(str(a,"id",""))||t.equals("OPEN_MENU")&&!menus.containsKey(str(a,"id",""))){button.addProperty("disabled",true);pending.add(str(j,"id","")+"."+str(button,"id","")+": destino ausente "+str(a,"id",""));}}}
  }
  for(String p:placeholders)if(!com.zorykai.menus.placeholders.RichTemplate.nativeToken(p.substring(1,p.length()-1)))pending.add("Placeholder externo preservado, provedor ainda necessário: "+p);
  Path global=source.resolve("skiesshop/config.json");if(Files.exists(global)){
   var cfg=read(global);String base=str(cfg,"base_shop","");if(base.equals("example_categories"))notes.add("base_shop example_categories: valor de exemplo antigo; não exige criação de loja");
   else if(!shops.containsKey(base))pending.add("skiesshop/config.json.base_shop: destino ausente "+base+"; aliases antigos não ativados automaticamente");
   for(String command:strings(cfg,"commands"))notes.add("Comando antigo /"+command+" open <nome>: equivalente atual /zmenu shop <nome>; não exige destino fixo");
   pending.add("Presets de exemplo não referenciados pelas lojas não são importados como produtos vendáveis. Sons e condições de clique globais são substituídos pelo fluxo nativo documentado da v1.0.");
   pending.add("Templates de confirmação antigos substituídos pelos controles da v1.0; layout e quantidades rápidas antigos não são copiados. Preços e componentes dos produtos permanecem iguais.");
  }
  var report=new JsonObject();report.addProperty("identifiedShops",shops.size());report.addProperty("convertedShops",shops.size());report.addProperty("identifiedProducts",products);report.addProperty("convertedProducts",shops.values().stream().mapToInt(j->arr(j,"products").size()).sum());report.addProperty("preservedPrices",prices);report.addProperty("divergentPrices",0);report.addProperty("convertedMenus",menus.size());report.addProperty("disabledMolangMenus",disabledMenus);report.addProperty("completeReplacement",false);
  var issues=new JsonArray();pending.forEach(issues::add);report.add("pending",issues);report.add("migrationNotes",notes);return new Result(menus,shops,report);
 }
 private boolean unsupportedMolang(JsonElement value){
  if(value.isJsonArray()){for(var e:value.getAsJsonArray())if(unsupportedMolang(e))return true;}
  if(value.isJsonObject()){var j=value.getAsJsonObject();if(str(j,"type","").equals("MOLANG")){
   String script=str(j,"script","");if(LegacyExpressions.message(script).isEmpty()&&LegacyExpressions.shinySlot(script).isEmpty())return true;
   if(j.has("output")&&!Set.of("0","1").contains(str(j,"output","")))return true;
  }for(var e:j.entrySet())if(unsupportedMolang(e.getValue()))return true;}
  return false;
 }
 private JsonObject base(String id,JsonObject old){var j=new JsonObject();j.addProperty("id",id);j.addProperty("title",str(old,"title",id));if(old.has("permission"))j.add("permission",old.get("permission").deepCopy());return j;}
 private JsonObject item(JsonObject old){
  var j=new JsonObject();String id=str(old,"item","minecraft:stone");if(id.equals("playerhead")){j.addProperty("id","minecraft:player_head");j.addProperty("viewerHead",true);pending.add("Alias visual playerhead convertido para minecraft:player_head com perfil do jogador");}else j.addProperty("id",id);
  if(old.has("components"))j.add("components",old.get("components").deepCopy());else if(old.has("nbt"))j.add("components",old.get("nbt").deepCopy());
  if(old.has("amount"))j.add("count",old.get("amount").deepCopy());return j;
 }
 private void visual(JsonObject old,JsonObject j){for(String field:List.of("name","lore","permission"))if(old.has(field))j.add(field,old.get(field).deepCopy());}
 private JsonArray buttons(JsonObject items,boolean gui,String path){var out=new JsonArray();for(var entry:items.entrySet()){
  var old=entry.getValue().getAsJsonObject();var j=new JsonObject();j.addProperty("id",entry.getKey());j.add("icon",item(old));j.add("slots",arr(old,"slots").deepCopy());visual(old,j);
  j.add("actions",actions(obj(old,gui?"click_actions":"actions"),path+"."+entry.getKey()));
  if(old.has("priority"))j.add("priority",old.get("priority").deepCopy());
  if(old.has("view_requirements")){
   var requirements=obj(obj(old,"view_requirements"),"requirements");
   if(requirements.size()==1){var requirement=requirements.entrySet().iterator().next().getValue().getAsJsonObject();var slot=LegacyExpressions.shinySlot(str(requirement,"script",""));
    String output=str(requirement,"output","");
    if(str(requirement,"type","").equals("MOLANG")&&slot.isPresent()&&(output.equals("1")||output.equals("0"))){j.addProperty("partyShinySlot",slot.getAsInt());j.addProperty("shinyExpected",output.equals("1"));}
    else {j.addProperty("disabled",true);pending.add(path+"."+entry.getKey()+": condição desconhecida; botão desativado");}
   }else{j.addProperty("disabled",true);pending.add(path+"."+entry.getKey()+": grupo de condições não suportado; botão desativado");}
  }
  out.add(j);
 }return out;}
 private JsonArray actions(JsonObject old,String path){var out=new JsonArray();for(var en:old.entrySet()){
  var a=en.getValue().getAsJsonObject().deepCopy();String t=str(a,"type","");
  if(t.equals("MOLANG")){var message=LegacyExpressions.message(str(a,"script",""));if(message.isPresent()){a.addProperty("message",net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().escapeTags(message.get()));t="SEND_MESSAGE";}}
  t=switch(t){case "OPEN_GUI"->"OPEN_MENU";case "COMMAND_PLAYER"->"PLAYER_COMMAND";case "COMMAND_CONSOLE"->"SERVER_COMMAND";case "PLAYSOUND"->"PLAY_SOUND";default->t;};
  if(!Set.of("OPEN_MENU","OPEN_SHOP","PLAYER_COMMAND","SERVER_COMMAND","CLOSE_MENU","BACK","NEXT_PAGE","PREVIOUS_PAGE","SEND_MESSAGE","PLAY_SOUND").contains(t)){pending.add(path+": ação não representável "+t);continue;}
  a.addProperty("type",t);if(t.equals("SERVER_COMMAND")){a.addProperty("permission","zorykaimenus.actions.server");pending.add(path+": comando privilegiado exige autorização explícita e allowServerCommands");}
  out.add(a);
 }return out;}
 private static JsonArray ints(int n){var a=new JsonArray();a.add(n);return a;}
 public static String hash(Path p)throws Exception{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}
}
