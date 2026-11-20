# Fases1.1

Fase1: descontos integrados ao fluxo existente, configuração discounts.json atômica, permissões explícitas, unidade arredondada na precisão real da moeda e prévia conferida novamente antes do débito. Build/testes passaram em /tmp/zorykai11-phase1.log. Nenhum preço-base foi alterado. Uma compra debitada cuja permissão/desconto mudou antes da entrega usa o estorno existente.

Fase2: seleção máxima usa a mesma função de cotação, busca binária limitada, capacidade de stacks e limite do produto. Não finaliza compra. Retorno assíncrono confere a tela/revisão e confirmações revalidam tudo. Teste comparativo com400 cenários determinísticos de saldo/desconto/arredondamento, além de casos zero/limite/sem espaço.

Fase2 compilada/testada em /tmp/zorykai11-phase2.log. A capacidade é obtida dos stacks reais e seus componentes; o cálculo máximo usa cotação idêntica ao preço de confirmação.

Fase3 acrescenta adaptador Cobblemon somente de leitura, condições e placeholders. APIs getParty(ServerPlayerEntity), PartyStore.get(int), Pokemon.getShiny/getLevel/getDisplayName(boolean) conferidas com javap no Cobblemon1.8.3 real. Sem dependência de compilação ou inicialização obrigatória do Cobblemon. Dados indisponíveis usam fallback configurável. Condições de raiz/botão/produto são verificadas também no clique e na transação.

Fase3 compilada/testada em /tmp/zorykai11-phase3.log. Fase4 compilada/testada em /tmp/zorykai11-phase4.log: API por fachada, registros com ownership, DTOs imutáveis e listeners anteriores canceláveis/depois confirmados, sobre os serviços existentes.

Fase5 mantém importadores e fixtures1.0 intactos, com cotação dinâmica sem gravar descontos no preço-base. O arquivo discounts.json é novo e só é criado quando ausente. Estágios de journal antigos continuam legíveis; eventos de recuperação preenchem desconto0 quando o registro antigo não possui esse campo.

Adição solicitada durante o trabalho: config/zorykaimenus/logs/erros.log com WARN/ERROR e stack traces do mod, rotação5MB e até5 arquivos compactados. Captura também exceções não tratadas de outros loggers que contenham frames deste mod. Nenhuma alteração nos logs globais; falha no destino é mostrada no console.
