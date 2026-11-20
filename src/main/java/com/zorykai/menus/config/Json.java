package com.zorykai.menus.config;
import com.google.gson.*;
import java.util.*;
public final class Json {
 public static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
 public static String str(JsonObject j,String k,String d){return j.has(k)?j.get(k).getAsString():d;}
 public static int num(JsonObject j,String k,int d){return j.has(k)?j.get(k).getAsBigDecimal().intValueExact():d;}
 public static boolean bool(JsonObject j,String k,boolean d){return j.has(k)?j.get(k).getAsBoolean():d;}
 public static JsonObject obj(JsonObject j,String k){return j.has(k)?j.getAsJsonObject(k):new JsonObject();}
 public static JsonArray arr(JsonObject j,String k){return j.has(k)?j.getAsJsonArray(k):new JsonArray();}
 public static List<String> strings(JsonObject j,String k){var out=new ArrayList<String>();for(var e:arr(j,k))out.add(e.getAsString());return out;}
 public static JsonObject parse(String s){return JsonParser.parseString(s).getAsJsonObject();}
}
