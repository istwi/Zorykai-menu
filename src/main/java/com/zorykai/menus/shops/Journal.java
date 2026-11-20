package com.zorykai.menus.shops;
import com.google.gson.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static com.zorykai.menus.config.Json.*;
/** Internal crash journal, not a player-facing transaction history. FULL synchronous SQLite commits. */
public final class Journal implements AutoCloseable {
 private final Connection db;
 public Journal(Path file)throws Exception{Files.createDirectories(file.getParent());db=DriverManager.getConnection("jdbc:sqlite:"+file);try(var s=db.createStatement()){s.execute("PRAGMA journal_mode=WAL");s.execute("PRAGMA synchronous=FULL");s.execute("CREATE TABLE IF NOT EXISTS pending(id TEXT PRIMARY KEY, player TEXT UNIQUE NOT NULL, payload TEXT NOT NULL)");}}
 public void put(JsonObject tx)throws Exception{try(var s=db.prepareStatement("INSERT INTO pending(id,player,payload) VALUES(?,?,?) ON CONFLICT(id) DO UPDATE SET payload=excluded.payload")){s.setString(1,str(tx,"id",""));s.setString(2,str(tx,"player",""));s.setString(3,GSON.toJson(tx));s.executeUpdate();}}
 public JsonObject get(UUID player)throws Exception{try(var s=db.prepareStatement("SELECT payload FROM pending WHERE player=?")){s.setString(1,player.toString());try(var r=s.executeQuery()){return r.next()?parse(r.getString(1)):null;}}}
 public void done(JsonObject tx)throws Exception{try(var s=db.prepareStatement("DELETE FROM pending WHERE id=?")){s.setString(1,str(tx,"id",""));s.executeUpdate();}}
 public int count()throws Exception{try(var s=db.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM pending")){return r.getInt(1);}}
 public void close()throws Exception{db.close();}
}
