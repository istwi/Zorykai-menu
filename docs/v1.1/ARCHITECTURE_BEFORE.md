# Análise da base 1.0 antes das alterações

Revisão inicial09814ac, repositório existente istwi/Zorykai-menu. Foram lidas as22 classes Java principais, build.gradle, gradle.properties e fabric.mod.json. Minecraft1.21.1/Yarn1.21.1+build.3, Java21, Fabric Loader0.17.2/API0.116.6, Loom1.10.5, Impactor5.3.5. Dependências opcionais: LuckPerms5.4, Cobblemon1.8.3 e Text Placeholder API2.4.2+1.21. SQLite3.49.1.0 aninhado.

Catalog lê JSONs, decodifica registros/componentes e valida referências antes de trocar um Snapshot com revisão. Seed cria somente arquivos ausentes. Menus usa contêineres vanilla LockedMenu, inventários protegidos, callbacks locais e consulta assíncrona de saldo. Ações e permissões são verificadas no servidor.

Preço atual: BigDecimal unitário salvo no produto vezes quantidade. Não há serviço de preço nem desconto. Trades é o único caminho de compra/venda: bloqueio por jogador, journal SQLite, etapas de débito/entrega ou remoção/crédito, inventário salvo e conferido, compensações e recuperação explícita de respostas incertas. ImpactorGateway usa Currency.decimals, Account.balance/withdraw/deposit e EconomyService.save; não existem comandos como substituto financeiro.

Permissions consulta LuckPerms quando presente; o fallback de operador serve ao acesso administrativo. Esse fallback não deve determinar descontos VIP automaticamente. Conditions possui apenas o adaptador shiny legado. Placeholders usa componentes seguros, dados nativos e Text Placeholder API opcional. Não há API pública de extensões ou eventos de comércio.

LegacyImporter converte os formatos reais SkiesShop/SkiesGUI. MigrationBackup verifica hashes e retoma cópias; Migrator preserva destinos editados e registra ícones temporários. Existem32 lojas e510 produtos nos fixtures. As referências antigas à loja de blocos foram removidas por orientação do administrador. Comando atual: /zmenu shop <nome>.

Limites existentes: suporte Molang é restrito às expressões fornecidas; ícones ausentes usam fallback explícito; serviços externos de kits/warps/ginásios não são implementados aqui. Journal não oferece transação distribuída atômica ou entrega exatamente uma vez de callbacks após queda. Esta atualização documentará a semântica dos eventos sem prometer garantias que o provedor não oferece.

Extensão incremental: serviço puro de descontos/preços sobre Trades; botão máximo usando esse mesmo serviço e capacidade nativa; adaptador Cobblemon opcional de leitura; fachada API/registries e listeners isolados. Configurações1.0, preços-base e importadores permanecem.
