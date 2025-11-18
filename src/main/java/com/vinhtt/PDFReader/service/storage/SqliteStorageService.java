package com.vinhtt.PDFReader.service.storage;

import com.vinhtt.PDFReader.model.Paragraph;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteStorageService implements IStorageService {
    private String connectionUrl;

    @Override
    public void initDatabase(String dbPath) {
        connectionUrl = "jdbc:sqlite:" + dbPath;
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS paragraphs (" +
                    "id INTEGER PRIMARY KEY," +
                    "original_text TEXT," +
                    "translated_text TEXT," +
                    "y_position REAL)";
            stmt.execute(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveParagraphs(List<Paragraph> paragraphs) {
        String sql = "INSERT INTO paragraphs(id, original_text, y_position) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(connectionUrl)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Paragraph p : paragraphs) {
                    pstmt.setInt(1, p.hashCode()); // Simplified ID for demo
                    pstmt.setString(2, p.getOriginalText());
                    pstmt.setFloat(3, 0); // Simplified Y
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateTranslation(int id, String translatedText) {
        // Logic update DB here (omitted for brevity in demo, but crucial for production)
    }

    @Override
    public List<Paragraph> getParagraphs() {
        // Return empty list for now to force re-parse in demo or implement SELECT
        return new ArrayList<>();
    }
}