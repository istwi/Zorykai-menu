package com.zorykai.menus.config;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import com.zorykai.menus.util.ItemsJson;
import static com.zorykai.menus.config.Json.*;
public final class Catalog {
 public record Snapshot(long revision,JsonObject config,JsonObject economy,JsonObject permissions,JsonObject messages,JsonObject discounts,Map<String,JsonObject> menus,Map<String,JsonObject> shops){}
 private final Path root;private Snapshot current;private long revision;
 public Catalog(Path root){this.root=root;}
 public Snapshot get(){if(current==null)throw new IllegalStateException("Configuração não carregada");return current;}
 public static void seed(Path root)throws Exception{
  for(String name:List.of("config.json","economy.json","discounts.json","permissions.json","messages/pt_br.json","menus/main.json","menus/second.json","shops/example.json")){
   Path dst=root.resolve(name);Files.createDirectories(dst.getParent());if(Files.exists(dst))continue;
   try(var in=Catalog.class.getResourceAsStream("/defaults/"+name)){if(in==null)throw new IllegalStateException(name);Files.copy(in,dst);}
  }
 }
 private JsonObject read(Path p)throws Exception{try(var r=Files.newBufferedReader(p)){return JsonParser.parseReader(r).getAsJsonObject();}catch(Exception e){throw new IllegalArgumentException(p+": JSON: "+e.getMessage(),e);}}
 public List<String> reload(MinecraftServer server,boolean apply){
  List<String> errors=new ArrayList<>();Map<String,JsonObject> menus=load("menus",errors),shops=load("shops",errors);
  JsonObject cfg=new JsonObject(),eco=new JsonObject(),perms=new JsonObject(),msgs=new JsonObject(),discounts=new JsonObject();
  java.math.BigDecimal discountCap=new java.math.BigDecimal("50");
  try{discounts=read(root.resolve("discounts.json"));com.zorykai.menus.pricing.Discounts.validate(discounts);discountCap=discounts.has("maxDiscountPercent")?discounts.get("maxDiscountPercent").getAsBigDecimal():discountCap;cfg=read(root.resolve("config.json"));eco=read(root.resolve("economy.json"));perms=read(root.resolve("permissions.json"));msgs=read(root.resolve("messages/pt_br.json"));
   if(!str(eco,"currency","impactor:dollars").matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))errors.add("economy.json.currency: identificador inválido");
   if(num(cfg,"maxQuantity",2304)<1||num(cfg,"maxQuantity",2304)>1000000)errors.add("config.json.maxQuantity: intervalo 1..1000000");
  }catch(Exception e){errors.add("Configuração/discounts.json: "+e.getMessage());}
  validate(server,menus,shops,errors,discountCap);
  if(!menus.containsKey(str(cfg,"mainMenu","main")))errors.add("config.json.mainMenu: menu inexistente");
  if(errors.isEmpty()&&apply)current=new Snapshot(++revision,cfg,eco,perms,msgs,discounts,Map.copyOf(menus),Map.copyOf(shops));
  for(String error:errors)com.zorykai.menus.ZorykaiMenus.LOG.error("Validação de configuração: {}",error);
  return errors;
 }
 private Map<String,JsonObject> load(String dir,List<String> errors){
  Map<String,JsonObject> out=new TreeMap<>();try(var files=Files.walk(root.resolve(dir))){for(Path p:files.filter(v->v.toString().endsWith(".json")).sorted().toList())try{
   JsonObject j=read(p);String id=str(j,"id",p.getFileName().toString().replaceFirst("\\.json$",""));
   if(!bool(j,"enabled",true))continue;
   if(!id.matches("[a-zA-Z0-9_.-]+"))throw new IllegalArgumentException("id: somente letras, números, _, . e -");
   if(out.putIfAbsent(id,j)!=null)throw new IllegalArgumentException("id duplicado: "+id);
  }catch(Exception e){errors.add(p+": "+e.getMessage());}}catch(Exception e){errors.add(root.resolve(dir)+": "+e.getMessage());}return out;
 }
 private void validate(MinecraftServer server,Map<String,JsonObject> menus,Map<String,JsonObject> shops,List<String> errors,java.math.BigDecimal discountCap){
  for(var group:List.of(menus,shops))for(var entry:group.entrySet()){
   String path=(group==menus?"menus/":"shops/")+entry.getKey()+".json";JsonObject j=entry.getValue();
   try{
    com.zorykai.menus.pricing.Discounts.validateRule(obj(j,"discount"),discountCap,"discount");
    if(group==shops)com.zorykai.menus.market.MarketPolicy.validate(obj(j,"market"));
    com.zorykai.menus.gui.Conditions.validate(j,server);
    int size=num(j,"size",54),pages=num(j,"pages",1);if(size<9||size>54||size%9!=0)throw new IllegalArgumentException("size: deve ser 9,18,27,36,45 ou54");if(pages<1||pages>1000)throw new IllegalArgumentException("pages: intervalo1..1000");
    Map<String,JsonObject> used=new HashMap<>();Set<String> ids=new HashSet<>();
    for(String kind:List.of("buttons","products"))for(var el:arr(j,kind)){
     var b=el.getAsJsonObject();String field=kind+"["+str(b,"id","?")+"]";
     try{
      if(b.has("partyShinySlot")&&(num(b,"partyShinySlot",-1)<0||num(b,"partyShinySlot",-1)>5))throw new IllegalArgumentException("partyShinySlot: intervalo0..5");
      com.zorykai.menus.pricing.Discounts.validateRule(obj(b,"discount"),discountCap,field+".discount");
      if(b.has("maxQuantity")&&(num(b,"maxQuantity",0)<1||num(b,"maxQuantity",0)>1000000))throw new IllegalArgumentException("maxQuantity: intervalo1..1000000");
      com.zorykai.menus.gui.Conditions.validate(b,server);
      String id=str(b,"id","");if(id.isBlank()||!ids.add(id))throw new IllegalArgumentException("id vazio ou duplicado: "+id);
      ItemsJson.readIcon(server,obj(b,"icon"));if(kind.equals("products")){
       var stack=ItemsJson.read(server,obj(b,"item"));if(stack.getCount()!=1)throw new IllegalArgumentException("item.count: produto unitário precisa ser1");
       com.zorykai.menus.market.MarketPolicy.validate(obj(b,"market"));
       if(!b.has("buy")&&!b.has("sell"))throw new IllegalArgumentException("buy/sell: preço ausente");
       for(String price:List.of("buy","sell"))if(b.has(price)){var x=b.get(price).getAsBigDecimal();if(x.signum()<0||x.precision()>30||Math.max(0,x.scale())>8)throw new IllegalArgumentException(price+": preço inválido");}
      }
      List<Integer> bp=pages(b,pages);for(int page:bp)for(var sl:arr(b,"slots")){int s=sl.getAsInt();if(s<0||s>=size)throw new IllegalArgumentException("slots: fora do inventário");var previous=used.putIfAbsent(page+":"+s,b);if(previous!=null&&(!kind.equals("buttons")||!previous.has("priority")||!b.has("priority")||num(previous,"priority",0)==num(b,"priority",0)))throw new IllegalArgumentException("slots: sobreposição sem prioridades distintas página"+page+" slot"+s);}
      for(var action:arr(b,"actions"))validateAction(action.getAsJsonObject(),menus,shops);
     }catch(Exception e){errors.add(path+"."+field+": "+e.getMessage());}
    }
    for(String kind:List.of("openActions","closeActions"))for(var a:arr(j,kind))validateAction(a.getAsJsonObject(),menus,shops);
   }catch(Exception e){errors.add(path+": "+e.getMessage());}
  }
 }
 public static List<Integer> pages(JsonObject b,int max){List<Integer> out=new ArrayList<>();if(!b.has("pages")){for(int i=1;i<=max;i++)out.add(i);}else for(var p:arr(b,"pages")){int v=p.getAsInt();if(v<1||v>max)throw new IllegalArgumentException("pages: página fora do intervalo");out.add(v);}return out;}
 private void validateAction(JsonObject a,Map<String,JsonObject> menus,Map<String,JsonObject> shops){
  String t=str(a,"type","");if(!com.zorykai.menus.api.ZorykaiMenusAPI.actionRegistered(t)&&!Set.of("OPEN_MENU","OPEN_SHOP","PLAYER_COMMAND","SERVER_COMMAND","CLOSE_MENU","BACK","NEXT_PAGE","PREVIOUS_PAGE","SEND_MESSAGE","PLAY_SOUND").contains(t))throw new IllegalArgumentException("actions.type desconhecido: "+t);
  if(t.equals("OPEN_MENU")&&!menus.containsKey(str(a,"id","")))throw new IllegalArgumentException("actions.id: menu inexistente "+str(a,"id",""));
  if(t.equals("OPEN_SHOP")&&!shops.containsKey(str(a,"id","")))throw new IllegalArgumentException("actions.id: loja inexistente "+str(a,"id",""));
  if(t.equals("SERVER_COMMAND")&&str(a,"permission","").isBlank())throw new IllegalArgumentException("SERVER_COMMAND exige permission explícita");
  for(String cmd:strings(a,"commands"))if(cmd.contains("\n")||cmd.contains("\r")||cmd.indexOf(0)>=0)throw new IllegalArgumentException("commands: caractere de controle proibido");
 }
}
