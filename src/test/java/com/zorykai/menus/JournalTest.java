package com.zorykai.menus;
import com.zorykai.menus.shops.Journal;
import static com.zorykai.menus.config.Json.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
class JournalTest {
 @TempDir Path tmp;
 @Test void persistentExclusivePlayerLockAndExactPayload()throws Exception{
  var player=UUID.randomUUID();var tx=parse("{\"id\":\"a\",\"stage\":\"DEBIT_INTENT\",\"total\":\"99999999999999.99\"}");tx.addProperty("player",player.toString());
  try(var j=new Journal(tmp.resolve("journal.sqlite"))){j.put(tx);var other=tx.deepCopy();other.addProperty("id","b");assertThrows(Exception.class,()->j.put(other));assertEquals(1,j.count());}
  try(var j=new Journal(tmp.resolve("journal.sqlite"))){assertEquals(tx,j.get(player));tx.addProperty("stage","DEBITED");j.put(tx);assertEquals("DEBITED",str(j.get(player),"stage",""));j.done(tx);assertNull(j.get(player));assertEquals(0,j.count());}
 }
}
