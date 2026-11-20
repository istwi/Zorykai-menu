# Alterações1.1.0

Atualização incremental da base09814ac, mantendo Mod ID, comandos, slots, importadores, JSONs e preços-base da1.0.

1. Serviço de descontos/preços sobre Trades, sem segundo sistema financeiro. Arquivo discounts.json, permissões VIP explícitas, regras gerais/loja/categoria/produto, exclusão e teto. Moeda e precisão vêm do Impactor. Venda mantém preço original.
2. Botão Comprar máximo na confirmação existente. Saldo assíncrono, capacidade real de stacks/componentes, limite global/produto e busca pela mesma função de cotação. Não confirma a compra automaticamente.
3. Adaptador Cobblemon opcional de leitura, condições de equipe/item e quatro placeholders. Preserva as condições shiny1.0 e a Text Placeholder API existente.
4. Fachada pública com registros de ações/placeholders/condições, consultas de produto/preço/desconto e eventos canceláveis antes/confirmados depois. DTOs imutáveis e ownership de registros; emissores internos. Exemplo Fabric compilado e testado como mod separado.
5. Pedido adicional do administrador: logs persistentes em config/zorykaimenus/logs/erros.log com rotação e stack trace, mantendo o console.

A cotação mostrada na confirmação é conferida novamente antes do débito. Mudança de preço/desconto exige nova confirmação; mudanças após débito usam o fluxo existente de estorno. Valores inválidos de moeda/precisão agora podem ser recusados na pré-validação, antes de criar intenção monetária. A regressão foi atualizada para também testar perda de resposta depois de débito realmente salvo.

O lock de uma operação em andamento não é removido por uma solicitação concorrente recusada. Falhas de observers posteriores não invalidam nem repetem resultados confirmados.

Não foram implementados pesquisa, editor visual, histórico consultável, missões completas, recompensas diárias ou alteração automática de Pokémon. Nenhuma dependência obrigatória nova foi adicionada. Os JARs de exemplo e diagnóstico não são embutidos no produto.
