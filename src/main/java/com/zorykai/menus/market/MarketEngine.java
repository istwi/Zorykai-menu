package com.zorykai.menus.market;
import com.zorykai.menus.pricing.PriceEngine;
import java.math.*;

/** Deterministic pricing: progressive batch volume, bounded floor, quantized idle recovery. */
public final class MarketEngine {
 private static final MathContext MC=MathContext.DECIMAL128;
 public record State(BigDecimal volume,long updatedAt){public State{if(volume==null||volume.signum()<0||updatedAt<0)throw new IllegalArgumentException("Estado do mercado inválido");}}
 public static State recover(State state,MarketPolicy rule,long now){
  long elapsed=now<=state.updatedAt()?0:now-state.updatedAt();long steps=elapsed/(rule.recoverySeconds()*1000L);
  return new State(state.volume().subtract(BigDecimal.valueOf(steps).multiply(BigDecimal.valueOf(rule.recoveryVolume()))).max(BigDecimal.ZERO).min(rule.cap()),state.updatedAt());
 }
 public static State sold(State old,MarketPolicy rule,int quantity,long now){
  if(quantity<1||quantity>1000000)throw new IllegalArgumentException("Quantidade inválida");var s=recover(old,rule,now);
  return new State(s.volume().add(BigDecimal.valueOf(quantity)).min(rule.cap()),Math.max(now,old.updatedAt()));
 }
 public static PriceEngine.Quote quote(BigDecimal base,State state,MarketPolicy rule,int quantity,int decimals,long now){
  if(base.signum()<0||quantity<1||quantity>1000000||decimals<0||decimals>30)throw new IllegalArgumentException("Cotação de mercado inválida");
  base=base.setScale(decimals,RoundingMode.UNNECESSARY);BigDecimal q=BigDecimal.valueOf(quantity),original=base.multiply(q);
  if(base.signum()==0)return new PriceEngine.Quote(base,BigDecimal.ZERO,base,quantity,original,original,BigDecimal.ZERO);
  BigDecimal floor=base.multiply(rule.minimumPercent().movePointLeft(2)).setScale(decimals,RoundingMode.CEILING);
  BigDecimal slope=rule.dropPercent().movePointLeft(2).divide(BigDecimal.valueOf(rule.volumePerDrop()),MC),volume=recover(state,rule,now).volume();
  // Number of units before the floor. Closed-form series avoids a loop up to a million items.
  BigDecimal cutoff=BigDecimal.ONE.subtract(floor.divide(base,MC)).divide(slope,MC).subtract(volume);
  int n=cutoff.signum()<=0?0:cutoff.min(q).setScale(0,RoundingMode.CEILING).intValueExact();
  BigDecimal active=BigDecimal.valueOf(n),pairs=active.multiply(active.subtract(BigDecimal.ONE)).divide(BigDecimal.valueOf(2));
  BigDecimal raw=base.multiply(active.multiply(BigDecimal.ONE.subtract(slope.multiply(volume))).subtract(slope.multiply(pairs)))
   .add(floor.multiply(BigDecimal.valueOf(quantity-n)));
  BigDecimal total=raw.setScale(decimals,RoundingMode.FLOOR).max(floor.multiply(q)).min(original);
  BigDecimal average=total.divide(q,Math.min(30,decimals+6),RoundingMode.HALF_UP);
  return new PriceEngine.Quote(base,BigDecimal.ZERO,average,quantity,original,total,original.subtract(total));
 }
}
