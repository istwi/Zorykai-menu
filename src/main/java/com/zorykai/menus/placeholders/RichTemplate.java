package com.zorykai.menus.placeholders;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Values are inserted as components, never concatenated into markup or serialized JSON. */
public final class RichTemplate {
    private static final Pattern TOKEN=Pattern.compile("%([^%\\n]+)%|\\{(player|uuid|balance)}");
    private RichTemplate() {}
    public static Component render(String template, Function<String, Component> resolve) {
        var matcher=TOKEN.matcher(template);var output=new StringBuilder();
        var tags=TagResolver.builder();int index=0;
        while(matcher.find()) {
            String token=matcher.group(1)!=null?matcher.group(1):matcher.group(2);
            Component value=resolve.apply(token);
            if(value==null){matcher.appendReplacement(output,java.util.regex.Matcher.quoteReplacement(matcher.group()));continue;}
            String tag="zorykai_value_"+(index++);
            tags.resolver(Placeholder.component(tag,value));
            matcher.appendReplacement(output,"<"+tag+">");
        }
        matcher.appendTail(output);
        return MiniMessage.miniMessage().deserialize(output.toString(),tags.build());
    }
    public static boolean nativeToken(String token) {
        return token.equals("luckperms:prefix")||token.equals("shirokai:dollars")||token.matches("player:objective [A-Za-z0-9_.-]{1,64}");
    }
}
