> Atualização final: a referência antiga a `loja_blocos` foi removida por confirmação do administrador. `/tentavip open <nome>` era o comando genérico antigo; `/zmenu shop <nome>` é seu equivalente e não exige loja fixa. Esses itens não são pendências.

# Revisão de compatibilidade autorizada — ZorykaiMenus 1.0.0

Esta revisão amplia o suporte aos recursos efetivamente usados nos arquivos fornecidos, conforme pedido de continuação. Não implementa descontos VIP, editor visual, pesquisa ou histórico consultável.

## Provedores identificados no GitHub informado

- [Shirokai Core — repositório Shirokai-tab](https://github.com/istwi/Shirokai-tab): versão publicada0.18.0-dev. Exporta `shirokai:cash`, `shirokai:tokens` e `shirokai:dex_percent` pela Text Placeholder API. Moedas e coleção são configuradas no próprio Core; o ZorykaiMenus não presume saldo ou calcula uma Pokédex paralela.
- [Shirokai Dex](https://github.com/istwi/Shirokai-Dex): publicação0.7.0-dev.1+mc1.21.1-cobblemon1.8.3. Publica `percent`, `registered`, `seen`, `total` e `remaining` por coleção.
- [Text Placeholder API](https://github.com/Patbox/TextPlaceholderAPI/releases/tag/2.4.2%2B1.21): versão2.4.2+1.21. API pública conferida no JAR real. Instale no servidor para as integrações externas; não precisa instalar no cliente.

O adaptador verifica registro, contexto e resultado válido. Valores e formatações cruzam como componentes de texto, sem concatenação insegura de JSON ou execução de comandos. Nada é registrado sobre os IDs de outros provedores. Se um provedor faltar, falhar ou não tiver dados, o mod não inventa saldo, porcentagem ou registro. O provedor pode retornar seu próprio `N/D`; uma expressão não resolvida pode permanecer visível para diagnóstico.

O alias antigo `%shirokaidex:progress <coleção>%` tem tratamento explícito: consulta os provedores reais `registered` e `total` e apresenta `registrados/total`, somente se ambos forem números válidos. Um provedor real de `progress`, se existir, tem precedência. As coleções national, special e shiny permanecem argumentos da configuração; sua definição continua no Dex.

Cargo usa a API do LuckPerms, dólares usam o saldo Impactor consultado para o menu e `%player:objective gyms%` lê o objetivo real do scoreboard. Objetivos inexistentes não são criados pelo mod. Os menus são atualizados a cada20 ticks para acompanhar o carregamento assíncrono dos provedores e as condições da equipe. A confirmação financeira continua consultando o Impactor e recalculando no servidor.

## Expressões Molang fornecidas

Suporte direcionado às expressões realmente encontradas nos quatro menus de exemplo:

- `q.gui.player.tell('test');`, convertido em mensagem de abertura.
- Consulta da equipe, `get_pokemon(0)` até `get_pokemon(5)` e `is_shiny`, com saída0 ou1.
- Prioridades distintas por slot: o botão de menor número prevalece quando sua condição é satisfeita; o outro fornece o visual alternativo.

A conversão produz `partyShinySlot`, `shinyExpected` e `priority`. O estado é consultado nas APIs reais do Cobblemon1.8.3. Slots vazios ou API ausente falham de forma fechada. A condição também é conferida no clique; produtos condicionais são conferidos novamente na transação.

**Não é um interpretador Molang geral.** Expressões diferentes ou código adicional não são executados; continuam pendentes/desativados e são informados na migração. Não se usa código nem engine do SkiesGUIs.

## Ícones de mods ausentes

Quando o item comercial existe, mas o ícone configurado não pode ser decodificado, a migração preserva `icon.id` e registra um `icon.fallback` explícito de `minecraft:paper`. A descrição mostra o ID original indisponível. Os19 botões que dependiam de Cobbleverse Badges/Mega Showdown permanecem acessíveis com esse visual temporário.

Se o mod original estiver instalado e o ID funcionar, o ícone original volta a ser utilizado. Essa regra não cria itens, não muda IDs comerciais e não permite vender/comprar um produto inexistente. O relatório lista `temporaryIconFallbacks`. Não é uma reprodução das texturas originais dos mods ausentes.

## Limites que ainda dependem de configuração

- A referência antiga a `loja_blocos` é removida da conversão; nenhuma loja ou preço é criado.
- `base_shop: example_categories` é um valor de exemplo antigo. Use `/zmenu shop <nome>` no lugar de `/tentavip open <nome>`.
- Comandos de outros mods, como kits, warps e ginásios, continuam dependências desses mods. O ZorykaiMenus preserva a ação e suas permissões.
- Conflitos de alias continuam preservando o comando já existente. O Core também pode fornecer menus próprios; use `/zmenu open menu` e `/zmenu open pokedex` para abrir explicitamente os menus migrados.

Não foi alterada a configuração ou o conteúdo de qualquer repositório remoto.
