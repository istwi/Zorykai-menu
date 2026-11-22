# ZorykaiMenus 1.3.6 — principal opcional e aliases prioritários

[Baixar JAR 1.3.6](dist/v1.3.6/zorykaimenus-1.3.6.jar) · [Configuração e testes](docs/optional-main/README.md). Não exige menu principal, não recria exemplos apagados e permite assumir comandos escolhidos com `aliasOverrides`. As permissões do destino, bloqueio de comandos genéricos, venda pelo PC e correção de cargos são preservados.

# ZorykaiMenus 1.3.5 — acesso por aliases/console e correção de cargo

[Baixar JAR 1.3.5](dist/v1.3.5/zorykaimenus-1.3.5.jar) · [Configurar aliases e comandos de console](docs/menu-access/README.md). Jogadores comuns não abrem arquivos arbitrários por `zmenu open/shop`; console usa `zmenu open <menu> <jogador>` ou `zmenu shop <loja> <jogador>`. Preserva venda de Pokémon pelo PC e corrige prefixos LuckPerms misturando códigos legados e MiniMessage.

# ZorykaiMenus 1.3.4 — abrir venda pelo NPC

Abertura direta bloqueada. Configure o NPC para executar no console `zmenu pokemon open %player%` (placeholder do Shirokai Trainers). [Baixar JAR1.3.4](dist/v1.3.4/zorykaimenus-1.3.4.jar) · [Configuração do NPC e limites](docs/pokemon-sales/NPC-ONLY-1.3.4.md).

# ZorykaiMenus 1.3.3 — voltar ao menu após vender

Venda concluída pelo menu retorna automaticamente à lista atualizada do PC, na mesma página quando possível. [Baixar JAR1.3.3](dist/v1.3.3/zorykaimenus-1.3.3.jar) · [Detalhes](docs/pokemon-sales/RETURN-MENU-1.3.3.md). Substitua o JAR e reinicie; configurações/dados e correção Windows preservados.

# ZorykaiMenus 1.3.2 — correção de gravação no Windows

Corrige `AccessDeniedException` ao sincronizar diretório do PC no Windows, preservando a gravação do arquivo e troca atômica. [Baixar JAR1.3.2](dist/v1.3.2/zorykaimenus-1.3.2.jar) · [Atualização, recuperação e erro separado do Spawn](docs/pokemon-sales/WINDOWS-1.3.2.md). Após substituir o JAR e reiniciar, use `/zmenu recover` para a operação pendente. Preserve mundo, configurações e journal.

# ZorykaiMenus 1.3.1 — venda pelo PC

`/zmenu pokemon` lista o PC com modelos nativos de Pokémon,45 por página e confirmação. A equipe permanece intacta. Limite compartilhado de10 vendas diárias por jogador para espécies acima de Uncommon, renovado à meia-noite de São Paulo; Common/Uncommon não consomem cota. Preços originais preservados, sem o novo bônus por IVs solicitado anteriormente e cancelado.

[Baixar JAR1.3.1](dist/v1.3.1/zorykaimenus-1.3.1.jar) · [Instalação, configuração e limites](docs/pokemon-sales/UPDATE-1.3.1.md). Substitua o JAR do ZorykaiMenus e reinicie, preservando `config/zorykaimenus` e seus dados. Não apague o journal interno. Campos novos ausentes recebem os padrões em memória.

# ZorykaiMenus 1.3.0

**Venda de Pokémon 1.3:** [JAR](dist/v1.3.0/zorykaimenus-1.3.0.jar) · [Preços e configuração](docs/pokemon-sales/README.md). Use `/zmenu pokemon`; referência de Magikarp comum = **250**. Menor dificuldade de obtenção entre spawn, pesca e evolução prevalece; rotas de datapacks podem reduzir a avaliação. Confirmação por UUID, retirada durável antes do crédito e recuperação com snapshot completo.


Atualização do mod existente de menus e lojas para **Minecraft1.21.1 / Fabric / Java21**, com Impactor e instalação somente no servidor. Mod ID preservado: `zorykaimenus`.

**Downloads 1.2:** [JAR](dist/v1.2.0/zorykaimenus-1.2.0.jar) · [Exemplo da bolsa](examples/dynamic-market/shops/farm_market.json) · [Configuração e testes](docs/market/README.md).

## Bolsa de valores

Escolha a loja adicionando `"market": {"enabled": true}` no JSON dela e use `/zmenu reload`. Vendas confirmadas reduzem a cotação global daquele produto; após intervalos sem novas vendas, o preço recupera gradualmente até a base. Piso, queda e recuperação são configuráveis. Os padrões são piso de 25%, queda de 1 ponto percentual da base por 64 unidades e recuperação de 64 unidades de pressão a cada 5 minutos sem nova venda confirmada.

O lote inteiro entra no cálculo progressivo, com o total exibido antes da confirmação. Se a cotação mudar, o jogador precisa revisar a confirmação; não há alteração silenciosa do valor. Lojas não selecionadas continuam com os preços fixos. Compra e descontos VIP mantêm suas regras.

Veja [como ativar em uma loja existente](docs/market/README.md), [JSON completo de exemplo](examples/dynamic-market/shops/farm_market.json) e [evidências e limites](docs/market/TESTING.md).

**Downloads1.1:** [JAR](dist/v1.1.0/zorykaimenus-1.1.0.jar) · [Projeto completo](dist/v1.1.0/zorykaimenus-1.1.0-projeto.zip) · [Exemplos JSON](dist/v1.1.0/zorykaimenus-1.1.0-exemplos.zip). A entrega1.0 permanece em dist/pronto-github/.

**Validação 1.2:** 41 testes automatizados e 32 verificações dedicadas aprovados com jogadores simulados. [Relatório](docs/market/TESTING.md).

## O que mudou

- Descontos VIP por permissões, com regras por loja/categoria/produto, teto e exclusões. Não altera preços-base nem preços de venda.
- Comprar máximo usando saldo, desconto, espaço, componentes e limites reais, seguido de confirmação.
- Condições e placeholders opcionais de equipe Cobblemon, somente de leitura.
- API pública para menus/lojas, ações, placeholders, condições, preços/descontos e eventos de comércio.
- Logs automáticos em **config/zorykaimenus/logs/erros.log**, com stack trace e rotação.

**Validação histórica da 1.1:**30 testes JUnit e105 verificações de integração aprovadas. [Relatório e limites](docs/v1.1/TESTING.md).

## Atualizar ou instalar

1. Pare o servidor e preserve uma cópia do mundo/configuração.
2. Instale somente **zorykaimenus-1.3.0.jar** na pasta mods, removendo a versão anterior do mesmo mod.
3. Mantenha FabricAPI0.116.6+1.21.1 e Impactor5.3.5+1.21.1, versões usadas na homologação. Java21 é necessário.
4. Inicie o servidor. discounts.json ausente será criado; configurações existentes não são sobrescritas.
5. Configure grupos no LuckPerms, valide com /zmenu validate e use /zmenu reload. Sem permissão VIP válida, o preço é integral.

O cliente não precisa de ZorykaiMenus. Para itens/modelos Cobblemon, continuam necessários os mods de conteúdo habituais. Os exemplos/diagnósticos Java são módulos de laboratório e não devem ser instalados junto ao produto em produção.

A1.3 lê JSONs1.0/1.1/1.2, inclusive arquivos diretamente em menus/shops ou subpastas. Não duplique IDs ao mover arquivos. /zmenu shop <nome> continua sendo o equivalente de /tentavip open <nome>. Nenhuma loja de blocos precisa ser criada.

## Documentação1.1

- [Análise da arquitetura1.0](docs/v1.1/ARCHITECTURE_BEFORE.md)
- [Descontos e comandos VIP](docs/v1.1/DISCOUNTS.md)
- [Comprar máximo](docs/v1.1/MAXIMUM.md)
- [Cobblemon opcional](docs/v1.1/COBBLEMON.md)
- [API e exemplos compiláveis](docs/v1.1/API.md)
- [Atualização e logs](docs/v1.1/UPGRADE.md)
- [Alterações](docs/v1.1/CHANGES.md)
- [Testes](docs/v1.1/TESTING.md)

Continuam disponíveis os comandos /zmenu, open, shop, reload, validate, migrate, status, recover e resolve. Os novos comandos administrativos são /zmenu discount reload, check <jogador> e list. Permissão: zorykaimenus.discount.admin ou operador4.

## Construir

```bash
python3 tools/prepare_dependencies.py
./gradlew clean build
```

Gradle Wrapper8.14.3 e Loom1.10.5 preservados. O preparador baixa os artefatos oficiais fixados de Impactor e Text Placeholder API com conferência de hash. Esta última é opcional no runtime das integrações externas e dependência de compilação do adaptador existente.

Saída: build/libs/zorykaimenus-1.3.0.jar. O build também compila o addon Java de exemplo; remapExampleJar produz seu JAR separado. As integrações registradas usam o namespace do addon, sem sobrescrever recursos nativos.

## Configuração e migração preservadas

[Formato JSON](docs/CONFIGURATION.md), [migração Skies](docs/MIGRATION.md), [recuperação financeira](docs/TRANSACTIONS.md) e [compatibilidade1.0](docs/COMPATIBILITY.md) continuam documentados. As diferenças atuais estão nos documentos1.1 acima.

A migração conserva510 produtos e preços dos arquivos fornecidos. Ícones ausentes podem usar o fallback explícito de papel sem mudar o ID original; ações dependentes de outros mods ainda precisam de seus serviços. Para cash/tokens/dex externos, mantenha Core/Dex e Text Placeholder API2.4.2+1.21. A integração Cobblemon não é um interpretador Molang geral.

Não apague internal/pending.sqlite para desbloquear operações incertas. Consulte os logs e a recuperação administrativa. Eventos não substituem a conciliação e não prometem entrega exatamente uma vez entre quedas.


## Pesquisa para loja de venda de Pokémon

Levantamento do Cobblemon Fabric 1.8.3 oficial, com planilha, CSVs de espécies/rotas/pesca, pesos e condições. Inclui ressalvas sobre tags não resolvidas e uma proposta econômica separada dos fatos. **Esta pesquisa ainda não implementa negociação de Pokémon no mod.**

- [Relatório e fontes](docs/pokemon-sales-research/README.md)
- [Planilha](docs/pokemon-sales-research/pesquisa-spawns-1.8.3.xlsx)
- [Lista por grupo observado](docs/pokemon-sales-research/lista-por-grupo.md)
- [Download completo da pesquisa](docs/pokemon-sales-research/pesquisa-spawns-cobblemon-1.8.3.zip)
