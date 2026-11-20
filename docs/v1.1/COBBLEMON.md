# Integração opcional Cobblemon

Adaptador de leitura validado contra Cobblemon1.8.3+1.21.1. Não adiciona dependência obrigatória de compilação/instalação do Cobblemon. Usa APIs públicas de storage, PartyStore.get(int), Pokemon.getShiny/getLevel/getDisplayName(boolean), conferidas no artefato instalado. Não usa setters em produção nem executa comandos administrativos para modificar Pokémon.

Configuração opcional em config.json:

```json
{"cobblemon":{"enabled":true,"fallback":"N/D"}}
```

Arquivos1.0 sem esse campo usam os mesmos padrões. O fallback é texto literal, sem injeção de formatação. Sem Cobblemon, com integração desligada ou API indisponível, condições falham de forma fechada e placeholders conhecidos usam o fallback.

## Placeholders

- `%cobblemon:party_count%`: slots ocupados da equipe.
- `%cobblemon:party_free%`:6 menos os slots ocupados.
- `%cobblemon:first_name%`: nome exibido do primeiro Pokémon presente na ordem da equipe.
- `%cobblemon:first_level%`: nível desse Pokémon.

Equipe vazia: count0, free6 e fallback no nome/nível. Integração ausente não representa uma equipe vazia conhecida: os placeholders usam o fallback. A leitura só ocorre na thread do servidor, em até seis slots de memória; não faz consultas pesadas ou IO de rede.

## Condições JSON

Podem ser usadas na raiz de menu/loja ou em botão/produto. A lista tem semântica AND e é verificada ao abrir, renderizar, clicar e executar a transação.

```json
{"conditions":[{"type":"cobblemon:has_pokemon"}]}
```

Tipos disponíveis:

- `cobblemon:has_pokemon`.
- `cobblemon:min_party`, com `min: 0..6` (padrão1).
- `cobblemon:party_space`, com `min: 0..6` (padrão1).
- `cobblemon:has_item`, com `item: {"id":"cobblemon:poke_ball"}` e `quantity: 1` ou mais. Exige ID realmente registrado e componentes compatíveis. A quantidade fica no campo quantity, não em item.count.

O campo legado `partyShinySlot` e as prioridades dos menus migrados continuam funcionando. Slots vazios não satisfazem consultas shiny/não-shiny sobre um Pokémon inexistente.

Itens de lojas continuam usando IDs/componentes reais. Para carregar um catálogo com itens Cobblemon, o mod que registra esses itens precisa estar instalado; ausência opcional não transforma itens inexistentes em itens vanilla. Um servidor com apenas exemplos vanilla inicia sem Cobblemon/LuckPerms.

## Ações de integração

OPEN_MENU e OPEN_SHOP já permitem abrir menus condicionais e de recompensas. Outros mods podem registrar ações próprias na API1.1. Essas ações podem chamar suas APIs de eventos/missões com permissão explícita; o ZorykaiMenus não implementa sistemas completos de missões, recompensas diárias ou gestão de Pokémon.
