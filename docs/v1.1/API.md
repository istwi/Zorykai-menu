# API pública1.1

Pacote: `com.zorykai.menus.api.ZorykaiMenusAPI`. Usa tipos Minecraft, Java e DTOs imutáveis; não expõe contas Impactor nem o JSON mutável do catálogo. O código completo de um addon compilável está em `src/example/`, com metadados de um mod Fabric separado.

## Dependência e presença

No projeto Fabric/Loom do consumidor, coloque o JAR1.1 em libs e adicione:

```groovy
modCompileOnly files('libs/zorykaimenus-1.1.0.jar')
```

Para um addon obrigatório, declare `zorykaimenus: ">=1.1.0"` em depends do fabric.mod.json, como no exemplo. Para integração opcional, consulte `FabricLoader.getInstance().isModLoaded("zorykaimenus")` e só então carregue uma classe adaptadora separada que referencia a API. Não coloque tipos opcionais em campos/superclasses do entrypoint que precisa iniciar sem o mod.

Registros podem ser feitos em ModInitializer, antes de o servidor ficar pronto. Chamadas que acessam servidor/jogador/catálogo/preço exigem a thread principal e `ready()`. Antes de pronto ou fora da thread, a API lança IllegalStateException. Para trabalhos assíncronos, use `server.execute(...)` e resolva novamente o jogador conectado pelo UUID. Nenhum método bloqueia para carregar saldo.

## Menus, produtos e preços

```java
boolean opened = ZorykaiMenusAPI.openMenu(player, "main");
boolean shopOpened = ZorykaiMenusAPI.openShop(player, "example");
boolean exists = ZorykaiMenusAPI.menuExists("main");
boolean shopExists = ZorykaiMenusAPI.shopExists("example");
var product = ZorykaiMenusAPI.product("example", "stone");
var price = ZorykaiMenusAPI.effectivePrice(player, "example", "stone",
    ZorykaiMenusAPI.Direction.BUY, 16);
var discount = ZorykaiMenusAPI.discount(player, "example", "stone");
```

open retorna false se indisponível/sem permissão/desconectado. effectivePrice valida acesso, quantidade e disponibilidade; lança IllegalArgumentException em dados inválidos, e pode lançar erros de precisão/provedor monetário. A consulta não realiza compra. Product contém IDs, categoria, Optional de preços-base e itemJson como string independente. Price contém unidade original, percentual, unidade efetiva, quantidade, originalTotal, total e savings. BigDecimal e records são imutáveis.

## Registros

```java
var action = ZorykaiMenusAPI.registerAction("example:message", "example.action.use",
    context -> context.player().sendMessage(Text.literal("Olá"), false));
var placeholder = ZorykaiMenusAPI.registerPlaceholder("example:hello",
    (player, argument) -> Text.literal("Olá, " + player.getGameProfile().getName()));
var condition = ZorykaiMenusAPI.registerCondition("example:allowed",
    (player, definitionJson) -> player.experienceLevel >= 1);
```

Esses trechos pressupõem imports de ZorykaiMenusAPI e net.minecraft.text.Text. O arquivo ExampleAddon.java é o exemplo completo compilado pelo build.

IDs precisam usar o namespace do addon. minecraft, cobblemon e zorykaimenus ficam reservados. IDs duplicados são recusados. O AutoCloseable retornado remove apenas aquele registro; fechar novamente não remove uma substituição posterior. Registre antes da validação de configurações que usam sua extensão. Falha de placeholder preserva valor não resolvido; falha de condição retorna false.

Uso JSON:

```json
{
  "name":"%example:hello%",
  "conditions":[{"type":"example:allowed"}],
  "actions":[{"type":"example:message"}]
}
```

A permissão declarada no registro de ação é obrigatória e revalidada mesmo quando o JSON não a repete. Também valem as verificações de menu, botão e condições. ActionContext inclui jogador, menuId, indicador shop, página, botão do mouse e definitionJson como string. Um addon que modifica seus próprios dados deve validar o payload; o núcleo não concede privilégios administrativos ao seu código.

## Eventos

```java
var beforeBuy = ZorykaiMenusAPI.beforePurchase(trade -> true);
var beforeSell = ZorykaiMenusAPI.beforeSale(trade -> !trade.productId().equals("blocked"));
var afterBuy = ZorykaiMenusAPI.afterPurchase(trade -> logger.info("Compra {}", trade.transactionId()));
var afterSell = ZorykaiMenusAPI.afterSale(trade -> logger.info("Venda {}", trade.transactionId()));
```

Trade contém UUID de transação/jogador, loja, produto, direção, quantidade, unidade/total originais, percentual de desconto, unidade/total efetivos e moeda. Não contém objeto interno mutável da transação.

Antes: chamado na thread do servidor depois de reservar a operação do jogador, antes de movimentar dinheiro ou itens. Retornar false cancela com segurança. Uma exceção de listener anterior também cancela. Depois: somente após resultado monetário e inventário confirmados e journal concluído. Cancelamentos, estornos e respostas incertas não emitem sucesso. Um listener posterior com falha é isolado e registrado nos logs, sem desfazer ou repetir a compra; outros listeners continuam.

Os métodos de emissão pertencem ao núcleo e não fazem parte da fachada pública. O addon só registra listeners. Fechar os handles remove o listener; uma iteração já em execução pode concluir sua cópia atual.

Não há garantia de entrega exatamente uma vez de eventos entre crashes. Uma queda entre conclusão durável e notificação pode perder um evento; integrações com efeitos financeiros próprios devem usar transactionId como chave idempotente e manter suas próprias garantias. Não é permitido alterar preço/quantidade do evento para modificar a compra.

## Compilar o exemplo

```bash
./gradlew compileExampleJava remapExampleJar
```

`zorykaimenus-1.1.0-example.jar` é um mod separado, não embutido no produto. Registra demonstrações, contadores e cancela o produto de teste chamado cancelled; use apenas em laboratório ou depois de revisar esse comportamento. Os comandos /zexample menu e /zexample shop exigem operador. O JAR principal não contém o entrypoint do exemplo nem os testes de integração.

Callbacks são síncronos na thread do servidor. Mantenha-os curtos, sem IO pesado nem espera por futuros; prepare dados em cache e retorne ao servidor com server.execute quando necessário.
