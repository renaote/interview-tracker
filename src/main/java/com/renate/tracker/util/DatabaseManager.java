package com.renate.tracker.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Handles connecting to the database and making sure the table exists.
// I keep the .db file in a "data" folder right next to the app, so
// there's nothing to install or configure - it just works on first run.
public class DatabaseManager {

    private static final String DB_FOLDER = "data";
    private static final String DB_FILE = "tracker.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FOLDER + "/" + DB_FILE;

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // Runs once when the app starts. Creates the data folder and the
    // table if they don't already exist.
    public void initSchema() {
        try {
            Files.createDirectories(Path.of(DB_FOLDER));
        } catch (IOException e) {
            throw new RuntimeException("Could not create data folder", e);
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String schemaSql = readSchemaResource();
            for (String statement : schemaSql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set up the database", e);
        }
    }

    // Reads schema.sql out of the resources folder as plain text
    private String readSchemaResource() {
        try (InputStream is = getClass().getResourceAsStream("/db/schema.sql")) {
            if (is == null) {
                throw new RuntimeException("schema.sql not found");
            }
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read schema.sql", e);
        }
    }
}