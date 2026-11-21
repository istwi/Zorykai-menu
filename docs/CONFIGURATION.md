# Configuração

A versão 1.2 acrescenta o objeto opcional `market` em lojas/produtos para preços dinâmicos de venda. Veja [bolsa de valores](market/README.md). Os formatos anteriores permanecem válidos sem esse campo.

Arquivos: `config.json`, `economy.json`, `permissions.json`, `messages/pt_br.json`, `menus/*.json`, `shops/*.json`. Subpastas são lidas recursivamente. Os exemplos completos acompanham `src/main/resources/defaults/`.

Menu mínimo:

```json
{
  "id": "central",
  "size": 27,
  "pages": 1,
  "title": "<white><bold>✦ ZORYKAI</bold></white> <aqua>Central",
  "aliases": ["central"],
  "buttons": [{
    "id": "loja",
    "slots": [13],
    "icon": {"id": "minecraft:emerald"},
    "name": "<aqua>Loja",
    "lore": ["<gray>Clique para consultar."],
    "actions": [{"type": "OPEN_SHOP", "id": "example"}]
  }]
}
```

`size`: 9/18/27/36/45/54; `pages`: número de páginas. Slots começam em zero. `buttons[].pages: [1,2]` limita a exibição; ausência significa todas as páginas. Sobreposições na mesma página são rejeitadas. IDs precisam ser únicos dentro do arquivo. `enabled: false` desativa o arquivo inteiro. `permission` é opcional em menu/loja/botão/produto/ação; quando preenchida, o jogador precisa dessa permissão.

Loja usa os mesmos campos e uma lista `products`:

```json
{
  "id": "materiais",
  "size": 27,
  "title": "<aqua>Materiais",
  "products": [{
    "id": "stone",
    "slots": [13],
    "item": {"id": "minecraft:stone"},
    "icon": {"id": "minecraft:stone"},
    "name": "<white>Pedra",
    "buy": "2.50",
    "sell": "1.25"
  }],
  "buttons": []
}
```

Ausência de `buy` ou `sell` desativa essa direção. Preços são por unidade, decimais exatos, sem separador de milhar. `item.count` deve ser1; `icon.count` pode variar dentro do limite nativo. `components` é um objeto de componentes **nativos da versão instalada**, decodificado pelo ItemStack.CODEC. O registro do servidor decide se item e componentes existem. Não há conversão automática de IDs inválidos. A aparência do ícone não muda o item entregue.

`icon.viewerHead: true` é uma conveniência para `minecraft:player_head` com o perfil do jogador. `priceLoreProvided: true` evita acrescentar as linhas automáticas de preço quando o importador já preservou o template antigo. Não use esse campo para anunciar preço diferente do configurado: a confirmação sempre mostra o valor real.

## Ações

Lista `actions`, executada na ordem. `click: LEFT`, `RIGHT` ou `ANY` opcional. `openActions` e `closeActions` pertencem ao menu/loja; não configure ações de abertura/fechamento que abram o próprio menu em ciclo.

| type | Campos adicionais |
|---|---|
| OPEN_MENU / OPEN_SHOP | `id` |
| PLAYER_COMMAND | `commands: ["/spawn"]` |
| SERVER_COMMAND | `commands: ["..."]`, `permission` explícita; console habilitado no config |
| CLOSE_MENU / BACK / NEXT_PAGE / PREVIOUS_PAGE | Nenhum |
| SEND_MESSAGE | `message` MiniMessage |
| PLAY_SOUND | `sound`, ID do registro de sons |

Sons de sucesso: `buySound` e `sellSound` em `messages/pt_br.json`. Placeholders básicos: `{player}`, `{uuid}` e `{balance}` em texto. Saldo pode aparecer inicialmente como indisponível enquanto a consulta assíncrona termina. Ações de comando não usam saldo para decisões. Placeholders `%...%` de serviços externos são preservados sem interpretação.

`config.json.aliases` pode ligar comandos antigos a um destino: `{"loja":{"shop":"materiais"}}`. Conflitos são registrados e o comando existente é preservado. Aliases são registrados no início; reinicie após modificá-los. O principal é `mainMenu`; `maxQuantity` limita a seleção (padrão2304); `allowServerCommands` inicia falso.

O reload valida todos os arquivos e referências antes de trocar o snapshot. Menus já abertos com revisão antiga precisam ser reabertos. Uma compra debitada cuja revisão mudou é estornada em vez de usar o preço antigo.
