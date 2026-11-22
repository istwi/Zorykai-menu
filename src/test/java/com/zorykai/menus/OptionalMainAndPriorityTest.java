package com.zorykai.menus;
import com.google.gson.*;
import com.zorykai.menus.config.*;
import com.zorykai.menus.commands.CommandPriority;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static com.zorykai.menus.config.Json.*;
import static org.junit.jupiter.api.Assertions.*;
class OptionalMainAndPriorityTest {
 @Test void emptyNullAbsentAndDeletedMainAreOptional(){for(String cfg:List.of("{}","{\"mainMenu\":null}","{\"mainMenu\":\"\"}","{\"mainMenu\":\"main\"}"))assertNull(MenuConfiguration.main(parse(cfg),Map.of()));assertEquals("custom",MenuConfiguration.main(parse("{\"mainMenu\":\"custom\"}"),Map.of("custom",new JsonObject())));}
 @Test void seedCreatesOnlyEssentialConfigAndNeverRecreatesDeletedExamples()throws Exception{Path root=Files.createTempDirectory("menus-seed");Catalog.seed(root);assertTrue(Files.exists(root.resolve("config.json")));assertTrue(Files.isDirectory(root.resolve("menus")));assertFalse(Files.exists(root.resolve("menus/main.json")));Files.writeString(root.resolve("menus/main.json"),"custom");Catalog.seed(root);assertEquals("custom",Files.readString(root.resolve("menus/main.json")));Files.delete(root.resolve("menus/main.json"));Catalog.seed(root);assertFalse(Files.exists(root.resolve("menus/main.json")));assertFalse(Files.exists(root.resolve("menus/second.json")));assertFalse(Files.exists(root.resolve("shops/example.json")));}
 @Test void priorityIsExplicitAndReservedManagementCommandIsProtected(){MenuConfiguration.validate(parse("{\"aliasOverrides\":[\"help\",\"kit\",\"warps\"]}"));assertTrue(MenuConfiguration.override(parse("{\"aliasOverrides\":[\"kit\"]}"),"kit",new JsonObject()));assertFalse(MenuConfiguration.override(new JsonObject(),"kit",new JsonObject()));assertTrue(MenuConfiguration.override(new JsonObject(),"kit",parse("{\"override\":true}")));assertThrows(IllegalArgumentException.class,()->MenuConfiguration.validate(parse("{\"aliasOverrides\":[\"zmenu\"]}")));assertThrows(IllegalArgumentException.class,()->MenuConfiguration.validate(parse("{\"aliasOverrides\":[\"kit\",\"kit\"]}")));}
 @Test void replacementDoesNotMergeOldSubcommandsAndPreservesOtherRoots()throws Exception{
  var d=new CommandDispatcher<Object>();var old=d.register(LiteralArgumentBuilder.<Object>literal("kit").executes(c->1).then(LiteralArgumentBuilder.<Object>literal("give").executes(c->2)));d.register(LiteralArgumentBuilder.<Object>literal("other").executes(c->3));d.register(LiteralArgumentBuilder.<Object>literal("plugin:kit").executes(c->1).redirect(old));
  var replacement=LiteralArgumentBuilder.<Object>literal("kit").executes(c->4).build();CommandPriority.replace(d.getRoot(),"kit",replacement);
  assertEquals(4,d.execute("kit",new Object()));assertThrows(Exception.class,()->d.execute("kit give",new Object()));assertEquals(3,d.execute("other",new Object()));assertEquals(1,d.execute("plugin:kit",new Object()));assertSame(replacement,d.getRoot().getChild("kit"));assertEquals(1,d.getCompletionSuggestions(d.parse("ki",new Object())).join().getList().size());
 }
}
