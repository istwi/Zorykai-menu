# ZorykaiMenus 1.2.0 — Bolsa de valores

Substitua o JAR anterior com o servidor parado. Mantenha Fabric API, Impactor e Java 21 para Minecraft 1.21.1. Não instale as fixtures. Faça backup da configuração, mundo e SQLite.

Nenhuma loja existente é alterada automaticamente. Ative `"market": {"enabled": true}` no nível principal do JSON da loja escolhida e use `/zmenu reload`. O ZIP contém uma loja de exemplo (`shops/farm_market.json`), não substitui suas lojas.

Padrões: piso 25% da base, queda de 1 ponto percentual por 64 unidades, recuperação de 64 unidades de pressão a cada 300 segundos sem venda confirmada. Os parâmetros são configuráveis. A confirmação mostra o valor do lote e rejeita uma cotação alterada antes da compra/venda.

[Configuração completa](../../docs/market/README.md) · [Testes e limitações](../../docs/market/TESTING.md). 41 testes automatizados e 32 verificações nativas aprovados, sem cliente gráfico real.
