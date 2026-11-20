# Dependências e referências

APIs verificadas nos artefatos reais usados pelo build/runtime:

- Minecraft1.21.1, Yarn1.21.1+build.3 e FabricAPI0.116.6+1.21.1: inventários vanilla, ScreenHandler, eventos Fabric, registros, componentes ItemStack.CODEC e salvamento de jogador.
- Impactor5.3.5+1.21.1, publicação oficial fixada em `evidence/impactor-version.json`: https://modrinth.com/mod/impactor ; https://github.com/NickImpact/Impactor . Metadados do JAR declaram MIT. Usam-se EconomyService, Currency, Account, EconomyTransaction e persistência assíncrona. O JAR não é incorporado ao artefato final.
- LuckPermsAPI5.4 (compileOnly), opcional no servidor: https://luckperms.net/ ; https://luckperms.net/wiki/Developer-API . Consulta de tristate por usuário. Não é incorporado ao artefato final.
- Adventure MiniMessage/serializador Gson4.17.0: https://docs.advntr.dev/minimessage/ ; já fornecidos pelo Impactor usado no runtime. Preserve essa versão do provedor para a combinação homologada.
- SQLite JDBC3.49.1.0, incluído no JAR como dependência Fabric aninhada: https://github.com/xerial/sqlite-jdbc . Licenças próprias incluídas no artefato upstream.
- Fabric Loom1.10.5, Gradle8.14.3, JUnit5.12.2 usados nas ferramentas de compilação/teste.
- Cobblemon1.8.3 é um provedor opcional de itens registrados; não é dependência de compilação e nenhuma implementação de Pokémon foi copiada. Os componentes de itens vêm das configurações fornecidas e são interpretados pelo próprio registro/codec instalado.

SkiesShop/SkiesGUIs: apenas dados de configuração e metadados fornecidos pelo usuário foram consultados para compatibilidade. SkiesGUIs declara All-Rights-Reserved; SkiesShop fornecido não declara licença no fabric.mod.json inspecionado. Não incorporar seus JARs, classes, texturas ou implementação neste projeto.
