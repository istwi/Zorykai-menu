> Atualização final: a referência antiga a `loja_blocos` foi removida por confirmação do administrador. `/tentavip open <nome>` era o comando genérico antigo; `/zmenu shop <nome>` é seu equivalente e não exige loja fixa. Esses itens não são pendências.

# ZorykaiMenus 1.0.0 — revisão atual de compatibilidade

Implementação concluída para os recursos fornecidos que puderam ser identificados:

- Expressões shiny dos quatro menus Molang de exemplo, mensagem de abertura e prioridade dos botões. As quatro definições antes desativadas agora estão ativas. Não é um interpretador Molang completo.
- Text Placeholder API2.4.2+1.21, com os provedores reais do Shirokai Core0.18.0-dev e Shirokai Dex0.7.0-dev.1+mc1.21.1-cobblemon1.8.3 encontrados no GitHub informado.
- Cash, tokens, percentual da Pokédex, métricas por coleção, cargo LuckPerms, dólares e objetivo gyms. Sem carteira paralela, contadores fabricados ou alteração dos valores dos provedores.
- Alias `progress` antigo apresenta `registered/total` a partir do Dex real, sem substituir um provedor existente de progress.
- Atualização dos menus a cada20 ticks para acompanhar dados e condições da equipe.
-19 ícones ausentes preservam seu ID original e usam papel vanilla explicitamente registrado como fallback; ações e botões permanecem acessíveis. Produtos inexistentes continuam proibidos.

## Verificação desta revisão

`./gradlew build remapIntegrationJar` concluído. **11 testes JUnit e51 verificações de integração aprovadas**, nenhuma ignorada nos testes JUnit desta execução.

As51 verificações correspondem às35 verificações de compra/venda, inventário, permissão, configuração e recuperação, repetidas com Core/Dex instalados, mais16 verificações novas de condição Cobblemon, provedores, scoreboard, ícones e validação do catálogo migrado. Relatórios individuais em `evidence/compatibility/`.

Servidor real isolado: Minecraft1.21.1, Java21.0.9, Fabric Loader0.17.2, FabricAPI0.116.6+1.21.1, Cobblemon1.8.3, Impactor5.3.5, LuckPerms5.4.140, Text Placeholder API2.4.2+1.21, Core0.18.0-dev e Dex0.7.0-dev.1. Usaram-se os artefatos publicados dos provedores, com hashes registrados. O checksum publicado do Dex foi conferido.

Cliente real com apenas FabricAPI e Cobblemon, sem ZorykaiMenus/Core/Dex. Foram observados: menu shiny alternando para ouro após receber Pikachu shiny; perfil com dados reais dos provedores; Pokédex com1/884 e0,11% calculados pelo Dex; ginásios com18 botões preservados e ícones temporários. As capturas são evidência de interface, separada das51 asserções.

Cash e tokens exibidos na captura são os valores retornados pelo Core para as contas descartáveis do laboratório. O teste de provider verifica resolução, não todos os possíveis saldos ou formatos monetários dessas moedas. A compra/venda do mod permanece em dólares e foi novamente testada com valores exatos.

Migração atual:32 lojas,510 produtos,510 preços preservados,0 divergências de preço,15 menus ativos,19 fallback icons explícitos,0 erros de validação dos arquivos ativos. A referência antiga à loja de blocos foi removida na revisão final.

## O que ainda depende do servidor

1. Referência antiga à loja de blocos: resolvida por remoção autorizada, sem criar loja.
2. Comando antigo: esclarecido e documentado; `/zmenu shop <nome>` substitui `/tentavip open <nome>`.
3. Para os desenhos originais das19 medalhas/ícones, instalar os mods que registram esses itens no servidor e nos clientes correspondentes. O fallback mantém os botões, mas não reproduz essas texturas.
4. Homologar as ações que delegam comandos a outros mods do seu servidor. A inspeção e os testes focaram os provedores de dados, não a execução de cada kit, warp ou ginásio externo.

Permanecem os limites de testes de falhas de disco, carga e outras combinações de mods descritos em docs/TESTING.md. Não foi alterado nem publicado qualquer repositório remoto. Não foi inventado conteúdo comercial da loja ausente.
