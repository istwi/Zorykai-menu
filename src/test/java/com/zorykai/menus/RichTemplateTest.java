package com.zorykai.menus;
import com.zorykai.menus.placeholders.RichTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RichTemplateTest {
 @Test void valuesCannotInjectMarkupOrBreakJson(){
  String attack="\"<click:run_command:'/op x'>bad</click>";
  var component=RichTemplate.render("<aqua>{player} %shirokai:dollars%",token->Component.text(attack));
  String json=GsonComponentSerializer.gson().serialize(component);
  assertFalse(json.contains("clickEvent"));assertEquals(component,GsonComponentSerializer.gson().deserialize(json));
 }
 @Test void unknownProvidersStayVisible(){
  var rendered=RichTemplate.render("%shirokai:cash%",token->null);
  assertEquals(Component.text("%shirokai:cash%"),rendered);
 }
 @Test void onlyDocumentedNativeProvidersAreRecognized(){
  assertTrue(RichTemplate.nativeToken("luckperms:prefix"));assertTrue(RichTemplate.nativeToken("shirokai:dollars"));
  assertTrue(RichTemplate.nativeToken("player:objective gyms"));assertFalse(RichTemplate.nativeToken("shirokai:cash"));
 }
}
