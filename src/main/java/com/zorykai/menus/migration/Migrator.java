package com.zorykai.menus.migration;
import com.zorykai.menus.*;
import com.zorykai.menus.util.ItemsJson;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import static com.zorykai.menus.config.Json.*;
public final class Migrator {
 private final ZorykaiMenus app;public Migrator(ZorykaiMenus a){app=a;}
 public String run()throws Exception{
  Path root=ZorykaiMenus.ROOT,source=root.resolve("migration/source");var data=new LegacyImporter().convert(source);
  String fingerprint;var hashes=new JsonObject();try(var files=Files.walk(source)){for(var p:files.filter(Files::isRegularFile).sorted().toList())hashes.addProperty(source.relativize(p).toString(),LegacyImporter.hash(p));}
  Path migration=root.resolve("migration");Files.createDirectories(migration);Path hashFile=migration.resolve("source-hashes.json");Files.writeString(hashFile,GSON.toJson(hashes));fingerprint=LegacyImporter.hash(hashFile);
  Path backup=migration.resolve("backups/"+fingerprint);var expectedHashes=new TreeMap<String,String>();hashes.entrySet().forEach(e->expectedHashes.put(e.getKey(),e.getValue().getAsString()));MigrationBackup.verifyOrComplete(source,backup,expectedHashes);
  var report=data.report();report.addProperty("sourceFingerprint",fingerprint);report.addProperty("backup",backup.toString());var invalid=new JsonArray();var iconFallbacks=new JsonArray();int activeProducts=0,activeShops=0,activeMenus=0;
  var manifestPath=migration.resolve("generated-hashes.json");var manifest=Files.exists(manifestPath)?parse(Files.readString(manifestPath)):new JsonObject();
  for(String group:List.of("menus","shops"))for(var entry:(group.equals("menus")?data.menus():data.shops()).entrySet()){
   var j=entry.getValue();for(String kind:List.of("buttons","products")){
    JsonArray valid=new JsonArray();for(var e:arr(j,kind)){var b=e.getAsJsonObject();if(bool(b,"disabled",false)){invalid.add(entry.getKey()+"."+str(b,"id","")+": desativado por incompatibilidade registrada");continue;}
     try{
      if(kind.equals("products"))ItemsJson.read(app.server,obj(b,"item"));
      var icon=obj(b,"icon");try{ItemsJson.read(app.server,icon);}catch(RuntimeException missingIcon){
       var fallback=new JsonObject();fallback.addProperty("id","minecraft:paper");icon.add("fallback",fallback);
       iconFallbacks.add(entry.getKey()+"."+str(b,"id","")+": "+str(icon,"id","")+" preservado; ícone temporário minecraft:paper");
      }
      ItemsJson.readIcon(app.server,icon);valid.add(b);
     }catch(Exception ex){invalid.add(entry.getKey()+"."+str(b,"id","")+": "+ex.getMessage());}
    }j.add(kind,valid);
   }
   Path relative=Path.of(group,"migrated",entry.getKey()+".json"),dst=root.resolve(relative);Files.createDirectories(dst.getParent());
   if(Files.exists(dst)&&(!manifest.has(relative.toString())||!LegacyImporter.hash(dst).equals(manifest.get(relative.toString()).getAsString()))){report.getAsJsonArray("pending").add("Conflito: arquivo editado preservado "+relative);continue;}
   Files.writeString(dst,GSON.toJson(j));manifest.addProperty(relative.toString(),LegacyImporter.hash(dst));if(bool(j,"enabled",true)){if(group.equals("shops")){activeShops++;activeProducts+=arr(j,"products").size();}else activeMenus++;}
  }
  report.add("invalidOrDisabledEntries",invalid);report.add("temporaryIconFallbacks",iconFallbacks);report.addProperty("writtenShops",activeShops);report.addProperty("writtenProducts",activeProducts);report.addProperty("writtenActiveMenus",activeMenus);report.addProperty("note","written não significa aprovado: ver validationErrors e pending. Não remover mods antigos sem homologação.");
  Files.writeString(manifestPath,GSON.toJson(manifest));var validation=app.catalog.reload(app.server,false);var errors=new JsonArray();validation.forEach(errors::add);report.add("validationErrors",errors);
  Files.writeString(migration.resolve("report.json"),GSON.toJson(report));return "Migração gravada; configuração ativa preservada. Consulte "+migration.resolve("report.json")+". Erros de validação: "+validation.size()+". Use /zmenu reload apenas após revisar.";
 }
}
