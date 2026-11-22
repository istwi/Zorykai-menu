# Venda aberta pelo NPC — 1.3.4

O jogador não abre mais a venda diretamente por `/zmenu pokemon` ou `/zmenu pokemon sell`; recebe orientação para procurar o NPC. A ação antiga `OPEN_POKEMON_SELL` de menus JSON também não abre a venda, evitando acesso pelo menu genérico.

Novo comando para console confiável:

```text
zmenu pokemon open <nome_do_jogador>
```

Exige fonte sem entidade e nível4. Jogador comum ou OP não executa diretamente o subcomando. O console real também pode abrir a interface para um jogador; o servidor não diferencia um clique de NPC de outro emissor que já tenha acesso ao console. Não conceda execução de console a jogadores.

## Shirokai Trainers

A versão do Trainers examinada já executa comandos de NPC no console, com nível4 e sem conceder OP ao jogador. Para o NPC existente, configure:

```text
/traineradmin npc command <id_do_npc> console zmenu pokemon open %player%
```

Substitua `<id_do_npc>` pelo ID cadastrado; `%player%` fica literal na configuração. Essa sintaxe e placeholder foram conferidos no código/README do Trainers. Não foi repetido um clique físico em NPC no ensaio desta alteração; o comando de console foi testado diretamente no dispatcher real.

A permissão `zorykaimenus.pokemon.sell` continua controlando quem pode negociar dentro do menu, com padrão liberado; não dá acesso ao novo comando de console. O retorno automático após venda, modelos, limite diário, preços e correção de Windows continuam iguais. `/zmenu recover` permanece disponível para resolver pendências sem depender de NPC.

Pare o servidor, substitua o JAR e reinicie, preservando mundo/configurações/journal. Atualize o comando do NPC para a forma de console acima.

Validação: 51 testes unitários e21 verificações Fabric com Cobblemon/Impactor reais e jogadores simulados. Confirmados bloqueio dos dois comandos de jogador e da chamada privilegiada, abertura pelo console, venda e retorno ao menu. SHA256: `e4925270320c7a2f0bd34c7bca8ae0caaa271d916a09540cef82cbb26bd3fa0a`.
