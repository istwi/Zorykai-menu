package com.zorykai.menus;
import com.zorykai.menus.api.ExtensionRegistry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExtensionRegistryTest {
 @Test void duplicateIdsAndLifetimeOwnership()throws Exception{var registry=new ExtensionRegistry<Object>();Object a=new Object(),b=new Object();var old=registry.register("example:action",a);assertThrows(IllegalArgumentException.class,()->registry.register("example:action",b));old.close();var current=registry.register("example:action",b);old.close();assertSame(b,registry.get("example:action"));current.close();assertFalse(registry.contains("example:action"));}
 @Test void closingOldHandleCannotRemoveSameHandlerReregistered()throws Exception{var registry=new ExtensionRegistry<Object>();var handler=new Object();var old=registry.register("example:shared",handler);old.close();var current=registry.register("example:shared",handler);old.close();assertTrue(registry.contains("example:shared"));current.close();}
 @Test void malformedIdentifiersAndNullHandlersRejected(){var r=new ExtensionRegistry<Object>();assertThrows(Exception.class,()->r.register("NO-NAMESPACE",new Object()));assertThrows(Exception.class,()->r.register("EXAMPLE:action",new Object()));assertThrows(Exception.class,()->r.register("example:a",null));}
}
