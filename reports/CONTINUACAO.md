> Revisão atual de compatibilidade: [documentação](../docs/COMPATIBILITY.md). Os avisos abaixo sobre Molang e placeholders sem suporte descrevem a entrega anterior; a cobertura atual e seus limites estão no documento da revisão.

# Continuação da versão 1.0 — backup de migração

A retomada de `/zmenu migrate` agora confere cada arquivo do backup contra o hash da origem antes de gravar as configurações convertidas. Uma pasta parcialmente copiada é completada; um arquivo existente divergente é preservado e interrompe a migração. Arquivos de origem adicionados, removidos ou alterados durante a cópia também interrompem essa etapa.

Não foram alterados os preços, as operações econômicas, os IDs ou as funcionalidades futuras planejadas. O projeto continua na versão 1.0.0.

Validação desta revisão: `./gradlew build` concluído com seis testes JUnit aprovados, nenhum ignorado. Três testes novos cobrem retomada de backup parcial e repetição, preservação de backup divergente e alterações da origem. Os três testes existentes de preços/componentes/migração e journal também passaram.

As 35 verificações no servidor/cliente registradas na entrega anterior não foram repetidas nesta revisão. Elas permanecem evidência da entrega anterior, sem serem apresentadas como uma nova homologação deste JAR. A mudança desta revisão está limitada à etapa de backup do importador, antes de gravar saídas.

Permanecem as pendências de Molang, placeholders externos, ícones de mods ausentes e loja de destino não fornecida, descritas em `docs/MIGRATION.md`. Nenhuma expansão para versões futuras foi feita.

Arquivos atualizados em `dist/continuacao/`. A entrega anterior em `dist/` foi preservada.
