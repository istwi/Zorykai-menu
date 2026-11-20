# Operações e recuperação

O Impactor fornece contas, `balance`, `withdraw`, `deposit`, resultados de transação e `save`; não oferece aqui um comprovante idempotente compartilhado com o inventário Minecraft. Portanto, o mod não promete uma transação distribuída atômica.

Compra: validar catálogo, revisão, quantidade, permissões e capacidade; persistir intenção; debitar e salvar a conta Impactor; registrar confirmação; revalidar revisão/permissões/capacidade; registrar inventário anterior/posterior; entregar e salvar jogador; conferir NBT salvo e sincronizar o arquivo; remover pendência. Se a entrega for inviável após débito confirmado, tentar estorno com intenção persistida antes do crédito.

Venda: validar componentes e quantidade; registrar inventário anterior/posterior; remover e salvar itens; registrar confirmação da remoção; persistir intenção de crédito; depositar e salvar conta; remover pendência. Crédito recusado com resultado comprovadamente negativo devolve itens; se não couberem ou jogador sair, ficam em custódia na pendência até recuperação. Exceção sem resultado confirmado não devolve automaticamente itens, pois o crédito pode ter ocorrido.

Não há saldo local alternativo. Preço unitário × quantidade usa BigDecimal, e a precisão aceita é conferida com `Currency.decimals()` do Impactor. Retornos negativos e exceções são tratados de formas diferentes. Uma exceção pode significar resposta perdida depois de uma alteração no saldo.

## Uso administrativo

1. Veja `/zmenu status` do jogador e o log, que informa ID e etapa da operação. O operador pode consultar o banco interno para diagnóstico; não o altere para forçar desbloqueio.
2. `/zmenu recover` recupera somente etapas comprovadas. Inventário anterior e posterior precisam coincidir exatamente com o estado atual quando houver entrega/remoção interrompida. Divergência exige conferência; o mod não sobrescreve um inventário diferente.
3. Em `DEBIT_INTENT`, `CREDIT_INTENT` ou `REFUND_INTENT`, confira o resultado no provedor econômico e seus registros. Não conclua apenas comparando o saldo atual: outras movimentações podem ter ocorrido.
4. Somente depois dessa conferência, use `/zmenu resolve <uuid> <id> applied` se a operação monetária daquele estágio ocorreu, ou `not_applied` se comprovadamente não ocorreu. O comando registra o atestado e libera o próximo passo seguro. Peça ao jogador para executar `/zmenu recover` quando necessário.
5. Se não for possível comprovar o resultado, mantenha a operação pendente e investigue. Não execute um segundo débito/crédito por tentativa.

Um débito conhecido recuperado devolve dinheiro, evitando uma entrega inesperada depois de reinício. Uma intenção de entrega/remoção conhecida pode ser concluída se o inventário corresponder a um dos snapshots. O journal usa SQLite WAL com `synchronous=FULL`, exclusão mútua por UUID e uma operação pendente por jogador.

O salvamento do inventário usa a rotina nativa do PlayerManager, lê o arquivo `.dat` produzido, compara `Inventory` e força o arquivo regular. Não tenta abrir diretórios como FileChannel. Continua sujeito às garantias reais do sistema de arquivos, do provedor econômico e dos demais mods. Interferência administrativa ou de outro mod no inventário durante recuperação pode exigir análise manual.

O journal interno é parte da segurança financeira, não o histórico consultável planejado para uma versão futura. Não contém ferramentas automáticas de backup/restauração. Backups de migração são feitos exclusivamente no comando explícito de migração, conforme solicitado.
