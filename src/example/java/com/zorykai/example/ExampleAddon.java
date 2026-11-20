package com.zorykai.example;
import com.zorykai.menus.api.ZorykaiMenusAPI;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import java.util.concurrent.atomic.AtomicInteger;
/** Separate example mod, never embedded in the production JAR. */
public final class ExampleAddon implements ModInitializer {
 public static final AtomicInteger ACTIONS=new AtomicInteger(),PURCHASES=new AtomicInteger(),SALES=new AtomicInteger();
 @Override public void onInitialize(){
  if(!FabricLoader.getInstance().isModLoaded("zorykaimenus"))return;
  ZorykaiMenusAPI.registerAction("zorykai_example:message","zorykai.example.action",context->{ACTIONS.incrementAndGet();context.player().sendMessage(Text.literal("Olá pela API pública!"),false);});
  ZorykaiMenusAPI.registerPlaceholder("zorykai_example:greeting",(player,argument)->Text.literal("Olá, "+player.getGameProfile().getName()));
  ZorykaiMenusAPI.registerCondition("zorykai_example:allowed",(player,json)->!com.google.gson.JsonParser.parseString(json).getAsJsonObject().has("deny"));
  ZorykaiMenusAPI.beforePurchase(trade->!trade.productId().equals("cancelled"));
  ZorykaiMenusAPI.afterPurchase(trade->PURCHASES.incrementAndGet());
  ZorykaiMenusAPI.afterSale(trade->SALES.incrementAndGet());
  CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(CommandManager.literal("zexample").requires(source->source.hasPermissionLevel(4))
   .then(CommandManager.literal("menu").executes(c->ZorykaiMenusAPI.openMenu(c.getSource().getPlayerOrThrow(),"main")?1:0))
   .then(CommandManager.literal("shop").executes(c->ZorykaiMenusAPI.openShop(c.getSource().getPlayerOrThrow(),"example")?1:0))));
 }
}
