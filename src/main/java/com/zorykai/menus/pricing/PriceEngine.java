package com.zorykai.menus.pricing;
import java.math.*;
import java.util.function.IntFunction;
/** Shared monetary function for preview, confirmation and maximum selection. */
public final class PriceEngine {
 public record Quote(BigDecimal originalUnit,BigDecimal discountPercent,BigDecimal effectiveUnit,int quantity,BigDecimal originalTotal,BigDecimal total,BigDecimal savings){}
 private PriceEngine(){}
 public static Quote quote(BigDecimal base,BigDecimal percent,int quantity,int decimals,RoundingMode rounding){
  if(base.signum()<0||quantity<1||quantity>1000000||decimals<0||decimals>30||percent.signum()<0||percent.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Preço, desconto, quantidade ou precisão inválidos");
  BigDecimal monetaryBase=base.setScale(decimals,RoundingMode.UNNECESSARY);
  BigDecimal effective=percent.signum()==0?monetaryBase:monetaryBase.multiply(BigDecimal.ONE.subtract(percent.movePointLeft(2))).setScale(decimals,rounding);
  BigDecimal originalTotal=base.multiply(BigDecimal.valueOf(quantity)),total=effective.multiply(BigDecimal.valueOf(quantity));
  return new Quote(base,percent,effective,quantity,originalTotal,total,originalTotal.subtract(total));
 }
 public static int maximum(BigDecimal balance,int capacity,int limit,IntFunction<BigDecimal> total){
  if(balance==null||balance.signum()<0||capacity<0||limit<0||limit>1000000)throw new IllegalArgumentException("Limites ou saldo inválidos");
  int low=0,high=Math.min(capacity,limit);
  while(low<high){int middle=low+(high-low+1)/2;BigDecimal cost=total.apply(middle);if(cost.signum()<0)throw new IllegalArgumentException("Preço negativo");if(cost.compareTo(balance)<=0)low=middle;else high=middle-1;}
  return low;
 }
}
