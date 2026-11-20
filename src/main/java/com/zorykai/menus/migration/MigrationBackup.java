package com.zorykai.menus.migration;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

/** An interrupted backup is resumed and verified before conversion files are written. */
public final class MigrationBackup {
    private MigrationBackup() {}

    public static void verifyOrComplete(Path source, Path backup, Map<String, String> hashes) throws Exception {
        Files.createDirectories(backup);
        for (var entry : hashes.entrySet()) {
            Path relative = Path.of(entry.getKey());
            if (relative.isAbsolute() || relative.normalize().startsWith("..")) {
                throw new IOException("Caminho de backup inválido: " + relative);
            }
            Path original = source.resolve(relative);
            Path destination = backup.resolve(relative);
            if (!LegacyImporter.hash(original).equals(entry.getValue())) {
                throw new IOException("Origem alterada durante a migração: " + relative);
            }
            Files.createDirectories(destination.getParent());
            if (!Files.exists(destination)) {
                Path temporary = Files.createTempFile(destination.getParent(), ".backup-", ".tmp");
                try {
                    Files.copy(original, temporary, StandardCopyOption.REPLACE_EXISTING);
                    if (!LegacyImporter.hash(temporary).equals(entry.getValue())) {
                        throw new IOException("Cópia de backup divergente: " + relative);
                    }
                    Files.move(temporary, destination);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
            if (!LegacyImporter.hash(destination).equals(entry.getValue())) {
                throw new IOException("Backup existente divergente; arquivo preservado: " + relative);
            }
        }
        // Recheck the complete source set: a removed or added file invalidates this fingerprint.
        try (var files = Files.walk(source)) {
            var actual = files.filter(Files::isRegularFile).toList();
            if (actual.size() != hashes.size()) throw new IOException("Arquivos da origem mudaram durante o backup");
            for (Path file : actual) {
                String expected = hashes.get(source.relativize(file).toString());
                if (expected == null || !LegacyImporter.hash(file).equals(expected)) {
                    throw new IOException("Origem alterada durante o backup: " + source.relativize(file));
                }
            }
        }
    }
}
