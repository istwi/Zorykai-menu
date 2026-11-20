package com.zorykai.menus.migration;
import java.util.regex.Pattern;
import java.util.OptionalInt;
import java.util.Optional;
/** Compatibility compiler for the expressions present in the supplied files, not a general script engine. */
public final class LegacyExpressions {
 private static final Pattern SHINY=Pattern.compile("t\\.party\\s*=\\s*q\\.gui\\.player\\.party\\s*;\\s*t\\.pokemon\\s*=\\s*t\\.party\\.get_pokemon\\(\\s*([0-5])\\s*\\)\\s*;\\s*t\\.pokemon\\.is_shiny\\s*;?");
 private static final Pattern TELL=Pattern.compile("q\\.gui\\.player\\.tell\\('([^'\\\\]*)'\\)\\s*;?");
 private LegacyExpressions(){}
 public static OptionalInt shinySlot(String script){var m=SHINY.matcher(script.trim());return m.matches()?OptionalInt.of(Integer.parseInt(m.group(1))):OptionalInt.empty();}
 public static Optional<String> message(String script){var m=TELL.matcher(script.trim());return m.matches()?Optional.of(m.group(1)):Optional.empty();}
}
