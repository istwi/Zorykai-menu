package com.zorykai.menus;
import com.zorykai.menus.pricing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.*;
import static com.zorykai.menus.config.Json.*;
import static org.junit.jupiter.api.Assertions.*;
class PricingTest {
 private com.google.gson.JsonObject config(){return parse("{\"enabled\":true,\"maxDiscountPercent\":50,\"groups\":[{\"permission\":\"vip\",\"discountPercent\":5},{\"permission\":\"vipplus\",\"discountPercent\":10},{\"permission\":\"mvp\",\"discountPercent\":15},{\"permission\":\"mvpplus\",\"discountPercent\":20}]}");}
 @ParameterizedTest @CsvSource({"normal,1000","vip,950","vipplus,900","mvp,850","mvpplus,800"}) void exactGroupPrices(String node,String price){var p=Discounts.applicable(config(),"s",parse("{}"),parse("{\"id\":\"x\"}"),node::equals);var q=PriceEngine.quote(new BigDecimal("1000"),p,1,2,RoundingMode.HALF_UP);assertEquals(0,q.total().compareTo(new BigDecimal(price)));}
 @Test void highestPermissionWithoutStacking(){var d=Discounts.applicable(config(),"s",parse("{}"),parse("{}"),p->true);assertEquals(0,d.compareTo(new BigDecimal("20")));}
 @Test void productDisableAndSpecificOverride(){var cfg=config();var shop=parse("{}");assertEquals(BigDecimal.ZERO,Discounts.applicable(cfg,"s",shop,parse("{\"discount\":{\"enabled\":false}}"),p->true));var product=parse("{\"discount\":{\"override\":true,\"groups\":[{\"permission\":\"vip\",\"discountPercent\":3}]}}");assertEquals(0,Discounts.applicable(cfg,"s",shop,product,p->true).compareTo(new BigDecimal("3")));}
 @Test void categoryOverridesShopAndProductOverridesCategory(){var cfg=config();cfg.add("shops",parse("{\"s\":{\"override\":true,\"groups\":[{\"permission\":\"vip\",\"discountPercent\":4}]}}"));cfg.add("categories",parse("{\"balls\":{\"override\":true,\"groups\":[{\"permission\":\"vip\",\"discountPercent\":7}]}}"));assertEquals(0,Discounts.applicable(cfg,"s",parse("{}"),parse("{\"category\":\"balls\"}"),p->true).compareTo(new BigDecimal("7")));}
 @Test void permissionCanDisableDiscountForProduct(){assertEquals(BigDecimal.ZERO,Discounts.applicable(config(),"s",parse("{}"),parse("{\"discount\":{\"disablePermission\":\"product.no_discount\"}}"),node->true));}
 @Test void invalidRulesRejected(){assertThrows(Exception.class,()->Discounts.validate(parse("{\"stackDiscounts\":true}")));assertThrows(Exception.class,()->Discounts.validate(parse("{\"groups\":[{\"permission\":\"vip\",\"discountPercent\":101}]}")));assertThrows(Exception.class,()->Discounts.validate(parse("{\"maxDiscountPercent\":10,\"groups\":[{\"permission\":\"vip\",\"discountPercent\":20}]}")));}
 @Test void roundingIsPerUnitAndCurrencyPrecisionExact(){var q=PriceEngine.quote(new BigDecimal("0.05"),new BigDecimal("10"),3,2,RoundingMode.HALF_UP);assertEquals(new BigDecimal("0.15"),q.total());assertThrows(ArithmeticException.class,()->PriceEngine.quote(new BigDecimal("0.001"),BigDecimal.ZERO,1,2,RoundingMode.HALF_UP));assertThrows(ArithmeticException.class,()->PriceEngine.quote(new BigDecimal("0.001"),BigDecimal.TEN,1,2,RoundingMode.UP));}
}
