# Validação executada — venda de Pokémon 1.3.0

Minecraft 1.21.1 / Fabric Loader 0.17.2 / Java 21 / Cobblemon 1.8.3 / Impactor 5.3.5, em runtimes descartáveis ligados a 127.0.0.1. As verificações de menus/armazenamento usam jogadores simulados do lado do servidor, com inventários, storage Cobblemon e carteira Impactor reais. Não são capturas de cliente gráfico nem testes de produção.

## Resultados

- **48 testes JUnit:** incluem os testes antigos de preços, comércio/journal, mercado, API e importação, mais sete testes de avaliação de Pokémon e histórico transacional. Zero falhas/erros.
- **22 verificações nativas de venda:** referência de 250, menu real de equipe e confirmação/cancelamento no inventário real, crédito no Impactor, ausência do UUID vendido na equipe/PC, preservação do outro Pokémon, repetição de UUID, resposta de crédito atrasada, bloqueio da transação concorrente, pagamento recusado com restauração integral de dados, resposta perdida após crédito real, recuperação sem repetir crédito, atestados administrativos aplicado/não aplicado, item equipado, não negociável, dono diferente, última unidade da equipe, confirmação com configuração antiga, gravação de equipe e inventário de itens inalterado.
- **7 verificações após reinício real do processo do servidor:** mesmo mundo, arquivos e carteira. UUID vendido continua ausente; saldo persiste; recover de operação concluída não repete dinheiro. Também foram montados estados persistidos de intenção de retirada e retorno para exercitar a retomada desses estágios, com repetição idempotente e preservação de nível/shiny/JSON completo.
- **32 verificações nativas de regressão do mercado de itens 1.2:** compras/vendas, pressão/cotação compartilhada, congelamento de confirmação, recuperação, concorrência, preço fixo, recusa financeira, API/listener, reset, recarga inválida e logs. Zero falhas.
- **5 verificações sem Cobblemon/LuckPerms:** inicialização normal, consulta opcional indisponível, placeholder/condição seguros e preço normal de itens. A nova classe de venda foi inicializada nesse ambiente sem ligação obrigatória às classes Cobblemon. O Impactor fornece bibliotecas opcionais transitivas, portanto não se afirma ausência absoluta de Placeholder API aninhada.
- **25 verificações com datapacks:** repetição dos 22 cenários de venda com o CCC modificado carregado e três verificações adicionais: rota comum adicionada para Beldum aparece no pool ativo; avaliação cai a 250; Arceus recebe a referência ultra-rara de 7.500 pela rota do CCC. O comando de abertura também foi usado nesse ensaio.

São **91 verificações nativas executadas nos cinco ensaios**, incluindo a repetição da suíte de venda com datapacks, e não 91 funcionalidades distintas. Relatórios JSON desta pasta contêm os nomes e resultados. O mesmo JAR de produção, com hash em `verification.json`, foi usado em todos os processos, inclusive no reinício. A fixture de teste é um JAR separado e não está no produto distribuído.

## Limites

Foi testado o backend NBT flat-file nativo. O código também aceita o adaptador JSON flat-file, mas o ensaio nativo não foi repetido com esse formato; backends de banco de dados/outros addons são recusados na checagem de capacidade. Não houve simulação de queda de energia ou medição de latência sob carga concorrente de muitos jogadores. A perda de resposta do provedor foi injetada depois de um depósito real salvo no Impactor, sem repetir o crédito automaticamente.

O reinício foi limpo. As etapas de intenção/retorno foram construídas como registros persistidos para testar a recuperação; isso não equivale a matar o processo em cada instrução possível. Não houve teste gráfico, teste de todos os formatos privados de ovos/addons ou integração completa com todos os mods do servidor do usuário. Não se vende diretamente do PC, não se transfere Pokémon entre jogadores e não se altera o mercado de itens para os Pokémon.

## Compilar e reproduzir

```sh
python3 tools/prepare_dependencies.py
./gradlew test build remapIntegrationJar
```

`src/integration/java/.../PokemonSaleChecks.java` e `PokemonRestartChecks.java` contêm os ensaios, habilitados apenas pelas propriedades JVM `zorykai.pokemonTest` e `zorykai.pokemonRestart`. A fixture existente também inclui outros ensaios opcionais; para rodar isoladamente, selecione só o entrypoint correspondente no `fabric.mod.json` do JAR de checks, em um servidor local descartável. Nunca instale o JAR de checks em produção. O ensaio com rotas extras usa `zorykai.liveDatapackTest=true`, o CCC testado e uma rota de teste comum para Beldum.
