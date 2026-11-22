# Venda do PC — 1.3.1

`/zmenu pokemon` mantém o comando, mas passa a listar somente o PC do próprio jogador. A equipe não é oferecida para venda. São45 Pokémon por página, com anterior/próxima e confirmação individual. Ícones nativos `cobblemon:pokemon_model` preservam espécie, forma, aspectos e shiny; a confirmação também usa o modelo. Ícones são apenas visuais, não entregam itens ao jogador. O cliente precisa do Cobblemon, como já necessário para jogar; ZorykaiMenus continua somente no servidor.

## Preço preservado

A alteração por IVs/shiny foi cancelada pelo usuário. O cálculo original1.3.0 permanece integralmente, incluindo o `shinyMultiplier` que já existia no servidor. Nenhum bônus novo por IVs foi adicionado.

## Limite diário

Por UUID de jogador, dez vendas somadas de espécies Rare/Ultra-Rare por dia. Common/Uncommon não consomem essa cota. Usa a raridade direta da espécie e rotas diretas ativas do datapack, sem herdar a raridade do ancestral usada no preço econômico. Espécie sem raridade conhecida, quando vendável por preço manual, também entra na cota.

```json
{
  "rareDailyLimit": 10,
  "dailyTimezone": "America/Sao_Paulo"
}
```

Adicione esses campos ao JSON existente, preservando os demais. Os dois campos novos ausentes usam os padrões acima sem sobrescrever seu arquivo. `shinyMultiplier` já existia e continua com seu valor configurado. Use `/zmenu reload` após editar. A renovação é à meia-noite no fuso configurado; sair/reentrar, reiniciar ou recarregar não reseta a cota.

Só vendas concluídas consomem cota. Cancelamento, recusa de pagamento ou devolução não contam. Pagamento incerto permanece bloqueado no journal até recuperar/atestar; não se repete automaticamente. O dia da conclusão/atestado é o dia contabilizado. O histórico anterior à1.3.1 não tem classificação registrada e não é contado retroativamente.

## Atualização e recuperação

Pare o servidor, faça backup e substitua apenas o JAR ZorykaiMenus; remova duplicados. Preserve `config/zorykaimenus`, `internal/pending.sqlite`, mundo e arquivos Cobblemon. Não instale o JAR `checks`.

A venda revalida UUID, posse, local no PC, IVs/preço, revisão da configuração, cota e estado negociável antes da retirada. Snapshot completo é arquivado; gravação flat-file do PC é confirmada antes de pedir pagamento. Recusa conhecida devolve ao PC; se estiver cheio, libera uma vaga e usa `/zmenu recover`. Transações antigas da equipe mantêm recuperação no armazenamento original. Não se altera o mercado dinâmico de itens ou o sistema de descontos.

Backends suportados continuam os flat-file NBT/JSON nativos de Cobblemon1.8.3. Pagamentos externos e arquivos nativos não formam uma transação distribuída; estados incertos exigem conciliação como na1.3.0. Não apague o journal.


## Validação

51 testes unitários sem falhas, incluindo cota persistida/reabertura SQLite e separação de raridade direta/preço de evolução.16 verificações em servidor Fabric com Cobblemon/Impactor reais: modelos, confirmação/cancelamento, pagamento, UUID removido, equipe intacta, recusa com devolução integral ao PC, dono diferente, paginação, bloqueio da11ª rara e common liberado após cota. Para testar o limite, dez registros de sucesso foram preparados na base descartável; não foram dez capturas/vendas manuais. Jogadores simulados; sem validação visual em cliente gráfico ou repetição de reinício completo nesta alteração.

SHA256 do JAR: `f69debe00e58e3e62cd62034dfa1e5452825b77c9b8b1f0220b5b727daad0b33`
