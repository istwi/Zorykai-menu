package com.zorykai.menus.market;
import com.google.gson.*;
import com.zorykai.menus.shops.Journal;
import com.zorykai.menus.pricing.PriceEngine;
import java.math.*;
import java.util.*;

public final class Market {
 private final Journal journal;
 public Market(Journal journal){this.journal=journal;}
 public static String key(String shop,String product){return new Gson().toJson(List.of(shop,product));}
 public PriceEngine.Quote quote(String shop,String product,JsonObject policy,BigDecimal base,int quantity,int decimals){
  try{return MarketEngine.quote(base,journal.market(key(shop,product)),MarketPolicy.parse(policy),quantity,decimals,System.currentTimeMillis());}
  catch(Exception e){throw new IllegalStateException("Cotação de mercado indisponível",e);}
 }
 public String status(String shop,String product,MarketPolicy rule)throws Exception{var s=MarketEngine.recover(journal.market(key(shop,product)),rule,System.currentTimeMillis());return "Pressão: "+s.volume().stripTrailingZeros().toPlainString()+" unidades · piso "+rule.minimumPercent()+"% · recupera "+rule.recoveryVolume()+" unidades após cada "+rule.recoverySeconds()+"s sem venda confirmada.";}
 public void reset(String shop,String product)throws Exception{journal.resetMarket(key(shop,product));}
}
