package com.zorykai.menus;
import com.zorykai.menus.market.*;
import com.zorykai.menus.shops.Journal;
import com.google.gson.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.zorykai.menus.config.Json.*;
class MarketTest {
 @TempDir Path tmp;
 static final long NOW=1000000;
 MarketPolicy rule=MarketPolicy.parse(new JsonObject());
 MarketEngine.State empty=new MarketEngine.State(BigDecimal.ZERO,NOW);
 BigDecimal total(MarketEngine.State state,int quantity){return MarketEngine.quote(new BigDecimal("1.00"),state,rule,quantity,2,NOW).total();}
 @Test void onlyExplicitlySelectedProductsEnableMarket(){
  var shop=new JsonObject();var p=new JsonObject();assertTrue(MarketPolicy.of(shop,p).isEmpty());shop.add("market",parse("{\"enabled\":true}"));assertTrue(MarketPolicy.of(shop,p).isPresent());p.add("market",parse("{\"enabled\":false}"));assertTrue(MarketPolicy.of(shop,p).isEmpty());
 }
 @Test void quantityLowersItsOwnPriceAndNextSale(){
  assertEquals(new BigDecimal("1.00"),total(empty,1));assertEquals(new BigDecimal("63.68"),total(empty,64));var next=MarketEngine.sold(empty,rule,64,NOW);assertEquals(new BigDecimal("0.99"),total(next,1));
 }
 @Test void splittingCannotIncreaseRevenue(){
  for(int q:List.of(2,16,64,127,2304,5000)){
   BigDecimal sum=BigDecimal.ZERO;var state=empty;for(int i=0;i<q;i++){sum=sum.add(total(state,1));state=MarketEngine.sold(state,rule,1,NOW);}
   assertTrue(total(empty,q).compareTo(sum)>=0,"split "+q);
  }
 }
 @Test void batchMatchesHighPrecisionReference(){
  var random=new Random(9);
  for(int i=0;i<200;i++){
   var state=new MarketEngine.State(BigDecimal.valueOf(random.nextInt(6000)),NOW);int quantity=1+random.nextInt(2304);
   BigDecimal expected=BigDecimal.ZERO,pressure=state.volume().min(rule.cap()),slope=new BigDecimal("0.00015625");
   for(int j=0;j<quantity;j++)expected=expected.add(BigDecimal.ONE.subtract(slope.multiply(pressure.add(BigDecimal.valueOf(j)))).max(new BigDecimal("0.25")));
   assertEquals(0,expected.setScale(2,RoundingMode.FLOOR).compareTo(total(state,quantity)));
  }
 }
 @Test void floorCapsPressureAndOfflineIdleRecovers(){
  var floor=MarketEngine.sold(empty,rule,1000000,NOW);assertEquals(0,floor.volume().compareTo(new BigDecimal("4800")));assertEquals(new BigDecimal("16.00"),total(floor,64));
  assertEquals(0,MarketEngine.recover(floor,rule,NOW+300000).volume().compareTo(new BigDecimal("4736")));
  assertEquals(0,MarketEngine.recover(floor,rule,NOW+75L*300000).volume().signum());
 }
 @Test void recoveryUsesFullIntervalsAndEachSaleRestartsIdleClock(){
  var state=MarketEngine.sold(empty,rule,64,NOW);assertEquals(new BigDecimal("0.99"),MarketEngine.quote(new BigDecimal("1.00"),state,rule,1,2,NOW+299999).total());
  assertEquals(new BigDecimal("1.00"),MarketEngine.quote(new BigDecimal("1.00"),state,rule,1,2,NOW+300000).total());
  var later=MarketEngine.sold(state,rule,1,NOW+200000);assertEquals(NOW+200000,later.updatedAt());assertEquals(0,MarketEngine.recover(later,rule,NOW+300000).volume().compareTo(new BigDecimal("65")));
 }
 @Test void smallPricesAndClockRollbackRemainSafe(){
  var s=new MarketEngine.State(new BigDecimal("4800"),NOW);
  assertEquals(new BigDecimal("0.64"),MarketEngine.quote(new BigDecimal("0.01"),s,rule,64,2,NOW).total());
  assertEquals(s.volume(),MarketEngine.recover(s,rule,NOW-10000).volume());assertEquals(new BigDecimal("0.00"),MarketEngine.quote(BigDecimal.ZERO,empty,rule,64,2,NOW).total());
 }
 @Test void invalidAndUnknownRulesRejected(){
  for(String json:List.of("{\"enabled\":\"true\"}","{\"minimumPercent\":0}","{\"dropPercent\":-1}","{\"volumePerDrop\":1.1}","{\"recoverySeconds\":0}","{\"recovreySeconds\":300}"))assertThrows(Exception.class,()->MarketPolicy.validate(parse(json)),json);
 }
 JsonObject tx(String shop,String product,int quantity){var tx=parse("{\"stage\":\"CREDIT_INTENT\",\"sell\":true}");tx.addProperty("id",UUID.randomUUID().toString());tx.addProperty("player",UUID.randomUUID().toString());tx.addProperty("quantity",quantity);tx.addProperty("marketKey",Market.key(shop,product));tx.add("marketPolicy",rule.json());return tx;}
 @Test void productReservationSurvivesRestartAndBlocksOtherPlayer()throws Exception{
  Path file=tmp.resolve("pending.sqlite");var a=tx("farm","cactus",64);var b=tx("farm","cactus",64);
  try(var j=new Journal(file)){j.put(a);assertThrows(Exception.class,()->j.put(b));assertEquals(1,j.count());assertThrows(Exception.class,()->j.resetMarket(Market.key("farm","cactus")));}
  try(var j=new Journal(file)){assertThrows(Exception.class,()->j.put(b));assertTrue(j.complete(a,true,NOW));assertFalse(j.complete(a,true,NOW));assertEquals(0,j.market(Market.key("farm","cactus")).volume().compareTo(new BigDecimal("64")));j.put(b);assertTrue(j.complete(b,false,NOW));assertEquals(0,j.market(Market.key("farm","cactus")).volume().compareTo(new BigDecimal("64")));}
 }
 @Test void separateProductsAndShopsRemainIndependent()throws Exception{
  try(var j=new Journal(tmp.resolve("journal.sqlite"))){var a=tx("farm","cactus",64);var b=tx("other","cactus",32);var c=tx("farm","melon",16);j.put(a);j.put(b);j.put(c);j.complete(a,true,NOW);j.complete(b,true,NOW);j.complete(c,true,NOW);assertEquals(new BigDecimal("64"),j.market(Market.key("farm","cactus")).volume());assertEquals(new BigDecimal("32"),j.market(Market.key("other","cactus")).volume());assertEquals(new BigDecimal("16"),j.market(Market.key("farm","melon")).volume());j.resetMarket(Market.key("farm","cactus"));assertEquals(BigDecimal.ZERO,j.market(Market.key("farm","cactus")).volume());}
 }
 @Test void badSettlementRollsBackPendingAndMarketTogether()throws Exception{
  try(var j=new Journal(tmp.resolve("journal.sqlite"))){var t=tx("farm","cactus",0);j.put(t);assertThrows(Exception.class,()->j.complete(t,true,NOW));assertEquals(1,j.count());assertEquals(BigDecimal.ZERO,j.market(Market.key("farm","cactus")).volume());}
 }
}
