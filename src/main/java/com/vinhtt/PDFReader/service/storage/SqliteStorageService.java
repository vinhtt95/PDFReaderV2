package com.vinhtt.PDFReader.service.storage;

import com.vinhtt.PDFReader.model.Paragraph;
import com.vinhtt.PDFReader.model.Sentence;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteStorageService implements IStorageService {
    private Connection connection;

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database not initialized");
        }
        return connection;
    }

    @Override
    public void initDatabase(String dbPath) {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement stmt = connection.createStatement()) {
                // Thêm cột page_index vào bảng paragraphs
                stmt.execute("CREATE TABLE IF NOT EXISTS paragraphs (" +
                        "id INTEGER PRIMARY KEY," +
                        "page_index INTEGER," + // Cột mới
                        "original_text TEXT," +
                        "translated_text TEXT," +
                        "y_position REAL)");

                stmt.execute("CREATE TABLE IF NOT EXISTS sentences (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "paragraph_id INTEGER," +
                        "original_text TEXT," +
                        "analysis_json TEXT," +
                        "FOREIGN KEY(paragraph_id) REFERENCES paragraphs(id))");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public boolean hasData() {
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM paragraphs")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public void saveParagraphs(List<Paragraph> paragraphs) {
        String sql = "INSERT INTO paragraphs(id, page_index, original_text, y_position) VALUES(?,?,?,?)";
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Paragraph p : paragraphs) {
                    pstmt.setInt(1, p.getId());
                    pstmt.setInt(2, p.getPageIndex()); // Lưu page index
                    pstmt.setString(3, p.getOriginalText());
                    pstmt.setFloat(4, p.getYPosition());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateParagraphTranslation(int id, String translatedText) {
        try (PreparedStatement pstmt = getConnection().prepareStatement(
                "UPDATE paragraphs SET translated_text = ? WHERE id = ?")) {
            pstmt.setString(1, translatedText);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<Paragraph> getAllParagraphs() {
        List<Paragraph> list = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM paragraphs ORDER BY id ASC")) {
            while (rs.next()) {
                Paragraph p = new Paragraph(
                        rs.getInt("id"),
                        rs.getInt("page_index"), // Load page index
                        rs.getString("original_text"),
                        rs.getFloat("y_position")
                );
                p.setTranslatedText(rs.getString("translated_text"));
                list.add(p);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ... (Giữ nguyên các method Sentences và close() như cũ)
    @Override
    public List<Sentence> getSentencesByParagraphId(int paragraphId) {
        List<Sentence> list = new ArrayList<>();
        String sql = "SELECT * FROM sentences WHERE paragraph_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, paragraphId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Sentence(rs.getInt("id"), rs.getInt("paragraph_id"),
                        rs.getString("original_text"), rs.getString("analysis_json")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public void saveSentences(List<Sentence> sentences) {
        String sql = "INSERT INTO sentences(paragraph_id, original_text, analysis_json) VALUES(?,?,?)";
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Sentence s : sentences) {
                    pstmt.setInt(1, s.getParagraphId());
                    pstmt.setString(2, s.getOriginal());
                    pstmt.setString(3, s.getAnalysis());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateSentenceAnalysis(int id, String analysisJson) {
        try (PreparedStatement pstmt = getConnection().prepareStatement(
                "UPDATE sentences SET analysis_json = ? WHERE id = ?")) {
            pstmt.setString(1, analysisJson);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException e) { e.printStackTrace(); }
    }
}