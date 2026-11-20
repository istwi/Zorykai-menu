package com.zorykai.menus;

import com.zorykai.menus.migration.LegacyImporter;
import com.zorykai.menus.migration.MigrationBackup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MigrationBackupTest {
    @TempDir Path temporary;
    private Map<String, String> fixture(Path source) throws Exception {
        Files.createDirectories(source.resolve("shops"));
        Files.writeString(source.resolve("config.json"), "{\"currency\":\"impactor:dollars\"}");
        Files.writeString(source.resolve("shops/shop.json"), "{\"buy\":\"75000\"}");
        return Map.of("config.json", LegacyImporter.hash(source.resolve("config.json")),
                "shops/shop.json", LegacyImporter.hash(source.resolve("shops/shop.json")));
    }
    @Test void completesPartialBackupAndCanRepeat() throws Exception {
        Path source=temporary.resolve("source"), backup=temporary.resolve("backup");
        var hashes=fixture(source);
        Files.createDirectories(backup);
        Files.copy(source.resolve("config.json"), backup.resolve("config.json"));
        MigrationBackup.verifyOrComplete(source, backup, hashes);
        MigrationBackup.verifyOrComplete(source, backup, hashes);
        for(var entry:hashes.entrySet()) assertEquals(entry.getValue(),LegacyImporter.hash(backup.resolve(entry.getKey())));
    }
    @Test void refusesToOverwriteDivergentBackup() throws Exception {
        Path source=temporary.resolve("source"), backup=temporary.resolve("backup");
        var hashes=fixture(source);Files.createDirectories(backup);
        Files.writeString(backup.resolve("config.json"),"preserve this evidence");
        assertThrows(Exception.class,()->MigrationBackup.verifyOrComplete(source,backup,hashes));
        assertEquals("preserve this evidence",Files.readString(backup.resolve("config.json")));
    }
    @Test void detectsSourceChangesAndAdditionalFiles() throws Exception {
        Path source=temporary.resolve("source"), backup=temporary.resolve("backup");
        var hashes=fixture(source);Files.writeString(source.resolve("shops/extra.json"),"{}");
        assertThrows(Exception.class,()->MigrationBackup.verifyOrComplete(source,backup,hashes));
        Files.delete(source.resolve("shops/extra.json"));
        Files.writeString(source.resolve("config.json"),"changed");
        assertThrows(Exception.class,()->MigrationBackup.verifyOrComplete(source,backup,hashes));
    }
}
