package com.zorykai.menus.checks;
import com.zorykai.menus.economy.impactor.ImpactorGateway;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import java.math.BigDecimal;
/** Test-only gate: provider really persists the debit, delivery response waits for a controlled interruption. */
final class DeferredDebit extends ImpactorGateway {
 final CompletableFuture<Boolean> saved=new CompletableFuture<>();final CompletableFuture<Void> release=new CompletableFuture<>();
 DeferredDebit(){super(()->"impactor:dollars");}
 @Override public CompletableFuture<Boolean> change(UUID player,String currency,BigDecimal amount,boolean deposit){
  var result=super.change(player,currency,amount,deposit);if(deposit)return result;
  return result.whenComplete((ok,error)->{if(error!=null)saved.completeExceptionally(error);else saved.complete(ok);}).thenCompose(ok->release.thenApply(ignored->ok));
 }
}
