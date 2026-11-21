# Pesquisa de spawns e raridade para venda de Pokémon

Base: **Cobblemon Fabric 1.8.3+1.21.1**, distribuição oficial cujo SHA-512 foi conferido contra os metadados da versão publicada. Esta entrega é **pesquisa e proposta econômica**; não adiciona ainda compra/venda de Pokémon ao ZorykaiMenus. O JAR 1.2 continua negociando itens, não aceita automaticamente esses CSVs como lojas.

## Arquivos

- [Planilha com filtros](pesquisa-spawns-1.8.3.xlsx): resumo, rotas de pesca, rotas completas e pesos de grupos.
- [Resumo dos 1.025 registros de espécies](data/pokemon-resumo.csv): grupos observados, métodos, captura e restrições. Separador `;`, UTF-8 com BOM para Excel.
- [Rotas completas](data/rotas-spawn.csv): **8.716 registros normalizados** de Pokémon/membros de grupos, com pesos e condições; não são 8.716 tentativas independentes.
- [Rotas de pesca](data/rotas-pesca.csv).
- [Lista por grupo observado](lista-por-grupo.md).
- [Metadados, hashes e contagens](data/metadata.json) · [validação da extração](data/validation.json).

Foram lidos **1.591 arquivos de `spawn_pool_world`**, incluindo grupos/herds e Alphas. Há 1.025 registros de espécies: 905 com `implemented: true`, dos quais 876 têm alguma rota nesse pool e 29 não têm. Os outros 120 omitem `implemented: true`; no código oficial o padrão é `false`. Isso não é uma pesquisa dos datapacks adicionais do seu servidor.

## O que significa spawn rate

Há três informações diferentes:

1. **Frequência das tentativas:** o config padrão inspecionado usa `ticksBetweenSpawnAttempts = 20` (aproximadamente um segundo a 20 TPS), `maximumSpawnsPerPass = 8` e `pokemonPerChunk = 1.0`. São controles de tentativa/densidade; não significam um Pokémon por segundo ou oito Pokémon garantidos. São configuráveis e não determinam a chance de uma espécie sozinhos.
2. **Peso do grupo de raridade:** o JAR contém os pesos abaixo. Somam 100 em cada coluna, mas eventos/influências e pools disponíveis podem modificar a seleção.
3. **Peso da entrada dentro do grupo:** compete com as outras entradas elegíveis naquele local/contexto, incluindo modificadores, presets, grupos e entradas vazias. **`weight: 90` não significa 90% de chance.**

| Grupo | Mundo — peso padrão | Pesca — peso padrão |
| --- | ---: | ---: |
| `common` | 94,00 | 83,25 |
| `uncommon` | 5,00 | 11,25 |
| `rare` | 0,50 | 4,125 |
| `ultra-rare` | 0,20 | 1,375 |
| `boss` | 0,30 | Não listado |

A coluna `boss` é do mecanismo nativo de grupos/Alphas do Cobblemon, não do mod Shirokai/Zorykai Raids. Grupos podem gerar membros seguidores sem `alpha=true`; não transforme cada membro da planilha em uma tentativa independente ou some seu peso ao peso do grupo.

Não existe aqui uma porcentagem universal de encontrar cada espécie. Para isso, seria necessário fixar mundo, bioma, posição, altura, estrutura, horário, clima, pesca/isca/encantamentos, influência de Poké Snacks/habitats, concorrentes e datapacks. O arquivo `best-spawner-config.json` **embutido no JAR** foi usado para os pesos efetivos de configuração, em vez de tomar só os inicializadores da classe Kotlin (que apresentam alguns valores diferentes).

## Exemplos relevantes para preço

| Pokémon | Dados conferidos | Interpretação para venda |
| --- | --- | --- |
| Magikarp | Pesca `common`, peso 90; catch rate base 255. | Referência de menor preço: amplamente pescável e fácil de capturar. |
| Gyarados | Pesca `common`, peso 10; catch rate 45; também vem de Magikarp no nível 20. | Acima do Magikarp, mas não preço de raridade lendária. |
| Pidgey / Zubat | Rotas `common`, incluindo grupos. | Faixa barata; condições concretas continuam na planilha. |
| Feebas | Pesca `common`, pesos 57/95, exige slime chunk e exclui costa/oceano; Lure altera o peso. Catch rate 255. | Não tratar automaticamente como ultra-raro: difícil localizar a área, mas farmável depois. |
| Pikachu | `uncommon`, pesos 21,82/7,27 em rotas individuais. | Intermediário, sujeito ao contexto. |
| Ralts / Abra | Têm rotas `rare`, mas também `common` com condições de mansão/presets. | Revisar a rota farmável antes de atribuir preço alto. |
| Dratini | Rotas `rare`/`uncommon`; entrada `common`, peso 4.500, referencia `#cobblemon:spawns/is_ocean_ruins`, tag não definida no JAR analisado. | Validar no servidor antes de considerar essa rota comum ativa; não afirmar farm garantido. |
| Gible / Bagon / Larvitar | Rotas `rare` e `ultra-rare`, variando com ambiente. | Faixa alta, com revisão das rotas alternativas. |
| Beldum | Ultra-raro em contextos do Overworld, raro no End; há rotas de addons, incluindo uma incomum no Aether. Catch rate 3. | Faixa alta e captura difícil; não confundir uma rota do Aether com disponibilidade vanilla. |
| Bulbasaur / Charmander / Squirtle | Rotas normais em `ultra-rare`, além de participações em grupos nativos. | Faixa alta/ultra; considerar iniciais gratuitos e reprodução. |
| Axew / Dreepy | Rotas normais `ultra-rare`; Dreepy inclui pesca. | Faixa alta, sem presumir chance idêntica entre biomas/métodos. |
| Mewtwo / Rayquaza e vários lendários | Nenhuma rota em `spawn_pool_world` na distribuição analisada. | Preço manual conforme o mod/evento que realmente os fornece. |

`catchRate` é um parâmetro bruto da captura, **não uma porcentagem**. Formas, Poké Balls, HP e status podem modificar a dificuldade real; o resumo usa o valor base da espécie. Os pesos acima também não permitem comparar diretamente Pokémon que não concorrem no mesmo contexto.

## Como foi classificada a lista

As colunas `bucket` são fatos do pool. `categoria_observada` é um **agrupamento de pesquisa**, não uma raridade oficial única nem um preço aprovado:

- Considera a rota normal mais comum encontrada, mantendo o detalhe das outras rotas.
- Separa referências explicitamente dependentes de biomas/estruturas/blocos de outros mods ou gates de instalação. Isso não resolve recursivamente todos os biome tags nem prova que um bioma existe no mundo do servidor.
- `comum_condicionado` sinaliza que todas as rotas comuns consideradas têm requisitos como slime chunk, estrutura, Poké Snack, isca/encantamento ou presets adicionais. Horário/altura/clima também constam para revisão, mas sozinhos não fazem essa marcação.
- Referências obrigatórias a tags próprias não definidas no JAR são sinalizadas e excluídas do mínimo `core` observado; datapacks podem fornecer essas tags depois. **871 registros normalizados** têm esse tipo de requisito. As 16 tags identificadas estão nos metadados; elas não foram tratadas como prova de spawn ativo.
- O grupo `boss` e seus seguidores ficam em colunas separadas da classificação normal.
- `sem_rota_neste_pool` **não significa impossível de obter**: pode haver evolução, fóssil, interação, recompensa ou addon. Rotom e os fósseis de Galar são exemplos que exigem examinar outro mecanismo.

Contagens por categoria estão nos metadados. Não calculamos “média de raridade” somando pesos de entradas mutuamente exclusivas.

## Proposta de preços — ainda não aplicada

Recomendo usar o valor do Magikarp como unidade **M** e ajustar os outros preços por acesso real, dificuldade de captura e possibilidade de farm:

| Faixa proposta | Multiplicador inicial para revisão |
| --- | ---: |
| Magikarp comum | 1× M |
| Comuns fáceis | 1× a 3× M |
| Condicionados/incomuns | 3× a 8× M |
| Raros | 10× a 25× M |
| Ultra-raros | 25× a 60× M |
| Lendários/eventos/sem rota ou não implementados | Sem preço automático; configuração individual |

Esses multiplicadores são **sugestões econômicas**, não extraídos do Cobblemon. Exceções devem prevalecer: Feebas não deve receber o maior preço só pela reputação; Dratini exige revisão das rotas e tags; Gyarados não deve render um salto enorme por evoluir Magikarp.

No arquivo de loja fornecido anteriormente (`migration-input/skiesshop/shops/loja_pokebolas.json`), a Poké Ball custa 325. Esse é um registro antigo, não uma leitura do servidor em produção. O valor M deve ser escolhido considerando o custo atual de captura e a renda desejada; não foi fixado neste levantamento.

Para uma loja em que **jogadores vendem ao servidor**, também é preciso definir o tratamento de Pokémon reproduzidos, iniciais, recompensas, trocas, shiny/Alpha e IVs alterados por cristais. Raridade de spawn não mede o custo de reprodução nem comprova a origem de um Pokémon já existente. A bolsa dinâmica e limites de venda podem ajudar no volume, mas não substituem essa decisão. Esta pesquisa não remove nenhum Pokémon nem movimenta saldo.

## Fontes e reprodução

Fonte principal: [Cobblemon Fabric 1.8.3 oficial](https://modrinth.com/mod/cobblemon/version/IgUZQKm8), [JAR publicado](https://cdn.modrinth.com/data/MdwFAVRL/versions/IgUZQKm8/Cobblemon-fabric-1.8.3%2B1.21.1.jar). O hash verificado está em `metadata.json`.

Referências oficiais de implementação consultadas: [BestSpawnerConfig.kt](https://gitlab.com/cable-mc/cobblemon/-/blob/1.8.3/common/src/main/kotlin/com/cobblemon/mod/common/api/spawning/preset/BestSpawnerConfig.kt), [Spawner.kt](https://gitlab.com/cable-mc/cobblemon/-/blob/1.8.3/common/src/main/kotlin/com/cobblemon/mod/common/api/spawning/spawner/Spawner.kt), [Species.kt](https://gitlab.com/cable-mc/cobblemon/-/blob/1.8.3/common/src/main/kotlin/com/cobblemon/mod/common/pokemon/Species.kt) e [CobblemonConfig.kt](https://gitlab.com/cable-mc/cobblemon/-/blob/1.8.3/common/src/main/kotlin/com/cobblemon/mod/common/config/CobblemonConfig.kt). O fonte consultado é o cache oficial do commit `ffe005511dd0d3ba68377fa3f95906b3837d9a0e`; não foi afirmada equivalência bit a bit entre seu build e o JAR.

Os caminhos exatos dos recursos consultados acompanham cada linha. A distribuição declara MPL-2.0. Não foi redistribuído o JAR, nem incorporadas classes, algoritmos ou sprites a um mod: os CSVs são uma extração de fatos para análise, com atribuição, e o analisador é original. Não foram utilizados Daycare+ ou Cobbreeding como fonte.

```sh
python3 tools/analyze_pokemon_spawns.py --jar /caminho/Cobblemon-fabric-1.8.3+1.21.1.jar
```

O script exige o hash da distribuição oficial verificada. São 13 verificações de extração, não testes de probabilidade por simulação. Outros addons/configs de spawn do servidor podem mudar a conclusão. Para a implementação da loja, a tabela deve ser ajustada ao conjunto real de datapacks e regras do servidor.
