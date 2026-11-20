# ZorykaiMenus 1.0.0

Mod de menus e lojas para Fabric1.21.1, Java21 e Impactor. Instalação do ZorykaiMenus somente no servidor.

**Downloads:** [JAR](dist/pronto-github/zorykaimenus-1.0.0.jar) · [Configurações migradas](dist/pronto-github/zorykaimenus-configs-migradas.zip) · [Projeto completo](dist/pronto-github/zorykaimenus-1.0.0-projeto.zip).

A revisão final remove a referência antiga à loja de blocos. `/zmenu shop <nome>` substitui `/tentavip open <nome>`, sem necessidade de configurar uma loja fixa. [Estado da entrega e testes](reports/PRONTO_GITHUB.md).

> Entrega atual: [pronto para GitHub](reports/PRONTO_GITHUB.md). Os arquivos finais estão em `dist/pronto-github/`.

> Revisão atual de compatibilidade: [documentação](docs/COMPATIBILITY.md). Os avisos abaixo sobre Molang e placeholders sem suporte descrevem a entrega anterior; a cobertura atual e seus limites estão no documento da revisão.


Mod independente para menus e lojas de inventário vanilla, instalado somente no servidor Fabric **Minecraft 1.21.1 / Java 21**. Integração direta com **Impactor 5.3.5+1.21.1**, moeda `impactor:dollars`. Fabric Loader 0.17.2 e Fabric API 0.116.6+1.21.1 usados no desenvolvimento. LuckPerms é opcional; Cobblemon só é necessário para usar seus itens.

**Revisão atual:** compatibilidade com as expressões shiny fornecidas, placeholders do Core/Dex e ícones temporários explícitos. A referência antiga à loja de blocos foi removida e o comando antigo foi esclarecido; não se declara substituição irrestrita de todas as funções dos mods antigos. Consulte [migração](docs/MIGRATION.md), [testes](docs/TESTING.md) e o relatório JSON produzido pelo importador.

## Instalação

1. Instale Fabric, Fabric API e Impactor nas versões acima no servidor de homologação. Faça uma cópia externa do mundo e das configurações antes de trocar mods.
2. Copie **somente** `zorykaimenus-1.0.0.jar` de `build/libs/` para `mods/`. O arquivo `*-checks.jar` é exclusivo do laboratório e não faz parte da instalação.
3. Inicie o servidor. Arquivos ausentes são criados em `config/zorykaimenus/`; arquivos existentes são preservados.
4. Abra `/zmenu` e `/zmenu shop example`. O exemplo usa pedra: compra $2,50/unidade e venda $1,25/unidade. **Esses são preços de exemplo, não preços migrados.**
5. Edite os JSONs e execute `/zmenu validate`, depois `/zmenu reload`. JSON inválido conserva a configuração ativa anterior. Para aliases novos, reinicie o servidor.

O cliente não precisa do ZorykaiMenus. Itens de outros mods exigem os clientes normalmente necessários para esses mods; não é uma promessa de que o Cobblemon funcione em cliente vanilla.

## Construção

```bash
python3 tools/prepare_dependencies.py
./gradlew build
```

Java 21 deve estar no PATH ou em JAVA_HOME. O preparador baixa o Impactor oficial e confere SHA-512 contra os metadados fixados em `evidence/impactor-version.json`. Gradle baixa as demais dependências. Saída: `build/libs/zorykaimenus-1.0.0.jar`; o `*-sources.jar` contém o código.

## Comandos

| Comando | Uso |
|---|---|
| `/zmenu` | Abrir menu principal |
| `/zmenu open <menu>` | Abrir menu; sugestões automáticas |
| `/zmenu shop <loja>` | Abrir loja; sugestões automáticas |
| `/zmenu reload` | Validar e substituir a configuração ativa de uma só vez |
| `/zmenu validate` | Validar sem aplicar |
| `/zmenu migrate` | Converter os arquivos antigos na pasta de origem definida |
| `/zmenu status` | Contagens e operação pendente do próprio jogador |
| `/zmenu recover` | Recuperar etapas comprovadas; nunca repetir débito/crédito incerto |
| `/zmenu resolve <uuid> <operação> applied\|not_applied` | Atestado administrativo **após conferir no Impactor** |

Os dois últimos comandos dão acesso à recuperação necessária para falhas parciais; não implementam o histórico pesquisável planejado para versões futuras. As permissões administrativas são `zorykaimenus.reload`, `.validate`, `.migrate`, `.status` e `.resolve`, ou operador nível 4. Jogadores comuns podem usar `.use`, `.open`, `.shop`, `.buy`, `.sell` por padrão; LuckPerms pode negá-las explicitamente. Permissão personalizada de menu, produto, botão ou ação exige concessão explícita ou operador. Sem LuckPerms, esses acessos restritos ficam limitados a operadores.

## Funcionalidades

Menus de 9 a 54 slots; itens registrados e componentes nativos; MiniMessage, degradês e lore; navegação, páginas, mensagens, sons, ações e aliases com detecção de conflito. Categorias e subcategorias são lojas/menus ligados por ações `OPEN_SHOP` e `OPEN_MENU`.

Compra com seleção `-64/-10/-1/+1/+10/+64`, atalhos 1/16/32/64, preço unitário, total e saldo estimado. Venda por botão direito, mesma seleção, quantidade elegível e opção de vender todos. Quantidade máxima configurável. O servidor recalcula o total e verifica permissões e componentes.

Apenas os 36 slots principais entram nas compras/vendas; armadura, mão secundária e outros contêineres não são vendidos. Nome, encantamentos, componentes de TM e outros dados precisam coincidir com o produto configurado. Slots decorativos não são retirados pelo jogador.

## Limites de segurança e escopo

Valores usam BigDecimal e o Impactor é a autoridade do saldo. O journal SQLite interno registra intenções antes das etapas financeiras e de inventário; o salvamento do inventário é conferido no arquivo de jogador. Operações incertas ficam bloqueadas para conferência: **não apagar `internal/pending.sqlite` para desbloqueá-las**, pois isso remove a proteção contra repetição. Não há transação distribuída atômica entre Minecraft e Impactor; veja [recuperação](docs/TRANSACTIONS.md).

Ações de console ficam desativadas por padrão. Exigem `allowServerCommands: true`, permissão explícita na ação e concessão ao jogador. Comandos do jogador mantêm as permissões dele. Um administrador que libera um comando de console assume responsabilidade pelo comando configurado.

Esta revisão dá suporte às expressões shiny fornecidas e à Text Placeholder API do Core/Dex. Não é um interpretador Molang geral. Descontos VIP, busca, editor visual e histórico pesquisável continuam fora do escopo. Veja docs/COMPATIBILITY.md.

## Instalação da revisão de compatibilidade

Use o JAR atualizado em `dist/compatibilidade/`. Para os valores externos, instale no servidor [Text Placeholder API2.4.2+1.21](https://github.com/Patbox/TextPlaceholderAPI/releases/download/2.4.2%2B1.21/placeholder-api-2.4.2%2B1.21.jar), junto aos seus Shirokai Core e Shirokai Dex. Essas dependências não foram incorporadas ao JAR. Nenhuma delas é exigida no cliente para exibir os menus.

Depois da troca do JAR e do reinício, use `/zmenu migrate`, confira `migration/report.json`, depois `/zmenu validate` e `/zmenu reload`. A migração preserva arquivos convertidos que você tenha editado; conflitos exigem revisão manual. O ZIP de JSONs é uma alternativa para instalação limpa em homologação e não deve ser extraído sobre configurações personalizadas sem revisão.
