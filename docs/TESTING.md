> Revisão atual de compatibilidade: [documentação](COMPATIBILITY.md). Os avisos abaixo sobre Molang e placeholders sem suporte descrevem a entrega anterior; a cobertura atual e seus limites estão no documento da revisão.

> Revisão de continuação: veja [CONTINUACAO.md](../reports/CONTINUACAO.md). Nesta revisão passaram seis testes JUnit; os testes de servidor abaixo se referem à entrega anterior.

# Relatório de testes

Execução local em 20/11/2026, servidor isolado em127.0.0.1. Java21.0.9, Minecraft1.21.1, Fabric Loader0.17.2, FabricAPI0.116.6+1.21.1, Cobblemon1.8.3, Impactor5.3.5+1.21.1 e LuckPerms5.4.140. Homologação de Linux; não é uma comprovação de execução em Windows ou em todas as combinações de mods.

**Resultado final:3 testes JUnit e35 verificações de integração aprovados.**

## Construção e testes automáticos

`./gradlew build`: compila código, recursos, access widener, JAR remapeado e fontes. JUnit testa persistência/exclusão por jogador no journal e compara todos os510 produtos migrados com a origem, incluindo IDs, slots, preços e componentes. Também verifica conversão repetível e desativação das condições Molang. Resultados XML acompanham `evidence/junit/`.

A primeira compilação da fundação passou. Checkpoints posteriores compilaram menus, economia, seletor e importadores. Uma falha inicial no teste de contagem foi corrigida: quatro exemplos de preset não fazem parte dos510 produtos comerciais. Uma falha no remapeamento do JAR de diagnóstico foi corrigida incluindo a saída principal no classpath do remapeador; não era uma falha de compra no JAR principal. Os relatórios finais correspondem às execuções corrigidas.

## Integração com servidor real

O módulo separado `src/integration/` fornece o JAR de laboratório `*-checks.jar`; ele não está incluído no mod de produção. Foi executado com jogador real conectado, usando registros Minecraft, ScreenHandlers nativos, arquivos reais de jogador, contas reais do Impactor e LuckPerms. Parte das interações é dirigida pelo servidor de testes, por chamadas ao próprio handler nativo; isso não é apresentado como teste de todos os pacotes de rede possíveis.

Asserções e resultados individuais: `evidence/integration-final.json`. Logs: `evidence/server-final.log`. Incluem:

- Seis tamanhos de inventário; abertura, paginação, loja e confirmação.
- Bloqueio de retirada de ícones, shift, drag, swap/hotbar, throw e clone em handler ativo.
- Quantidade16; compra de16 unidades por40.00 e saldo9960 a partir de10000.
- Duas solicitações simultâneas: um único débito e uma única entrega.
- Venda de10 unidades com crédito12.50; venda de todos os elegíveis pelo menu.
- Venda sem itens, componentes incompatíveis, inventário cheio, saldo insuficiente e revisão antiga: operações recusadas.
- Negação LuckPerms antes da compra; revogação de acesso ao menu enquanto aberto.
- Reload inválido preserva snapshot; reload válido aplica.
- IDs inválidos rejeitados pelo registro.
- Compra real de duas Poké Balls migradas por650.0, conforme os325.0 unitários da origem.
- Compra real de TM por75000, preservando seu componente `cobblemon:tm_move`.
- Moeda inexistente injetada para provocar exceção real do adaptador: pendência preservada, sem tentativa automática de repetir débito; atestado administrativo de não aplicação libera a operação.
- Débito confirmado seguido por mudança do catálogo: estorno sem entrega.
- Inventário preenchido entre solicitação e retorno assíncrono: nova verificação e estorno.
- Desconexão real durante compra: estorno, sem entrega ao jogador desconectado.

A injeção de moeda inexistente prova o tratamento de exceções do adaptador; **não simula uma prova de falha de rede exatamente após um débito persistido**. A sequência de compra/estorno usa operações reais no provedor, sem carteira falsa.

## Cliente e interface

Cliente Minecraft1.21.1/Fabric com apenas FabricAPI e Cobblemon na pasta de mods, **sem ZorykaiMenus**. Registrado em `evidence/runtime.json`. O cliente foi operado por teclado/mouse no display local; ações e capturas acompanham a entrega.

Compra física pelo menu:16 pedras por40.00. Venda física pelo botão direito, selecionar todos e confirmar:8 pedras por10.00. Confirmações constam dos logs e capturas `purchase-total.png`/`sale-total.png`. Isso comprova a instalação somente no servidor para o ZorykaiMenus; o cliente continua precisando dos mods normais dos itens mostrados.

## Migração real

Importador executado sobre os JSONs extraídos do ZIP fornecido, no servidor com registros reais.32 lojas,510 produtos e15 menus convertidos; preços sem divergência. Validação final sem erros nos arquivos ativados. Quatro menus Molang permanecem desativados;19 ícones dependem de mods ausentes e um botão referencia loja não fornecida. Placeholders e serviços externos continuam pendentes.

Migração repetida:47 arquivos antes/depois, mesmos hashes, originais intactos e uma única pasta de backup por fingerprint. Evidência: `evidence/migration-repeatability.json`. Esses números não significam substituição integral dos mods antigos.

## Limites da comprovação

Não houve teste de queda física de energia, corrupção de disco, interrupção em cada instrução de cada etapa, servidor com múltiplos nós, carga massiva de jogadores ou todos os mods externos das configurações. Outros fluxos de recuperação existem no código, mas só os cenários listados são declarados executados. Não se afirma compatibilidade funcional com serviços ausentes de badges, mega_showdown, perfis, kits, warps, dex, cash e tokens.

## Repetir

```bash
python3 tools/prepare_dependencies.py
./gradlew build
./gradlew remapIntegrationJar
```

O build de produção não depende do módulo de diagnóstico. Os testes de migração usam a pasta `migration-input/` de JSONs fornecidos. Sem essa pasta, esses dois testes são explicitamente marcados como ignorados pelo JUnit, não como migração validada.

Para repetir a integração, monte servidor novo e isolado, aceite a EULA de teste, inclua os mods listados e o JAR de diagnóstico; execute com `-Dzorykai.integrationTest=true` e `server-ip=127.0.0.1`. Migre e recarregue os dados fornecidos; conecte exatamente um perfil descartável operador. Execute `zcheck` no console. **O ensaio zera inventário e define saldo desse perfil**, e termina desconectando-o. Nunca usar num servidor de produção ou com perfil de jogador real.
