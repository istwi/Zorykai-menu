# Testes da bolsa

## Automatizados

A suíte inclui as regras anteriores de preços VIP, cálculo de máximo, migração, extensões e journal, mais verificações de mercado: queda progressiva de lote, referência de alta precisão, conservação contra vendas fracionadas, piso, recuperação por intervalos/tempo offline, relógio retrocedendo, preços pequenos/zero, validação de regras, isolamento de produtos/lojas, reserva exclusiva e reinício do SQLite, conclusão idempotente e rollback conjunto de pendência/pressão.

## Servidor dedicado

Minecraft 1.21.1, Fabric Loader 0.17.2, Java 21.0.9, Fabric API e Impactor reais. Jogadores `ServerPlayerEntity` simulados usam inventários nativos e cliques nos menus. Não há clientes gráficos conectados.

O ensaio verifica venda pelo menu com valor exato creditado no Impactor, redução de preço para outro jogador, rejeição de confirmação antiga, produto fixo, preço de compra preservado, bloqueio durante crédito real atrasado, média com divisão periódica, crédito recusado com devolução, cancelamento por addon, recuperação real por espera curta, reset administrativo e reload inválido preservando a configuração e registrando erro.

Uma confirmação perdida é injetada **depois que o Impactor realmente persistiu o depósito**. A pressão aguarda a conciliação; a confirmação administrativa deve contabilizar uma vez e não repetir o depósito. Esse erro é esperado na fixture.

O reinício da base SQLite é coberto nos testes automatizados; não foi executada queda abrupta do processo no meio de um pagamento real. Não houve benchmark com centenas de jogadores nem ensaio gráfico com os JSONs personalizados do servidor. Não há alteração de Cobblemon ou dependência nova dele; o runtime desta suíte não carrega os provedores opcionais LuckPerms/Cobblemon.

## Reproduzir

Java 21, dependências oficiais via `python3 tools/prepare_dependencies.py`, depois:

```sh
./gradlew test build remapIntegrationJar
python3 tools/prepare_market_checks.py \
  build/libs/zorykaimenus-1.2.0-checks.jar /servidor-descartavel/mods/checks.jar
```

Adicione o produto, Fabric API e Impactor ao servidor descartável, aceitando a EULA apenas para esse teste conforme autorização do responsável. Inicie o servidor com `-Dzorykai.integrationTest=true -Dzorykai.marketTest=true`, ligado somente a `127.0.0.1`. A fixture cria jogadores, configura uma loja de teste, movimenta saldo/itens, grava `market-results.json` e encerra o servidor. Não instale a fixture em produção.

## Resultado final

**41 testes JUnit**, zero falhas; **32 verificações nativas**, todas aprovadas. A regressão nativa inclui uma compra real debitada/entregue e uma venda real de produto excluído da bolsa, além dos casos de mercado. [JUnit](unit-results.json) · [servidor dedicado](native-results.json) · [trechos de execução](runtime-evidence.txt). Nenhum cliente gráfico real foi usado.
