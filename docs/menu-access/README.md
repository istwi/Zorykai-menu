# ZorykaiMenus 1.3.5 — aliases, abertura confiável e prefixos

Baseada na branch mais recente `feature/pokemon-sales-limits`, versão 1.3.4. Preserva venda pelo PC com modelos nativos, cota diária, correção Windows, retorno após venda e abertura da venda de Pokémon apenas por comando de console confiável.

## Atualizar

Pare o servidor, substitua o JAR anterior por `zorykaimenus-1.3.5.jar` e reinicie. Mantenha uma única versão do mod e preserve configurações/mundo/journal. Não muda os IDs dos menus, lojas, Pokémon, permissões de visualização ou saldos.

## Jogador comum

Jogadores comuns não abrem arquivos arbitrários por `/zmenu open <id>` ou `/zmenu shop <id>`. `/zmenu` sem argumentos também não abre automaticamente o menu principal. Os aliases que você configurar continuam funcionando, assim como botões internos `OPEN_MENU`, `OPEN_SHOP`, Voltar e navegação entre páginas.

Botões antigos que executam `PLAYER_COMMAND` com `zmenu open/shop` também ficam sujeitos ao bloqueio. Para navegação configurada dentro do JSON, use `OPEN_MENU`/`OPEN_SHOP`; para NPC, use o comando de console com o jogador.

Os subcomandos de recuperação/status próprios continuam disponíveis. A venda de Pokémon continua na política da 1.3.4: comando direto do jogador e ação antiga de menu não abrem a venda; use a abertura confiável pelo console/NPC.

## Escolher aliases

Em `config/zorykaimenus/config.json`, integre o campo `aliases` aos demais campos existentes:

```json
"aliases": {
  "menu": {"menu": "main"},
  "loja": {"shop": "example"}
}
```

- `/menu` abre `menus/` com ID `main`.
- `/loja` abre `shops/` com ID `example`.

Troque os IDs pelos seus. Também continua aceitando `"aliases": ["meumenu"]` dentro do JSON de um menu. Um alias da configuração global tem precedência sobre o mesmo alias declarado num menu.

Para exigir permissão no próprio alias:

```json
"aliases": {
  "lojavip": {"shop": "example", "permission": "meusmenus.lojavip"}
}
```

Conceda `meusmenus.lojavip` pelo LuckPerms. Sem `permission`, o alias é público, respeitando as permissões/condições do menu ou loja de destino. Para limitar um menu por grupo, sua `permission` no JSON continua valendo.

**Novos nomes de aliases exigem reinício**, como nas versões anteriores. Mudar o destino ou remover um alias já registrado é verificado após `/zmenu reload`; uma entrada antiga não continua abrindo o alvo que foi removido da configuração. Conflitos com comandos de outros mods não são sobrescritos.

## Abrir para um jogador pelo console ou NPC

Menus da pasta `menus/`:

```text
zmenu open <id_do_menu> <jogador>
```

Lojas da pasta `shops/`:

```text
zmenu shop <id_da_loja> <jogador>
```

Exemplos no console:

```text
zmenu open main Dudz_cfrag
zmenu shop loja_pokebolas Dudz_cfrag
```

A forma com jogador exige fonte **sem entidade e nível 4**, como console real/RCON confiável ou comando de console do NPC. Um jogador não abre menu para outra pessoa por esses argumentos. Não use `execute as <jogador>`: isso transforma a fonte em jogador e não atende ao requisito de console.

O alvo continua com suas próprias permissões: a abertura por console **não concede OP nem ignora a permissão/condições do menu**. Para abrir um menu de uso normal somente pelo NPC, mantenha as permissões de visualização adequadas ao público; o bloqueio do comando genérico já limita a entrada.

No Shirokai/Zorykai Trainers, um NPC de comando pode usar:

```mcfunction
/traineradmin npc command <id_do_npc> console zmenu open main %player%
```

Para loja, troque por `console zmenu shop <id_da_loja> %player%`. Em botão de diálogo, use `"executor": "console"` e `"command": "zmenu open main %player%"`. A forma console do Trainers foi conferida no código; este ensaio chamou o dispatcher de console diretamente, sem clique físico em NPC.

Para venda de Pokémon, preserve o comando específico:

```text
zmenu pokemon open <jogador>
```

## Administradores

A permissão **`zorykaimenus.command.open`**, ou OP nível 4, permite abrir para si por `/zmenu`, `/zmenu open <id>` e `/zmenu shop <id>`. Não conceda esse nó ao grupo padrão se quiser permitir somente aliases.

As permissões antigas `zorykaimenus.use`, `zorykaimenus.open` e `zorykaimenus.shop` controlam o uso/visualização, mas não liberam o novo acesso genérico administrativo. Se você havia negado esses nós para bloquear comandos, essa negação também bloqueia a visualização por alias/console; use o novo nó `zorykaimenus.command.open` para controlar somente o comando genérico. Wildcards que incluam o novo nó também o concedem. A abertura com argumento de outro jogador permanece exclusiva da fonte de console.

## Cargo/prefixo mostrando `<bold>`

O placeholder nativo `%luckperms:prefix%` agora trata prefixos administrativos que misturam MiniMessage com códigos legados `&`/`§`, incluindo hexadecimal `&#RRGGBB` e `§x§R§R§G§G§B§B`. Antes, a presença de códigos `§` podia fazer o leitor cair no texto literal, deixando `<bold>` aparecer no cargo.

Foram verificados prefixos como `§4§lOWNER<bold>`: o texto renderizado conserva vermelho/negrito e não mostra a tag. MiniMessage puro continua funcionando. A interpretação de formatação é feita no valor confiável definido no LuckPerms; nomes de jogadores e outros placeholders não são concatenados e reinterpretados como marcação.

## Validação executada

- **55 testes JUnit**, zero falhas, incluindo mistura de formatos, hexadecimal, MiniMessage existente e proteção dos demais placeholders.
- **27 verificações Fabric locais** com Cobblemon 1.8.3, Impactor 5.3.5, LuckPerms 5.4.140 e jogadores simulados. Comandos genéricos negados; aliases global/local, navegação interna, loja por alias, permissões do destino, abertura por console, ausência de OP, abertura administrativa explícita e revogação, aliases alterados/removidos após reload, recuperação própria e comando exclusivo de Pokémon. Prefixo misto foi aplicado no LuckPerms real e conferido como componente com cor/negrito.
- **21 verificações de regressão de venda pelo PC**, incluindo modelos, venda/pagamento, retorno ao menu, devolução, paginação, cota diária, espécies comuns após a cota e restrição da abertura pelo console.

Relatórios JSON e hash do JAR acompanham esta pasta. O mesmo JAR foi usado nos dois servidores locais descartáveis. Não houve renderização em cliente gráfico nem alteração em produção; não se afirma que a configuração específica do servidor foi ensaiada. A fixture é separada e não está no JAR instalável.
