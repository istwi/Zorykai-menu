package com.zorykai.menus.placeholders;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.math.BigDecimal;

public final class NativePlaceholders {
    private NativePlaceholders() {}
    public static Component resolve(String token,ServerPlayerEntity player,BigDecimal balance) {
        return switch(token) {
            case "player" -> Component.text(player.getGameProfile().getName());
            case "uuid" -> Component.text(player.getUuidAsString());
            case "balance","shirokai:dollars" -> Component.text(Texts.money(balance));
            case "luckperms:prefix" -> FabricLoader.getInstance().isModLoaded("luckperms")?Luck.prefix(player):null;
            default -> {
                if(!token.startsWith("player:objective ")){
                    if(!FabricLoader.getInstance().isModLoaded("placeholder-api"))yield null;
                    try{yield ExternalPlaceholders.resolve(token,player);}catch(RuntimeException|LinkageError unavailable){yield null;}
                }
                String name=token.substring("player:objective ".length());
                var scoreboard=player.getServer().getScoreboard();
                var objective=scoreboard.getNullableObjective(name);
                if(objective==null)yield null;
                var score=scoreboard.getScore(player,objective);
                yield score==null?null:Component.text(score.getScore());
            }
        };
    }
    private static final class Luck {
        static Component prefix(ServerPlayerEntity player) {
            var user=net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(player.getUuid());
            if(user==null)return null;
            String value=user.getCachedData().getMetaData().getPrefix();
            if(value==null)return Component.empty();
            // Only administrators can define LuckPerms prefixes. Preserve MiniMessage styling.
            try{return MiniMessage.miniMessage().deserialize(value);}catch(IllegalArgumentException e){return Component.text(value);}
        }
    }
}
