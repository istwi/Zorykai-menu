package com.zorykai.menus.shops;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DeliveryReviewTest {
 @TempDir Path directory;

 @Test void oldDeliveryDoesNotBlockNewPurchaseAndCannotBeRefundedTwice() throws Exception {
  var player=UUID.randomUUID();
  var original=new JsonObject();
  original.addProperty("id","original");
  original.addProperty("player",player.toString());
  original.addProperty("stage","DELIVER_INTENT");
  try(var journal=new Journal(directory.resolve("pending.sqlite"))){
   journal.put(original);
   journal.deferDelivery(player,"original");
  }
  try(var journal=new Journal(directory.resolve("pending.sqlite"))){
   assertNull(journal.get(player));
   assertEquals(List.of("original"),journal.deliveryReviews(player));
   var next=original.deepCopy();
   next.addProperty("id","next");
   journal.put(next);
   assertThrows(java.sql.SQLException.class,()->journal.resolveDeliveryReview(player,"original",false));
   journal.done(next);
   assertTrue(journal.resolveDeliveryReview(player,"original",false));
   assertEquals("REFUND_PENDING",journal.get(player).get("stage").getAsString());
   assertFalse(journal.resolveDeliveryReview(player,"original",false));
  }
 }
}
