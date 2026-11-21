# Bolsa de valores — ZorykaiMenus 1.2.0

A cotação de **venda** diminui conforme a quantidade vendida por todos os jogadores e recupera gradualmente até o preço base após intervalos sem novas vendas confirmadas. A função é opcional por loja/produto. Nenhuma configuração existente é ativada ou reescrita automaticamente; lojas sem `market.enabled: true` mantêm preços fixos.

## Ativar na loja escolhida

Edite `config/zorykaimenus/shops/<sua_loja>.json` e adicione este bloco no nível principal, junto de `id`, `title` e `products`:

```json
"market": {
  "enabled": true,
  "minimumPercent": 25,
  "dropPercent": 1,
  "volumePerDrop": 64,
  "recoveryVolume": 64,
  "recoverySeconds": 300
}
```

Depois use `/zmenu reload`. Esses valores são exemplos editáveis:

| Campo | Significado |
| --- | --- |
| `enabled` | Ativa nesta loja. Também pode ser configurado em um produto. |
| `minimumPercent` | Piso em porcentagem do preço `sell` original: 25 significa nunca abaixo de 25% da base, respeitando as casas decimais da moeda. |
| `dropPercent` | Queda em pontos percentuais da base para cada `volumePerDrop` unidades. Não é uma queda composta. |
| `volumePerDrop` | Quantas unidades representam essa queda. 64 unidades são uma pilha comum, mas o cálculo usa unidades individuais. |
| `recoveryVolume` | Quantas unidades de pressão são retiradas por intervalo de recuperação. |
| `recoverySeconds` | Intervalo de recuperação sem nova venda confirmada. 300 = 5 minutos. |

Exemplo: com base de $1,00, vender 64 unidades parte de 100% e termina perto de 99%; o lote todo é pago pela soma das cotações progressivas, **$63,68**, não $64. A próxima unidade vale $0,99. Com as regras acima, cinco minutos sem venda retiram 64 unidades de pressão. Se houver volume maior acumulado, são necessários vários intervalos até a base.

Cada nova venda confirmada reinicia a contagem do intervalo. O tempo com o servidor desligado conta quando a cotação for consultada novamente. A recuperação não ultrapassa a base. A pressão é limitada ao volume que atinge o piso, evitando uma dívida de milhões de unidades quando o preço já está no mínimo.

Os valores são arredondados para baixo no total de cada operação, com piso monetário arredondado para cima. Isso evita receber mais ao fracionar a mesma venda em diversas operações. A cotação pode se mover em degraus perceptíveis quando a moeda tem apenas duas casas decimais. O preço médio por unidade do lote pode conter mais casas; o **total confirmado** é o valor efetivamente pago.

## Produtos e lojas independentes

O controle é global por par **ID da loja + ID do produto**. Cacto e cana não compartilham pressão; o mesmo cacto em outra loja também é independente. Mantenha os IDs se quiser preservar o histórico; mudar um ID cria um novo mercado. Produtos duplicados com IDs diferentes têm mercados independentes.

Um produto herda as regras da loja. Para deixá-lo fixo:

```json
"market": {"enabled": false}
```

Ou substitua apenas parâmetros específicos no objeto `market` do produto. Só o preço `sell` varia. Compra e descontos VIP de compra continuam com suas regras anteriores.

## Menu de exemplo

Copie [farm_market.json](../../examples/dynamic-market/shops/farm_market.json) para `config/zorykaimenus/shops/farm_market.json`, execute `/zmenu reload` e abra:

```text
/zmenu shop farm_market
```

O exemplo é somente venda. Inclui cacto e cana com cotação e pedra fixa. Ele não substitui suas lojas existentes. Para abri-lo de outro menu, use uma ação `OPEN_SHOP` com `id: "farm_market"`.

O menu mostra a base, cotação atual de uma unidade e a indicação de bolsa. A lista se atualiza pelo refresh existente, aproximadamente a cada segundo. A confirmação conserva o preço mostrado: se outra venda, recuperação ou mudança de configuração alterar o total antes do clique, a operação é recusada sem retirar itens/pagar; reabra para revisar a nova cotação. A atualização assíncrona do saldo não muda silenciosamente o preço confirmado.

Lores podem usar `{market_sell}`, `{market_base}` e `{market_percent}` em produtos dinâmicos. Descrições antigas com preços escritos literalmente não são reescritas; substitua esses números pelos placeholders ou remova as linhas fixas para não confundir os jogadores. Cores, textos e ícones personalizados permanecem configuráveis.

## Administração

Permissão: `zorykaimenus.market.admin`, com o mesmo fallback administrativo do mod (OP nível 4 sem LuckPerms):

```text
/zmenu market status <loja> <produto>
/zmenu market reset <loja> <produto>
```

O reset restaura a base e é registrado no log. É recusado enquanto houver venda pendente daquele produto. Alterar parâmetros não apaga os dados; o histórico é limitado e interpretado com as novas regras no próximo cálculo.

## Segurança e persistência

Os dados ficam na base SQLite já existente: `config/zorykaimenus/internal/pending.sqlite`, na tabela adicional `market_state`. Não apague esse arquivo para resetar preços: ele também contém operações financeiras pendentes. A nova tabela e o índice são criados de forma aditiva; transações antigas sem campo de mercado continuam recuperáveis.

- Só créditos confirmados entram na pressão. Vendas canceladas ou com crédito recusado não derrubam preço.
- A atualização de pressão e remoção da transação pendente acontecem no **mesmo commit SQLite**. Repetir a conclusão não conta o volume duas vezes.
- Só uma venda dinâmica por produto pode ficar em processamento. Outra tentativa recebe aviso para aguardar, sem alterar o inventário. Essa reserva persiste após reinício. Outros produtos/lojas continuam funcionando.
- Uma resposta financeira incerta segue a recuperação já existente do mod. O produto fica reservado até a conferência. Se o administrador atestar `applied` depois de verificar o Impactor, conta a venda uma vez e não repete o pagamento; `not_applied` permite devolver os itens sem pressão.
- Journal e atualização local não tornam o provedor Impactor atomicamente participante do SQLite. O procedimento anterior de conciliação para respostas ambíguas permanece.

Logs de erro continuam em `config/zorykaimenus/logs/`, além do console. Faça backup do mundo, configurações e base SQLite antes de atualizar. Não há novo mod obrigatório ou instalação no cliente.

## API

`ZorykaiMenusAPI.product()` mantém os preços base do JSON. `effectivePrice(..., SELL, quantidade)` retorna a cotação dinâmica; o `total` é autoritativo e `effectiveUnit` é a média do lote. `discountPercent` segue zero em vendas. Os registros existentes não foram alterados. Eventos de venda confirmada recebem o total real, a média efetiva e a referência de base. Addons não devem recomputar o total multiplicando uma média arredondada.

[Resultados e limites dos testes](TESTING.md).
