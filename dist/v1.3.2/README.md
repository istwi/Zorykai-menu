# Hotfix Windows — 1.3.2

O log enviado mostra `AccessDeniedException` no `FileChannel.open` do diretório `world/pokemon/pcstore/0c`. A exceção ocorre depois da gravação/sincronização do arquivo e da troca atômica, ao tentar fsync do diretório. O Windows não permite essa abertura de diretório pela API Java usada. Não indica, por si só, falta de acesso ao arquivo do PC.

A 1.3.2 mantém a validação da serialização, `force(true)` do arquivo temporário e substituição atômica. Só a sincronização adicional do diretório é omitida no Windows; em Unix continua obrigatória. Erros reais na gravação do arquivo continuam bloqueando o pagamento. A durabilidade contra queda de energia segue as garantias de sistema/armazenamento; não se promete fsync de diretório no Windows.

## Atualização e operação pendente

1. Pare o servidor, guarde backup do mundo e de `config/zorykaimenus`, incluindo o journal SQLite completo.
2. Substitua somente o JAR ZorykaiMenus pela1.3.2 e reinicie.
3. O jogador afetado pode usar `/zmenu recover`. O estado `POKEMON_REMOVE_INTENT` do log é anterior ao pedido de crédito; a recuperação confere a localização/IDs e salva a retirada antes de pagar. Não precisa vender outro Pokémon para recuperar essa operação.
4. Não apague `internal/pending.sqlite`, registros pendentes ou arquivos de Pokémon. Se a recuperação acusar outra inconsistência, preserve o estado para conferência.

Os preços, cota diária e modelos do menu não foram alterados neste hotfix.

## Erro separado de Spawn

O `shirokai-spawn-1.1.0-zorykai-visual.jar` recusou o nome `0c56dd85-8da6-3572-8952-b8a09f6c2dc9-11523583845133269304.dat`: ele tem sufixo numérico adicional, não apenas UUID. Isso desativa o teleporte automático daquele mod e não é corrigido pelo JAR de menus.

Com o servidor parado, localize esse arquivo, faça backup e mova apenas ele para fora da pasta examinada pelo Spawn, mantendo também backup do arquivo UUID normal. Não renomeie o arquivo com sufixo por cima do UUID normal. Reinicie depois de retirar o arquivo irregular da varredura. O log não informa o diretório examinado pelo Spawn, portanto a localização exata deve ser confirmada no servidor.

Validação: build e51 testes unitários;16 verificações nativas de regressão em Linux/Cobblemon/Impactor, jogadores simulados. Não foi possível executar em Windows neste ambiente. SHA256: `79b842ba8ec7c992ee9341edb0bbe701e1bdb9fb750705480c4b89d3ae09efaf`.
