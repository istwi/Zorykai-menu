package com.zorykai.menus.placeholders;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import java.math.BigDecimal;
public final class Texts {
 public static String replace(String s,ServerPlayerEntity p,BigDecimal balance){return s.replace("{player}",p.getGameProfile().getName()).replace("{uuid}",p.getUuidAsString()).replace("{balance}",money(balance));}
 public static Text text(String s,ServerPlayerEntity p,BigDecimal balance){
  String json=GsonComponentSerializer.gson().serialize(RichTemplate.render(s,token->NativePlaceholders.resolve(token,p,balance)));
  Text t=Text.Serialization.fromJson(json,p.getRegistryManager());return t==null?Text.literal(s):t.copy().styled(v->v.withItalic(false));
 }
 public static String money(BigDecimal x){return x==null?"indisponível":x.stripTrailingZeros().toPlainString();}
}
