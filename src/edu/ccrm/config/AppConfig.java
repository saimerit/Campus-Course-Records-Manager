package edu.ccrm.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AppConfig {
    private static AppConfig instance;
    private final Path dataDirectory;
    private final Path backupDirectory;

    private AppConfig() {
        this.dataDirectory = Paths.get("app-data");
        this.backupDirectory = Paths.get("backups");
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Path getBackupDirectory() {
        return backupDirectory;
    }

    public Path getStudentsFilePath() {
        return dataDirectory.resolve("students.csv");
    }

    public Path getCoursesFilePath() {
        return dataDirectory.resolve("courses.csv");
    }

    public Path getInstructorsFilePath() {
        return dataDirectory.resolve("instructors.csv");
    }

    public Path getEnrollmentsFilePath() {
        return dataDirectory.resolve("enrollments.csv");
    }
}
