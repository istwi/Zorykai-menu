# Venda de Pokémon — ZorykaiMenus 1.3.0

Venda dos Pokémon da **equipe do jogador para o servidor**, com pagamento pelo Impactor. Não é venda entre jogadores nem uma loja de compra de Pokémon. Minecraft 1.21.1 / Fabric / Java 21; integração conferida com Cobblemon 1.8.3 e Impactor 5.3.5.

## Instalar e usar

Com o servidor parado e backup do mundo/configuração, substitua o JAR anterior por `zorykaimenus-1.3.0.jar`. Mantenha apenas uma versão desse mod. Os IDs técnicos `zorykaimenus`, caminhos, lojas de itens, descontos e o mercado dinâmico da 1.2 são preservados. Não precisa instalar outro mod no cliente além dos mods de conteúdo habituais.

No jogo:

```mcfunction
/zmenu pokemon
```

Também aceita `/zmenu pokemon sell`. O menu lista os slots presentes da sua própria equipe, nome/nível/shiny, preço e motivo da avaliação. Selecione o Pokémon, revise a confirmação e clique em Confirmar venda. Cancelar ou fechar não vende. A confirmação é de uma única unidade, pelo UUID do Pokémon, não por um comando removendo o slot que estiver ocupado depois.

Só a equipe está habilitada nesta entrega. Um Pokémon no PC precisa ser movido para a equipe antes de abrir a venda. A busca no PC é usada para recuperação/checagem de duplicidade, não para selecionar Pokémon de outros jogadores.

## Preços: Magikarp comum = 250

A referência solicitada é **250 de dinheiro** para um Magikarp comum. A moeda é a que já está configurada em `economy.json`; não há saldo separado nem conversão entre moedas.

| Obtenção mais fácil | Multiplicador padrão | Valor de referência normal |
|---|---:|---:|
| Comum | 1 | 250 |
| Incomum | 3 | 750 |
| Raro | 10 | 2.500 |
| Ultra-raro | 30 | 7.500 |

A avaliação usa a pesquisa de 1.025 espécies do Cobblemon 1.8.3 (incluindo pesca), os vínculos de evolução e rotas do pool efetivamente carregado no servidor. É conservadora: uma rota comum/fácil prevalece sobre uma rota rara da mesma espécie. Pesos de spawn não são tratados como porcentagens universais de encontro.

- **Magikarp:** comum/pescável, 250.
- **Gyarados:** também há pesca comum, portanto o padrão continua 250; não ganha um preço de lendário por estar evoluído.
- **Feebas:** pesca comum condicionada, tratado como preço comum conservador, sem presumir que a dificuldade de localizar o slime chunk torna cada exemplar ultra-raro.
- Evoluções sem rota direta barata têm um teto derivado de um ancestral mais acessível, com fator de esforço padrão de 1,25 por etapa; uma rota direta ainda mais barata prevalece.
- Novas rotas mais fáceis de datapacks podem reduzir a avaliação. O CCC com as regras raras adicionadas pode fornecer a referência que faltava para lendários/míticos. A avaliação é por espécie: usa a rota mais barata entre formas, sem precificar uma forma como se fosse uma espécie separada.
- Sem rota conhecida, ancestral avaliável ou preço manual, a venda fica indisponível. A ausência de spawn não vira um preço alto inventado.

A base de pesquisa também é mantida como referência conservadora; remover uma rota fácil do servidor não eleva automaticamente o preço além dessa base. Para um servidor que quer reprecificar espécies/rotas específicas, configure `speciesPrices`. Depois de mudar datapacks, faça a recarga deles e `/zmenu reload` para atualizar a avaliação em cache.

Nível, IVs e EVs **não aumentam o preço** nesta configuração. Isso evita usar a própria área de farm de XP para transformar um Pokémon barato em uma venda cara. Shiny usa multiplicador padrão 2, configurável. Há teto de preço, arredondado para baixo na precisão da moeda do Impactor. O mercado dinâmico de vendas de itens continua separado; não aplica pressão/cotação de itens às vendas de Pokémon.

## Configurar por JSON

Novo arquivo, criado sem sobrescrever os existentes:

```text
config/zorykaimenus/pokemon-sales.json
```

```json
{
  "schemaVersion": 1,
  "enabled": true,
  "permission": "zorykaimenus.pokemon.sell",
  "referencePrice": 250,
  "bucketMultipliers": {"common": 1, "uncommon": 3, "rare": 10, "ultra-rare": 30},
  "shinyMultiplier": 2,
  "evolutionMultiplier": 1.25,
  "maxPrice": 100000,
  "keepParty": 1,
  "confirmationSeconds": 60,
  "speciesPrices": {},
  "blockedSpecies": []
}
```

Recarregar: `/zmenu reload`. Validar sem aplicar: `/zmenu validate`. Uma configuração inválida não substitui o catálogo válido em memória. Uma confirmação aberta antes da recarga precisa ser revista.

- `referencePrice`: muda a referência comum de toda a tabela.
- `bucketMultipliers`: fatores por grupo de obtenção.
- `shinyMultiplier`: bônus shiny; use 1 para não diferenciar.
- `evolutionMultiplier`: teto de esforço da evolução em relação a um ancestral.
- `maxPrice`: teto final por Pokémon.
- `speciesPrices`: preço normal manual por espécie, antes do fator shiny/teto. Exemplo: `"speciesPrices": {"magikarp": 250, "mewtwo": 12000}`.
- `blockedSpecies`: espécies que não podem ser vendidas. Exemplo: `["mew", "arceus"]`.
- `keepParty`: mínimo de Pokémon que deve permanecer na equipe; padrão 1, aceito 1 a 6.
- `confirmationSeconds`: validade de 10 a 300 segundos.
- `permission`: permissão LuckPerms configurável; por padrão permitida, seguindo a política das lojas de itens. Para bloquear um grupo: `lp group GRUPO permission set zorykaimenus.pokemon.sell false`.

## Abrir por menu ou NPC

Ação de botão do ZorykaiMenus:

```json
{"type": "OPEN_POKEMON_SELL"}
```

O exemplo completo está em `examples/pokemon-sales/menu-venda.json`; copie para `config/zorykaimenus/menus/` se quiser esse menu e recarregue. Ele abre com `/zmenu open venda_pokemon`. Não sobrescreva menus seus só para acrescentar o botão.

No diálogo do Shirokai/Zorykai-Trainers, use um botão de comando **como jogador**:

```json
{"id":"vender","label":"Vender Pokémon","action":"command","executor":"player","command":"zmenu pokemon"}
```

Há diálogo completo em `examples/pokemon-sales/comprador_pokemon.json`. O Trainers só abre o menu; a autorização e a venda são conferidas novamente pelo ZorykaiMenus. Não precisa atualizar o Trainers para esse comando.

## Regras de venda e recuperação

- Apenas o dono da equipe pode vender um UUID presente nela. Mover/substituir o Pokémon ou alterar sua avaliação antes de confirmar exige nova revisão.
- Não vende durante batalha, com Pokémon solto/tethered, nem Pokémon marcados como não negociáveis. Retire o item equipado antes de vender; nenhum item equipado é descartado automaticamente.
- Ovos reconhecidos pelos marcadores compatíveis são recusados. Representações de ovos ou armazenamento de outros addons precisam de verificação própria; não se assume compatibilidade com qualquer formato privado.
- Preserva pelo menos o mínimo de equipe configurado.
- Vendas de itens e Pokémon compartilham a exclusão de transação por jogador. Cliques repetidos não criam outra operação.
- A retirada é gravada e verificada no armazenamento nativo antes de iniciar o pagamento. O Pokémon completo é guardado como snapshot de recuperação no journal, com UUID, natureza, habilidades, movimentos, IVs/EVs e demais dados nativos.
- Pagamento recusado de forma confirmada devolve o mesmo UUID e dados. Se a equipe estiver cheia/offline, o retorno fica pendente; libere uma vaga e use `/zmenu recover`.
- Se o provedor perder a resposta após um possível crédito, o mod preserva a operação e **não repete o pagamento automaticamente**. Após conferir no Impactor, a staff usa o comando existente `/zmenu resolve <UUID-do-jogador> <ID-da-operação> applied|not_applied`. `applied` encerra como pago; `not_applied` permite recuperar o Pokémon. Essa é uma declaração administrativa e precisa corresponder à conferência real.
- No login, etapas recuperáveis são processadas; `/zmenu recover` permite nova tentativa manual. Não há tentativas a cada tick.

O journal fica em `config/zorykaimenus/internal/pending.sqlite`, usando o banco existente. A nova tabela de histórico `pokemon_sale_history` mantém snapshots e resultado final. **Não apague o banco enquanto houver operações pendentes.** O comando `/zmenu status` exibe a operação/etapa pendente.

A retirada/devolução durável é compatível com o armazenamento padrão **flat-file NBT/JSON** do Cobblemon 1.8.3; backend diferente é recusado antes da retirada. O caminho NBT foi ensaiado nesta entrega. A gravação local passa pelo executor do Cobblemon e a thread do servidor aguarda sua confirmação com limite de cinco segundos, evitando que transferências de equipe/PC interfiram nesse intervalo; isso pode causar latência se o disco estiver lento. O pagamento pelo Impactor continua assíncrono.

## Testes e código

Os resultados executados e suas limitações estão em [TESTING.md](TESTING.md). Não foi aplicado nada à produção. Para compilar: Java 21, `python3 tools/prepare_dependencies.py` e `./gradlew test build`. O perfil de espécies já está incluído. Para regenerá-lo a partir da pesquisa e do JAR oficial: `python3 tools/generate_pokemon_sale_profiles.py --cobblemon-jar /caminho/fabric-1.8.3+1.21.1.jar`.
