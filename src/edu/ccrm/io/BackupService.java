package edu.ccrm.io;

import edu.ccrm.config.AppConfig;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class BackupService {

    private final AppConfig config = AppConfig.getInstance();

    public void performBackup() throws IOException {
        Path sourceDir = config.getDataDirectory();
        if (!Files.exists(sourceDir)) {
            System.out.println("Data directory does not exist. Nothing to backup.");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path backupTargetDir = config.getBackupDirectory().resolve("backup_" + timestamp);
        Files.createDirectories(backupTargetDir);

        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.forEach(sourcePath -> {
                try {
                    Path destinationPath = backupTargetDir.resolve(sourceDir.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(destinationPath)) {
                            Files.createDirectory(destinationPath);
                        }
                    } else {
                        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to backup file: " + sourcePath + " - " + e.getMessage());
                }
            });
        }
        System.out.println("Backup completed successfully to: " + backupTargetDir);
    }
}