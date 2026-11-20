# Comprar máximo

O botão COMPRAR MÁXIMO aparece no slot34 da confirmação de compra. A venda conserva seu botão de vender todos. O máximo apenas seleciona a quantidade; o jogador ainda confirma.

Considera saldo real do Impactor, desconto atual, espaço nos36 slots principais, stacks parciais, componentes idênticos e limites nativos de stack. Não usa armadura ou mão secundária. O limite global é `config.json.maxQuantity`; `products[].maxQuantity` opcional pode reduzi-lo. Faixa validada:1..1000000. `available: false` em loja/produto bloqueia acesso e compra sem apagar o produto.

A seleção usa busca binária limitada com a mesma função de preço da confirmação. A função atual usa unidade arredondada por moeda, sem preços dependentes da quantidade; não existem tabelas de quantidade ocultas. Não se limita a dividir saldo por preço. Com preço0, respeita espaço e limites sem divisão. Valores inválidos são recusados antes da operação financeira.

O retorno assíncrono só atualiza a tela se jogador/tela/revisão ainda coincidirem. A confirmação recomputa preço, permissão e capacidade, e o Impactor verifica o saldo no débito. Uma cotação que mudou exige confirmação nova; falta de espaço depois do débito confirmado usa o estorno. Erros como saldo insuficiente ou inventário cheio mantêm a compra sem confirmação.

Exemplo: unidade200 comVIP+10% torna-se180; saldo9000 e espaço64 selecionam50, total9000. Stacks com componentes diferentes não contam como capacidade de completar o mesmo produto.
