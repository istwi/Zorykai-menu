# Entrega ZorykaiMenus1.1.0

Atualização incremental do projeto existente, Mod ID zorykaimenus preservado.

- Descontos VIP por permissão, regras gerais/loja/categoria/produto, maior percentual válido, sem soma, teto/exclusões e precisão real Impactor. Preços-base e venda preservados.
- Comprar máximo seleciona e aguarda confirmação; valida saldo, espaço, componentes, stacks e limites pela mesma cotação.
- Cobblemon opcional: condições e quatro placeholders de equipe, sem modificação de Pokémon.
- API pública e addon Java separado compilado/testado, incluindo cancelamentos e eventos confirmados.
- Pedido adicional atendido: config/zorykaimenus/logs/erros.log com rotação e stack trace.

Build ./gradlew clean build aprovado.30 testes JUnit e105 verificações de integração passaram no Linux com servidor real e cliente sem ZorykaiMenus. A inicialização sem Cobblemon/LuckPerms foi verificada separadamente. Fonte dos105 resultados:42 da atualização,22 de compatibilidade,36 de regressão e5 de ausência opcional.

O JAR final é idêntico ao usado no servidor validado. O produto não contém classes/entrypoints dos diagnósticos ou do exemplo. Não restaram operações pendentes no laboratório. Os510 produtos/preços dos fixtures continuam preservados.

Atualização e instalação: docs/v1.1/UPGRADE.md. API: docs/v1.1/API.md. Relatório completo/limites: docs/v1.1/TESTING.md. Os19 ícones temporários e dependências de comandos externos da1.0 continuam documentados; não se promete compatibilidade com qualquer mod, carga ou cenário de falha não testado.
