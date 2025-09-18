package edu.ccrm.io;

import edu.ccrm.config.AppConfig;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupService {

    public void performBackup() throws IOException {
        Path backupDir = AppConfig.getInstance().getBackupDirectory();
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
            System.out.println("Backup directory created at: " + backupDir);
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path backupFile = backupDir.resolve("ccrm-backup-" + timestamp + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(backupFile))) {
            Path dataDir = Path.of("app-data"); 
            try (Stream<Path> paths = Files.walk(dataDir)) {
                paths.filter(path -> !Files.isDirectory(path)).forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(dataDir.relativize(path).toString());
                    try {
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        System.err.println("Could not add file to zip: " + path);
                    }
                });
            }
            System.out.println("Backup created successfully at " + backupFile);
        }
    }
}