package com.renate.tracker.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.renate.tracker.model.Company;
import com.renate.tracker.model.Stage;
import com.renate.tracker.util.DatabaseManager;

// This class talks to the database. All my add/edit/delete/search
// logic lives here so the rest of the app doesn't need to know any SQL.
public class CompanyDAO {

    private final DatabaseManager db;

    public CompanyDAO(DatabaseManager db) {
        this.db = db;
    }

    // Adds a brand new application to the database
    public void addCompany(Company c) {
        String sql = """
            INSERT INTO company (name, role_title, stage, deadline, notes, application_url)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, c);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not add company", e);
        }
    }

    // Saves changes to an application that already exists
    public void updateCompany(Company c) {
        String sql = """
            UPDATE company
            SET name = ?, role_title = ?, stage = ?, deadline = ?, notes = ?,
                application_url = ?, updated_at = datetime('now')
            WHERE id = ?
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, c);
            ps.setInt(7, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not update company", e);
        }
    }

    // Removes an application completely
    public void deleteCompany(int id) {
        String sql = "DELETE FROM company WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete company", e);
        }
    }

    // Gets every application, soonest deadline first
    public List<Company> getAllCompanies() {
        String sql = "SELECT * FROM company ORDER BY deadline IS NULL, deadline ASC";
        List<Company> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not fetch companies", e);
        }
        return results;
    }

    // Finds applications matching a search word, and optionally a stage too
    public List<Company> searchCompanies(String keyword, Stage stageFilter) {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM company WHERE (name LIKE ? OR role_title LIKE ?)");
        if (stageFilter != null) {
            sql.append(" AND stage = ?");
        }
        sql.append(" ORDER BY deadline IS NULL, deadline ASC");

        List<Company> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            if (stageFilter != null) {
                ps.setString(3, stageFilter.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not search companies", e);
        }
        return results;
    }

    // Counts how many applications are at each stage - powers the dashboard
    public java.util.Map<Stage, Integer> getStageCounts() {
        java.util.Map<Stage, Integer> counts = new java.util.EnumMap<>(Stage.class);
        for (Stage s : Stage.values()) counts.put(s, 0);

        String sql = "SELECT stage, COUNT(*) as cnt FROM company GROUP BY stage";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                counts.put(Stage.valueOf(rs.getString("stage")), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not count stages", e);
        }
        return counts;
    }

    // Helper so I'm not repeating the same 6 lines in add and update
    private void bind(PreparedStatement ps, Company c) throws SQLException {
        ps.setString(1, c.getName());
        ps.setString(2, c.getRoleTitle());
        ps.setString(3, c.getStage().name());
        ps.setString(4, c.getDeadline() != null ? c.getDeadline().toString() : null);
        ps.setString(5, c.getNotes());
        ps.setString(6, c.getApplicationUrl());
    }

    // Turns one row from the database into a Company object
    private Company mapRow(ResultSet rs) throws SQLException {
        Company c = new Company();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setRoleTitle(rs.getString("role_title"));
        c.setStage(Stage.valueOf(rs.getString("stage")));
        String deadline = rs.getString("deadline");
        c.setDeadline(deadline != null ? LocalDate.parse(deadline) : null);
        c.setNotes(rs.getString("notes"));
        c.setApplicationUrl(rs.getString("application_url"));
        return c;
    }
}