package com.zorykai.menus;
import com.zorykai.menus.pricing.PriceEngine;
import org.junit.jupiter.api.Test;
import java.math.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MaximumTest {
 @Test void exampleVipMaximum50(){assertEquals(50,PriceEngine.maximum(new BigDecimal("9000"),64,2304,n->PriceEngine.quote(new BigDecimal("200"),BigDecimal.TEN,n,2,RoundingMode.HALF_UP).total()));}
 @Test void fullEmptyZeroAndLimits(){assertEquals(0,PriceEngine.maximum(BigDecimal.TEN,0,64,n->BigDecimal.ZERO));assertEquals(32,PriceEngine.maximum(BigDecimal.ZERO,64,32,n->BigDecimal.ZERO));assertEquals(0,PriceEngine.maximum(BigDecimal.ONE,64,64,n->new BigDecimal("2").multiply(BigDecimal.valueOf(n))));assertThrows(Exception.class,()->PriceEngine.maximum(BigDecimal.ONE,-1,64,n->BigDecimal.ONE));assertThrows(Exception.class,()->PriceEngine.maximum(BigDecimal.TEN,64,64,n->new BigDecimal("-1")));}
 @Test void matchesBruteForceUsingActualRoundedQuotes(){var random=new Random(1901);for(int trial=0;trial<400;trial++){BigDecimal base=BigDecimal.valueOf(random.nextInt(500),2),discount=BigDecimal.valueOf(random.nextInt(101)),balance=BigDecimal.valueOf(random.nextInt(5000),2);int cap=random.nextInt(100),limit=random.nextInt(100);int expected=0;for(int n=1;n<=Math.min(cap,limit);n++)if(PriceEngine.quote(base,discount,n,2,RoundingMode.HALF_EVEN).total().compareTo(balance)<=0)expected=n;assertEquals(expected,PriceEngine.maximum(balance,cap,limit,n->PriceEngine.quote(base,discount,n,2,RoundingMode.HALF_EVEN).total()));}}
}
