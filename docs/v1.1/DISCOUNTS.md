# Descontos VIP1.1

`config/zorykaimenus/discounts.json` é criado somente quando ausente. A configuração é validada junto do catálogo, sem substituir o snapshot ativo se houver erro. Nunca reescreve os preços-base dos produtos.

Grupos iniciais: vip5%, vipplus10%, mvp15%, mvpplus20%. Os nós são `zorykaimenus.discount.<grupo>`. LuckPerms é opcional: sem ele, o desconto é0. Operador não recebe desconto por fallback; uma permissão explicitamente concedida ou herdada no LuckPerms é necessária.

```json
{
  "enabled": true,
  "stackDiscounts": false,
  "maxDiscountPercent": 50,
  "roundingMode": "HALF_UP",
  "groups": [
    {"permission":"zorykaimenus.discount.vip","discountPercent":5},
    {"permission":"zorykaimenus.discount.vipplus","discountPercent":10},
    {"permission":"zorykaimenus.discount.mvp","discountPercent":15},
    {"permission":"zorykaimenus.discount.mvpplus","discountPercent":20}
  ],
  "shops": {},
  "categories": {},
  "products": {}
}
```

Não há acumulação: `stackDiscounts: true` é rejeitado. Percentuais precisam estar entre0 e100 e não ultrapassar o teto configurado. `UNNECESSARY` não é aceito como política de arredondamento dos descontos; os modos usuais do Java são aceitos.

## Regras específicas

Cada escopo recebe um objeto com `groups`, `override`, `enabled` e/ou `disablePermission`. Exemplo de conteúdo para `shops`:

```json
{"loja_pokebolas":{"override":true,"groups":[{"permission":"zorykaimenus.discount.vip","discountPercent":7}]}}
```

`categories` usa a chave declarada em `product.category`, ou `shop.category` como fallback. Categorias migradas normalmente são lojas ligadas por OPEN_SHOP: pode usar uma regra `shops` para elas sem editar seus produtos. O campo novo `category` é opcional.

`products` usa a chave `idDaLoja/idDoProduto`. Também é possível declarar `discount` diretamente no JSON da loja/produto:

```json
{"id":"ultra_ball","discount":{"enabled":false}}
```

Ou desativar para jogadores que possuam uma permissão específica:

```json
{"id":"ultra_ball","discount":{"disablePermission":"zorykaimenus.discount.exclude.ultra_ball"}}
```

Ordem: geral → loja no arquivo global → discount inline da loja → categoria → produto no arquivo global → discount inline do produto. Sem `override`, escolhe o maior percentual válido desses escopos. Com `override: true`, o escopo substitui o percentual acumulado até aquele ponto; se nenhuma de suas permissões estiver concedida, o resultado desse escopo é0. Um `enabled: false` em escopo aplicável bloqueia o desconto; o desligamento geral também não é revertido por regra específica.

## Dinheiro e confirmação

A unidade com desconto é calculada em BigDecimal e arredondada na precisão informada por `Currency.decimals()` do Impactor. Total = unidade efetiva arredondada × quantidade. A mesma função atende prévia, máximo, API e confirmação. Por isso, descontos sobre valores muito pequenos podem resultar em economia0 após arredondamento; isso aparece na interface.

Preço original, percentual, economia, unidade final e saldo estimado aparecem na interface. A venda mantém seu preço original, sem desconto de compra. Na confirmação o servidor reconsulta regras/permissões e confere o total mostrado: se ele mudou, pede nova confirmação em vez de cobrar outro valor. Se mudar depois do débito e antes da entrega, usa o estorno já existente.

## Administração

- `/zmenu discount reload`: recarrega atomicamente descontos e configurações.
- `/zmenu discount list`: mostra regras atuais.
- `/zmenu discount check <jogador>`: mostra o desconto geral; loja/categoria/produto podem substituí-lo.

Exigem `zorykaimenus.discount.admin` ou operador nível4. Erros de configuração vão para o console e `config/zorykaimenus/logs/erros.log`.
