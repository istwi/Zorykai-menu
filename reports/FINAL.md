> Revisão atual de compatibilidade: [documentação](../docs/COMPATIBILITY.md). Os avisos abaixo sobre Molang e placeholders sem suporte descrevem a entrega anterior; a cobertura atual e seus limites estão no documento da revisão.

> Este documento registra a primeira entrega. A revisão mais recente está em [CONTINUACAO.md](CONTINUACAO.md).

# Entrega ZorykaiMenus 1.0.0

- JAR de produção compilado para Fabric1.21.1/Java21, com SQLite aninhado; Impactor é dependência instalada no servidor.
- Build concluído.3 testes JUnit e35 verificações de integração passaram. A integração usa servidor real, Impactor real, Cobblemon1.8.3, LuckPerms e um cliente sem ZorykaiMenus.
- Compra e venda também exercitadas por cliques físicos no cliente, com capturas e logs.
-32 lojas e510 produtos migrados; preços, IDs, slots e componentes comparados com a origem.0 divergências de preço.
-15 menus convertidos;4 exemplos Molang desativados;11 menus ativáveis com pendências documentadas.19 ícones requerem mods ausentes no laboratório. Uma categoria aponta para arquivo não fornecido. Placeholders externos e serviços de comandos permanecem dependências.
- Migração repetida:47 arquivos sem duplicação e com hashes iguais; originais preservados.
-0 operações financeiras pendentes no fim do laboratório.

A substituição integral de SkiesShop/SkiesGUIs **não está homologada**, pelos recursos externos fora do escopo1.0. A versão não implementa descontosVIP, editor, pesquisa, histórico pesquisável, Molang ou sistemas avançados de Pokémon.

Consulte docs/TESTING.md, docs/MIGRATION.md e docs/TRANSACTIONS.md para os limites específicos da comprovação e a recuperação de operações incertas. Não instalar o JAR de diagnóstico em produção.
