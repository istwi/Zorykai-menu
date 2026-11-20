# Testes1.1.0 —20/11/2026

Resultado final: **30 testes JUnit e105 verificações de integração aprovadas**, sem testes JUnit ignorados. O build solicitado `./gradlew clean build` passou, juntamente com os JARs separados de integração e exemplo. O JAR de produção reconstruído é idêntico ao usado nos testes nativos.

## Unitários e configurações

Incluem cinco grupos de preço, maior permissão sem soma, produto sem desconto, exclusão por permissão, override e categoria, percentuais/teto inválidos, arredondamento por unidade/precisão, máximo com preço0 e sem capacidade, limites e preços inválidos. O máximo foi comparado a busca exaustiva em400 cenários determinísticos usando as cotações reais do PriceEngine.

Regressão dos510 produtos importados: IDs, preços, slots e componentes preservados. Conversão repetível e remoção autorizada de referência antiga intactas; persistência/exclusão do journal, retomada de backup, segurança dos componentes de texto, ownership de extensões e gravação de erro/stack trace passaram.

## Servidor real

Minecraft1.21.1, Java21.0.9+10, Fabric Loader0.17.2, FabricAPI0.116.6+1.21.1, Cobblemon1.8.3, Impactor5.3.5, LuckPerms5.4.140, Text Placeholder API2.4.2+1.21, Core0.18.0-dev e Dex0.7.0-dev.1. Laboratório em127.0.0.1 com perfil descartável. Outros mods da produção não foram instalados integralmente.

-42 verificações da atualização: cotações e débitos reaisVIP/VIP+/MVP/MVP+, ausência de soma, exclusão/override de produto, permissão removida antes da confirmação e depois do débito persistido; máximo50 com9000/VIP+, clique duplicado, stacks parciais, limite16 nativo, inventário cheio, limite de produto, preço0 e saldo insuficiente; mod separado abrindo menus/lojas, ação/placeholder/condição, eventos de compra/venda, cancelamento anterior e isolamento de observer com falha; JSON inválido conserva snapshot; logs gravados com stack trace.
-22 verificações Cobblemon/compatibilidade: equipe vazia e vagas, nome/nível em equipe presente, snapshots estáveis de leitura, item Cobblemon, shiny e consultas antigas, provedores reais Core/Dex, scoreboard, fallback de ícone sem alterar o ID, rejeição de item comercial inválido e validação dos menus migrados.
-36 verificações de regressão: tamanhos de inventário, abertura, navegação, paginação, confirmação, ações de inventário bloqueadas, compra/venda reais e quantidades, saldo insuficiente, inventário cheio, componentes, concorrência, permissões, revisão de preço, reload, Poké Balls e TM com componente nativo, preflight de moeda ausente, perda de resposta, atestado/recuperação, catálogo/espaço alterados e desconexão.
-5 verificações em um segundo servidor sem Cobblemon/LuckPerms: inicia com exemplos vanilla, snapshot indisponível, fallback N/D, condição fechada e preço integral. O Impactor inclui Text Placeholder API2.4.1 transitivamente; não foi declarado um teste de ausência absoluta desse componente aninhado.

Os dados individuais e logs estão em evidence/v1.1/. Parte das ações foi dirigida ao próprio handler do servidor com um jogador real conectado; não representa fuzzing exaustivo de pacotes de rede.

## Cliente real

Minecraft/Fabric com apenas FabricAPI e Cobblemon, sem ZorykaiMenus/addon/API no cliente. Capturas mostram preço original200, VIP+10%, unidade180, economia e saldo estimado. Cliques físicos em Comprar máximo selecionaram53 unidades para saldo9660; a confirmação real debitou9540 e entregou53 itens. As35/36 verificações de comércio não se limitam a uma simulação de carteira: usam o Impactor e arquivos de jogador reais.

## Falhas controladas

Para testar interrupções, o módulo de diagnóstico usa um gateway derivado que executa o débito/salvamento reais e retém a resposta enquanto aplica a mudança/desconexão. Outra injeção lança IOException depois da persistência real para testar resultado incerto. Isso controla o ponto da interrupção; não é um teste de queda física de energia ou de cada falha de rede possível.

Um primeiro teste de desconexão permitia que uma compra rápida terminasse antes de desconectar. Foi corrigido o harness para aguardar o salvamento e segurar a resposta; uma compra concluída antes do logout não deve ser estornada. O teste sem mods opcionais também teve seu entrypoint separado, pois o harness VIP requer LuckPerms. Esses problemas de teste não foram ocultados removendo as verificações.

## Reproduzir

```bash
python3 tools/prepare_dependencies.py
./gradlew clean build remapIntegrationJar remapExampleJar
```

Os comandos do harness exigem operador, `-Dzorykai.integrationTest=true` e loopback. zupgrade altera inventário/saldo/permissões de um perfil descartável; zcheck termina desconectando-o. Nunca usar em produção. Para ausência opcional, use tools/prepare_absence_checks.py para selecionar somente AbsenceChecks no JAR de diagnóstico.

Depois da validação, a embalagem dos JARs checks/example foi reduzida para não duplicar o SQLite aninhado do produto. Suas classes são idênticas às executadas nos testes; o JAR principal permaneceu byte a byte igual.

Não foram testados Windows, queda de energia, carga massiva, todos os plugins/mods externos, todos os pontos de falha do filesystem nem entrega de eventos exatamente uma vez entre crashes. O journal e a conciliação de respostas incertas continuam necessários. Erros intencionais aparecem nos logs como parte dos testes de falha.
