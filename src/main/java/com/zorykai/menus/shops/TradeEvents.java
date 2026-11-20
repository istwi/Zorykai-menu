package com.zorykai.menus.shops;
import com.zorykai.menus.ZorykaiMenus;
import com.zorykai.menus.api.ZorykaiMenusAPI.*;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.*;
import static com.zorykai.menus.config.Json.*;
/** Core dispatch is package-private: integrations register observers, they cannot forge confirmed events through the facade. */
public final class TradeEvents {
 private static final List<Predicate<Trade>> BEFORE_BUY=new CopyOnWriteArrayList<>(),BEFORE_SELL=new CopyOnWriteArrayList<>();
 private static final List<Consumer<Trade>> AFTER_BUY=new CopyOnWriteArrayList<>(),AFTER_SELL=new CopyOnWriteArrayList<>();
 private TradeEvents(){}
 public static AutoCloseable listenBefore(Direction direction,Predicate<Trade> handler){var list=direction==Direction.BUY?BEFORE_BUY:BEFORE_SELL;list.add(Objects.requireNonNull(handler));var closed=new java.util.concurrent.atomic.AtomicBoolean();return ()->{if(closed.compareAndSet(false,true))list.remove(handler);};}
 public static AutoCloseable listenAfter(Direction direction,Consumer<Trade> handler){var list=direction==Direction.BUY?AFTER_BUY:AFTER_SELL;list.add(Objects.requireNonNull(handler));var closed=new java.util.concurrent.atomic.AtomicBoolean();return ()->{if(closed.compareAndSet(false,true))list.remove(handler);};}
 private static void thread(){var app=ZorykaiMenus.instance;if(app==null||!app.server.isOnThread())throw new IllegalStateException("Evento exige thread principal");}
 private static void observerFailure(Throwable failure){if(failure instanceof VirtualMachineError fatal)throw fatal;if(failure instanceof ThreadDeath death)throw death;ZorykaiMenus.LOG.warn("Observador de comércio falhou",failure);}
 static boolean before(Trade trade){thread();for(var listener:trade.direction()==Direction.BUY?BEFORE_BUY:BEFORE_SELL)try{if(!listener.test(trade))return false;}catch(Throwable failure){observerFailure(failure);return false;}return true;}
 static void confirmed(Trade trade){thread();for(var listener:trade.direction()==Direction.BUY?AFTER_BUY:AFTER_SELL)try{listener.accept(trade);}catch(Throwable failure){observerFailure(failure);}}
 static Trade snapshot(JsonObject tx){
  int quantity=num(tx,"quantity",1);BigDecimal total=tx.get("total").getAsBigDecimal(),original=tx.has("originalTotal")?tx.get("originalTotal").getAsBigDecimal():total;
  return new Trade(UUID.fromString(str(tx,"id","")),UUID.fromString(str(tx,"player","")),str(tx,"shop",""),str(tx,"product",""),bool(tx,"sell",false)?Direction.SELL:Direction.BUY,quantity,original.divide(BigDecimal.valueOf(quantity)),original,tx.has("discountPercent")?tx.get("discountPercent").getAsBigDecimal():BigDecimal.ZERO,total.divide(BigDecimal.valueOf(quantity)),total,str(tx,"currency","impactor:dollars"));
 }
}
