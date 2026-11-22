package com.zorykai.menus.shops;
import com.google.gson.*;
import com.zorykai.menus.market.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import static com.zorykai.menus.config.Json.*;
/** Internal crash journal and market state share FULL synchronous SQLite commits. */
public final class Journal implements AutoCloseable {
 private final Connection db;
 public Journal(Path file)throws Exception{
  Files.createDirectories(file.getParent());db=DriverManager.getConnection("jdbc:sqlite:"+file);
  try(var s=db.createStatement()){
   s.execute("PRAGMA journal_mode=WAL");s.execute("PRAGMA synchronous=FULL");
   s.execute("CREATE TABLE IF NOT EXISTS pending(id TEXT PRIMARY KEY, player TEXT UNIQUE NOT NULL, payload TEXT NOT NULL)");
   s.execute("CREATE TABLE IF NOT EXISTS pokemon_sale_history(id TEXT PRIMARY KEY, player TEXT NOT NULL, pokemon TEXT NOT NULL, payload TEXT NOT NULL, result TEXT NOT NULL, finished INTEGER NOT NULL)");
   s.execute("CREATE TABLE IF NOT EXISTS market_state(key TEXT PRIMARY KEY, volume TEXT NOT NULL, updated INTEGER NOT NULL)");
   s.execute("CREATE UNIQUE INDEX IF NOT EXISTS pending_market_key ON pending(json_extract(payload,'$.marketKey')) WHERE json_extract(payload,'$.marketKey') IS NOT NULL");
  }
 }
 public synchronized void put(JsonObject tx)throws Exception{
  if(tx.has("marketKey")&&marketBusy(str(tx,"marketKey",""),str(tx,"id","")))throw new IllegalStateException("Este produto tem uma venda em processamento. Aguarde e confirme novamente.");
  try(var s=db.prepareStatement("INSERT INTO pending(id,player,payload) VALUES(?,?,?) ON CONFLICT(id) DO UPDATE SET payload=excluded.payload")){s.setString(1,str(tx,"id",""));s.setString(2,str(tx,"player",""));s.setString(3,GSON.toJson(tx));s.executeUpdate();}
 }
 private boolean marketBusy(String key,String except)throws Exception{
  try(var s=db.prepareStatement("SELECT 1 FROM pending WHERE json_extract(payload,'$.marketKey')=? AND id<>? LIMIT 1")){s.setString(1,key);s.setString(2,except);try(var r=s.executeQuery()){return r.next();}}
 }
 public synchronized JsonObject get(UUID player)throws Exception{try(var s=db.prepareStatement("SELECT payload FROM pending WHERE player=?")){s.setString(1,player.toString());try(var r=s.executeQuery()){return r.next()?parse(r.getString(1)):null;}}}
 public synchronized MarketEngine.State market(String key)throws Exception{
  try(var s=db.prepareStatement("SELECT volume,updated FROM market_state WHERE key=?")){s.setString(1,key);try(var r=s.executeQuery()){return r.next()?new MarketEngine.State(new BigDecimal(r.getString(1)),r.getLong(2)):new MarketEngine.State(BigDecimal.ZERO,0);}}
 }
 public synchronized void resetMarket(String key)throws Exception{
  if(marketBusy(key,""))throw new IllegalStateException("Há uma venda pendente deste produto. Resolva-a antes do reset.");
  try(var s=db.prepareStatement("DELETE FROM market_state WHERE key=?")){s.setString(1,key);s.executeUpdate();}
 }
 public void done(JsonObject tx)throws Exception{complete(tx,false,System.currentTimeMillis());}
 /** Exactly-once pressure update and pending deletion, in the same local transaction. */
 public synchronized boolean complete(JsonObject tx,boolean success,long now)throws Exception{
  db.setAutoCommit(false);
  try{
   JsonObject saved;
   try(var s=db.prepareStatement("SELECT payload FROM pending WHERE id=?")){s.setString(1,str(tx,"id",""));try(var r=s.executeQuery()){saved=r.next()?parse(r.getString(1)):null;}}
   if(saved==null){db.commit();return false;}
   if(success&&bool(saved,"sell",false)&&saved.has("marketKey")){
    String key=str(saved,"marketKey","");var policy=MarketPolicy.parse(obj(saved,"marketPolicy"));
    var next=MarketEngine.sold(market(key),policy,num(saved,"quantity",0),now);
    try(var s=db.prepareStatement("INSERT INTO market_state(key,volume,updated) VALUES(?,?,?) ON CONFLICT(key) DO UPDATE SET volume=excluded.volume,updated=excluded.updated")){s.setString(1,key);s.setString(2,next.volume().toPlainString());s.setLong(3,next.updatedAt());s.executeUpdate();}
   }
   try(var s=db.prepareStatement("DELETE FROM pending WHERE id=?")){s.setString(1,str(saved,"id",""));s.executeUpdate();}
   db.commit();return true;
  }catch(Exception failure){db.rollback();throw failure;}finally{db.setAutoCommit(true);}
 }
 /** Keep the full original Pokemon snapshot as audit evidence, atomically with pending completion. */
 public synchronized void finishPokemon(JsonObject tx,String result)throws Exception{
  db.setAutoCommit(false);try{
   try(var s=db.prepareStatement("INSERT OR IGNORE INTO pokemon_sale_history VALUES(?,?,?,?,?,?)")){
    s.setString(1,str(tx,"id",""));s.setString(2,str(tx,"player",""));s.setString(3,str(tx,"pokemon",""));s.setString(4,GSON.toJson(tx));s.setString(5,result);s.setLong(6,System.currentTimeMillis());s.executeUpdate();
   }
   try(var s=db.prepareStatement("DELETE FROM pending WHERE id=?")){s.setString(1,str(tx,"id",""));s.executeUpdate();}
   db.commit();
  }catch(Exception e){db.rollback();throw e;}finally{db.setAutoCommit(true);}
 }
 public synchronized int pokemonSoldBetween(UUID player,long start,long end)throws Exception{
  try(var s=db.prepareStatement("SELECT COUNT(*) FROM pokemon_sale_history WHERE player=? AND result='sold' AND finished>=? AND finished<? AND json_extract(payload,'$.dailyLimited')=1")){
   s.setString(1,player.toString());s.setLong(2,start);s.setLong(3,end);try(var r=s.executeQuery()){return r.getInt(1);}
  }
 }
 public synchronized int count()throws Exception{try(var s=db.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM pending")){return r.getInt(1);}}
 public synchronized void close()throws Exception{db.close();}
}
