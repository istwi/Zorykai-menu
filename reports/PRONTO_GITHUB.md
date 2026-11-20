# Preparado para envio ao GitHub

A referência antiga a loja_blocos foi removida do importador, sem criar lojas ou preços. Os arquivos de origem são preservados. Um teste específico garante que a referência não reapareça nas próximas migrações.

O comando antigo /tentavip open <nome> corresponde a /zmenu shop <nome>. Não é necessário definir uma loja fixa nem criar example_categories. Esses pontos não são mais pendências.

Build aprovado com12 testes JUnit, sem falhas nem testes ignorados, incluindo a comparação dos510 produtos/preços e a remoção da referência antiga sem alterar a origem. As51 verificações no servidor pertencem à revisão de compatibilidade anterior; não foram repetidas para esta limpeza limitada ao importador e à documentação.

JAR, projeto e configurações consolidados estão em dist/pronto-github/. Os19 ícones temporários continuam documentados. A homologação na instalação completa do servidor continua recomendada; não é requisito para fornecer o repositório e preparar o envio.

Repositório de destino informado pelo usuário: https://github.com/istwi/Zorykai-menu . Fonte, Gradle Wrapper, fixtures de migração, documentação e artefatos finais preparados para publicação.
