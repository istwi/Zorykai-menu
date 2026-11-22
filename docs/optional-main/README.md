# ZorykaiMenus 1.3.6 — menu principal opcional e prioridade de aliases

Atualização da 1.3.5 para Minecraft 1.21.1/Fabric. Mantém o bloqueio dos comandos genéricos para jogadores, a abertura confiável por console/NPC, a correção do prefixo LuckPerms e as funções da venda de Pokémon pelo PC.

## Atualizar e remover os exemplos

Pare o servidor, substitua o JAR anterior por `zorykaimenus-1.3.6.jar`, mantenha apenas uma versão e reinicie. Preserve seus menus/configurações, dados de jogadores e journal.

Agora o mod:

- Inicia e aceita reload com a pasta de menus vazia.
- Não exige `mainMenu`; vazio, ausente, `null` ou um ID antigo já apagado são tratados como ausência de menu principal.
- Não gera nem recria `menus/main.json`, `menus/second.json` ou `shops/example.json`, inclusive depois de reiniciar. Os JSONs básicos de configuração/economia/permissões continuam sendo criados quando faltam.
- Não apaga ou reescreve menus que você já tem.

Se as versões antigas recriaram `main.json` e `second.json`, você pode apagá-los novamente. Na 1.3.6 eles não voltam. Integre em `config/zorykaimenus/config.json`:

```json
"mainMenu": ""
```

Sem principal, o comando administrativo `/zmenu` informa que não há principal e a ação Voltar sem histórico fecha o menu. Aliases e console continuam funcionando. Se quiser um principal, configure o ID de um menu existente.

**Referências entre botões continuam sendo validadas.** Se você mantiver algum JSON com `OPEN_MENU` apontando para `second` ou outro menu apagado, remova/troque essa ação ou apague o arquivo de exemplo que não usa. A mudança não ignora links quebrados dos seus menus nem remove botões automaticamente.

## Prioridade para /help, /kit e /warps

Mantenha o campo `aliases` que já aponta para seus menus e acrescente:

```json
"aliasOverrides": ["help", "kit", "warps"]
```

Exemplo de trecho completo, substituindo os IDs `ajuda`, `kits` e `destinos` pelos seus:

```json
{
  "mainMenu": "",
  "aliases": {
    "help": {"menu": "ajuda"},
    "kit": {"menu": "kits"},
    "warps": {"menu": "destinos"}
  },
  "aliasOverrides": ["help", "kit", "warps"]
}
```

Esse trecho deve ser integrado ao config existente, preservando os outros campos. A lista também se aplica aos aliases declarados dentro dos próprios JSONs de menu.

Alternativa por alias:

```json
"aliases": {
  "kit": {"menu": "kits", "override": true}
}
```

Só os nomes explicitamente escolhidos podem substituir comandos. Sem prioridade, um conflito mantém o comando original e é informado no log. Sem menu/loja de destino válido, o comando original não é removido. `zmenu` é reservado e não pode ser sobrescrito.

**Reinicie para registrar novos aliases ou mudar a prioridade**, inclusive para desfazer uma substituição. `/zmenu reload` recarrega arquivos e destinos dos aliases existentes; não restaura o comando original no dispatcher quando você retira a prioridade durante a execução.

Quando um nome é assumido, ele passa a abrir o menu e seus subcomandos antigos deixam de fazer parte desse nome. Por exemplo, `/kit give` não é combinado com o novo `/kit` de menu. O acesso pelo nome original sem namespace, inclusive pelo console, é substituído; nomes alternativos/namespaced que o mod original já registrou continuam separados, como `minecraft:help` ou um alias próprio do mod de kits. A disponibilidade desses nomes depende do mod original. Não se substituem comandos de outros nomes fora da lista.

O alias não concede OP ou permissões extras. As permissões/condições do destino continuam valendo; se um jogador não pode ver o menu, assumir o nome não libera esse acesso.

## Console/NPC e venda de Pokémon

Continuam funcionando:

```text
zmenu open <id_do_menu> <jogador>
zmenu shop <id_da_loja> <jogador>
zmenu pokemon open <jogador>
```

`open` usa a pasta `menus/`; `shop` usa `shops/`. As formas com jogador exigem console confiável, sem entidade e nível 4. O comando da venda de Pokémon mantém a restrição da 1.3.4. O nó administrativo dos comandos genéricos continua sendo `zorykaimenus.command.open`; jogadores comuns usam os aliases liberados.

## Validação

59 testes JUnit aprovados, incluindo configuração sem principal, criação apenas dos arquivos essenciais, ausência de recriação de exemplos, prioridade explícita, proteção de `zmenu` e substituição completa do comando sem mesclar subcomandos antigos. Os testes antigos de prefixos/negócios permanecem incluídos.

No Fabric local, foram executados os cenários de inicialização com zero menus/lojas, reload com principal antigo apagado, principal vazio, Voltar/raiz sem principal, conflito sem prioridade, tomada explícita de `/help`, `/kit` e `/warps`, subcomando antigo bloqueado, nome alternativo preservado, comando não escolhido intacto, bloqueio do acesso genérico, console, permissões e exclusão dos arquivos. A regressão de venda pelo PC é registrada separadamente. Consulte os relatórios JSON e `verification.json` para as contagens e hashes efetivos.

Os ensaios usam jogadores simulados em servidores locais descartáveis, não cliente gráfico ou produção. O conflito `/help` usa o comando disponível nesse servidor; `kit` e `warps` foram registrados pela fixture para simular comandos de outros mods. Não houve teste com todos os mods de kits/warps existentes nem medição sob carga de muitos jogadores. A fixture não está no JAR instalável.
