# Atualização1.0 →1.1

Pare o servidor e preserve uma cópia do mundo e da configuração. Troque somente o JAR de ZorykaiMenus por zorykaimenus-1.1.0.jar, mantendo Mod ID, Fabric/Minecraft1.21.1 e Java21. Não instale os JARs checks/example em produção.

Ao iniciar, apenas discounts.json ausente é criado com os quatro grupos iniciais. As configurações1.0, seus IDs, preços, componentes, slots, permissões, navegação e migração continuam válidos. Nenhum menu precisa ficar dentro de migrated: a leitura recursiva existente continua aceitando arquivos diretamente em menus/shops, sem cópias duplicadas.

Novos campos são opcionais: discount, category, maxQuantity por produto, available, conditions e config.cobblemon. Não existe reescrita em massa de arquivos. O importadorSkies e seus backups/hashes são preservados.

Execute /zmenu validate e /zmenu reload depois de editar regras. Permissões de desconto são atribuídas no LuckPerms; sem concessão válida, o jogador paga o preço integral. Revise o teto e as exclusões de produtos antes de liberar VIP.

Logs automáticos ficam em config/zorykaimenus/logs/erros.log. Registram WARN/ERROR do mod e exceções relevantes com stack trace, com5MB por arquivo e até5 anteriores compactados, além do arquivo ativo. O console permanece funcionando. Erro ao criar o destino usa o console como fallback. Ausência esperada de integração opcional não é falha financeira.

Operações pendentes da1.0 mantêm o schema do journal e a política de recuperação. Não apague internal/pending.sqlite para desbloquear pagamentos. Eventos novos derivados de registros antigos assumem desconto0 se o campo não existir. Eventos de sucesso não substituem a recuperação nem fornecem transação distribuída atômica.

São mantidas as dependências e versões usadas na1.0. Cobblemon/LuckPerms são opcionais; catálogos que usam itens reais de um mod continuam precisando de seu registro. Text Placeholder API pode vir aninhada no Impactor; o teste sem Cobblemon não removeu essa dependência transitiva do provedor. Core/Dex seguem opcionais para seus placeholders.
