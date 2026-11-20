> Atualização final: a referência antiga a `loja_blocos` foi removida por confirmação do administrador. `/tentavip open <nome>` era o comando genérico antigo; `/zmenu shop <nome>` é seu equivalente e não exige loja fixa. Esses itens não são pendências.

> Revisão atual de compatibilidade: [documentação](COMPATIBILITY.md). Os avisos abaixo sobre Molang e placeholders sem suporte descrevem a entrega anterior; a cobertura atual e seus limites estão no documento da revisão.

# Migração dos arquivos fornecidos

Foram inspecionados `skiesshop.zip`, metadados do SkiesShop 1.0.2 e metadados/exemplos de configuração do SkiesGUIs 1.8.1. Nenhuma classe ou implementação desses mods é carregada/copied pelo importador. SkiesGUIs declara All-Rights-Reserved; não se redistribuem os JARs antigos. O novo código é independente.

## Procedimento

Extraia **suas configurações** em:

```text
config/zorykaimenus/migration/source/
├── skiesshop/
│   ├── config.json
│   └── shops/*.json
└── skiesguis/
    └── guis/*.json
```

Execute `/zmenu migrate` como administrador. O comando copia a origem para `migration/backups/<fingerprint>/` antes de gravar conversões. Originais permanecem intactos. Backups parciais são completados e todos os arquivos são conferidos por hash antes da conversão; divergências interrompem a etapa e preservam o arquivo existente. As saídas vão para `shops/migrated/` e `menus/migrated/`; hashes de arquivos gerados permitem repetir sem duplicar produtos. Arquivos de destino editados pelo administrador são preservados e registrados como conflito. A configuração ativa não é trocada pelo comando; veja `migration/report.json`, valide e só então recarregue.

IDs, preços decimais, slots, páginas, itens e componentes são copiados dos dados. Nomes, lore MiniMessage e os templates de preço globais representáveis são preservados. Componentes são validados pelo codec nativo do Minecraft, não por uma lista presumida. Ações OPEN_GUI, COMMAND_PLAYER e PLAYSOUND são mapeadas explicitamente para OPEN_MENU, PLAYER_COMMAND e PLAY_SOUND. Hierarquias de lojas preservam OPEN_SHOP e seus destinos.

## Resultado dos dados recebidos

32 lojas; **510 produtos comerciais**; 15 menus. Há outros quatro registros de exemplo em presets, que não são produtos das lojas comerciais. O teste automatizado compara cada um dos 510 produtos com a origem: ID, slot, preço e componentes. Sem divergência de preço nas conversões verificadas.

No laboratório com Cobblemon 1.8.3, os 510 produtos passaram no codec nativo. Quatro menus de exemplo com condições Molang ficam desativados, sem remover as condições para liberar botões. Onze menus podem ser convertidos, com pendências visuais/funcionais registradas:

- 18 ícones `cobbleversebadges:*` e um `mega_showdown:rusted_sword` não existem nos mods instalados no laboratório; os botões são omitidos da saída ativa, preservando a origem e registrando a causa. Instalar os mods corretos e repetir a migração permite nova validação; não são inventados IDs substitutos.
- A categoria `loja_blocos` é referenciada, mas o arquivo não foi fornecido; seu botão fica desativado.
- `base_shop: example_categories` do arquivo global não possui destino fornecido. O alias `tentavip` não é ativado silenciosamente. Configure um destino existente conscientemente após revisar a navegação.
- Placeholders externos de LuckPerms, dex, objetivos, tokens e cash permanecem literais e são listados como pendentes. A versão1.0 oferece apenas seus placeholders básicos.
- `playerhead` é convertido explicitamente para `minecraft:player_head` com perfil do jogador.
- Nomes de arquivo de exemplos Molang com espaços/parênteses são normalizados e documentados; esses menus permanecem desativados.
- O arquivo `loja_vip.json.json` tem ID `loja_vip.json`, removendo somente o último sufixo. Isso é preservado, sem supor outro nome.
- O alias `help` conflita com o comando vanilla e não é registrado; o comando existente permanece disponível.
- Layouts de confirmação antigos são substituídos pelo seletor de quantidade exigido na1.0. Presets não usados e opções globais de som/clique não são tratados como lojas ou implementados como recursos avançados.

Os comandos de plugins/mods externos preservados nas ações continuam dependendo desses serviços. Preservar o texto `/perfil`, `/warp` ou `/kit` não implementa esses comandos no ZorykaiMenus.

**Não remover SkiesShop/SkiesGUIs da produção apenas porque a conversão gerou arquivos.** Há recursos usados fora do escopo1.0. A homologação deve verificar também os outros mods do servidor e decidir como tratar esses recursos. Não foi ampliado o escopo para implementar Molang ou os sistemas externos.
