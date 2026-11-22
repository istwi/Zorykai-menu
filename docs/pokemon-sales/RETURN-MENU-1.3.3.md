# Retorno ao menu — 1.3.3

Após uma venda confirmada e paga pelo menu, a lista do PC reabre automaticamente com os Pokémon e a cota diária atualizados. Volta à página em que o jogador estava; caso ela deixe de existir após a venda, abre a última página disponível. Cancelar a confirmação também volta à página anterior.

O menu só reabre após a conclusão da operação. Não substitui outro inventário aberto durante a espera, nem abre para uma nova conexão do jogador. Pagamento incerto não reabre a loja: continua usando a recuperação existente. Chamadas diretas da API/recuperação não provocam abertura inesperada.

Mantidas a correção Windows1.3.2, regras de preço, limite de vendas e configurações. Pare o servidor, substitua somente o JAR ZorykaiMenus e reinicie. Preserve `config/zorykaimenus`, mundo e journal.

Validação: 51 testes unitários e18 verificações Fabric com Cobblemon/Impactor reais e jogadores simulados, incluindo reabertura da lista após pagamento e remoção do modelo vendido. Sem cliente gráfico. SHA256: `9076e98a4c3dadcef219360bb33e0a0632def062f1d34b8e1ab32e2478ff6c6f`.
