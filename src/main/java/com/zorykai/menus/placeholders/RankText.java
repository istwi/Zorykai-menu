package com.zorykai.menus.placeholders;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.regex.Pattern;

/** Only trusted administrator-defined LuckPerms metadata is parsed as formatting. */
public final class RankText {
 private static final Pattern LEGACY=Pattern.compile("(?i)[&§]x(?:[&§][0-9a-f]){6}|[&§]#[0-9a-f]{6}|[&§][0-9a-fk-or]");
 private static final String[] COLORS={"black","dark_blue","dark_green","dark_aqua","dark_red","dark_purple","gold","gray","dark_gray","blue","green","aqua","red","light_purple","yellow","white"};
 private RankText(){}
 public static Component render(String raw){
  var matcher=LEGACY.matcher(raw);var normalized=new StringBuffer();
  while(matcher.find()){
   String code=matcher.group(),replacement;
   char c=Character.toLowerCase(code.charAt(1));
   if(c=='x'){String hex=code.substring(2).replace("&","").replace("§","");replacement="<reset><#"+hex+">";}
   else if(c=='#')replacement="<reset><"+code.substring(1)+">";
   else{
    int n=Character.digit(c,16);
    replacement=n>=0?"<reset><"+COLORS[n]+">":switch(c){case 'k'->"<obfuscated>";case 'l'->"<bold>";case 'm'->"<strikethrough>";case 'n'->"<underlined>";case 'o'->"<italic>";default->"<reset>";};
   }
   matcher.appendReplacement(normalized,java.util.regex.Matcher.quoteReplacement(replacement));
  }
  matcher.appendTail(normalized);
  return MiniMessage.miniMessage().deserialize(normalized.toString());
 }
}
