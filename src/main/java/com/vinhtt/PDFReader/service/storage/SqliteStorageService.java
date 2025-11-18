package com.vinhtt.PDFReader.service.storage;

import com.vinhtt.PDFReader.model.Paragraph;
import com.vinhtt.PDFReader.model.Sentence;
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

            // Bảng Paragraphs
            stmt.execute("CREATE TABLE IF NOT EXISTS paragraphs (" +
                    "id INTEGER PRIMARY KEY," +
                    "original_text TEXT," +
                    "translated_text TEXT," +
                    "y_position REAL)");

            // Bảng Sentences (Lưu kết quả Analysis)
            stmt.execute("CREATE TABLE IF NOT EXISTS sentences (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "paragraph_id INTEGER," +
                    "original_text TEXT," +
                    "analysis_json TEXT," +
                    "FOREIGN KEY(paragraph_id) REFERENCES paragraphs(id))");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasData() {
        String sql = "SELECT count(*) FROM paragraphs";
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void saveParagraphs(List<Paragraph> paragraphs) {
        String sql = "INSERT INTO paragraphs(id, original_text, y_position) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(connectionUrl)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Paragraph p : paragraphs) {
                    pstmt.setInt(1, p.hashCode()); // ID dựa trên hashcode để đơn giản
                    pstmt.setString(2, p.getOriginalText());
                    pstmt.setFloat(3, 0);
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
    public void updateParagraphTranslation(int id, String translatedText) {
        String sql = "UPDATE paragraphs SET translated_text = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, translatedText);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Paragraph> getAllParagraphs() {
        List<Paragraph> list = new ArrayList<>();
        String sql = "SELECT * FROM paragraphs ORDER BY id ASC"; // Hoặc order theo Y nếu có
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Paragraph p = new Paragraph(
                        rs.getInt("id"),
                        rs.getString("original_text"),
                        rs.getFloat("y_position")
                );
                p.setTranslatedText(rs.getString("translated_text"));
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Sentence> getSentencesByParagraphId(int paragraphId) {
        List<Sentence> list = new ArrayList<>();
        String sql = "SELECT * FROM sentences WHERE paragraph_id = ?";
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, paragraphId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new Sentence(
                        rs.getInt("id"),
                        rs.getInt("paragraph_id"),
                        rs.getString("original_text"),
                        rs.getString("analysis_json")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void saveSentences(List<Sentence> sentences) {
        String sql = "INSERT INTO sentences(paragraph_id, original_text, analysis_json) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(connectionUrl)) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateSentenceAnalysis(int id, String analysisJson) {
        String sql = "UPDATE sentences SET analysis_json = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, analysisJson);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}