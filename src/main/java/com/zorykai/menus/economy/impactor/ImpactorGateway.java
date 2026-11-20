package com.zorykai.menus.economy.impactor;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.kyori.adventure.key.Key;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
public final class ImpactorGateway implements AutoCloseable {
 private final ExecutorService executor=Executors.newSingleThreadExecutor(r->new Thread(r,"zorykai-economy"));
 private final Supplier<String> currency;
 public ImpactorGateway(Supplier<String> c){currency=c;}
 private Currency currency(String key){return EconomyService.instance().currencies().currency(Key.key(key)).orElseThrow(()->new IllegalStateException("Moeda não registrada: "+key));}
 public CompletableFuture<BigDecimal> balance(UUID id){return CompletableFuture.supplyAsync(()->currency(currency.get()),executor).thenCompose(c->EconomyService.instance().account(c,id)).thenApply(a->a.balance());}
 public CompletableFuture<Boolean> change(UUID id,String key,BigDecimal amount,boolean deposit){
  return CompletableFuture.supplyAsync(()->currency(key),executor).thenCompose(c->{
   if(amount.signum()<0||Math.max(0,amount.stripTrailingZeros().scale())>c.decimals())throw new IllegalArgumentException("Precisão inválida para "+key);
   return EconomyService.instance().account(c,id);
  }).thenComposeAsync(account->{
   if(amount.signum()==0)return CompletableFuture.completedFuture(true);
   if(!deposit&&account.balance().compareTo(amount)<0)return CompletableFuture.completedFuture(false);
   var result=deposit?account.deposit(amount):account.withdraw(amount);
   if(!result.successful())return CompletableFuture.completedFuture(false);
   return EconomyService.instance().save(account).thenApply(v->true);
  },executor);
 }
 @Override public void close(){executor.shutdown();}
}
