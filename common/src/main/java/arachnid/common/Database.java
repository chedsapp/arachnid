package arachnid.common;

import java.lang.AutoCloseable;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class Database implements AutoCloseable {
    private final Connection conn;

    public Database(Path file) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA foreign_keys=ON");
            st.execute("PRAGMA busy_timeout=5000");
        }
        migrate();
    }

    private void migrate() throws SQLException {
        conn.setAutoCommit(true);
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS schema_version (" +
                    "version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
        }
        int current;
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT COALESCE(MAX(version),0) FROM schema_version")) {
            rs.next();
            current = rs.getInt(1);
        }
        for (int v = current + 1;; v++) {
            String resource = "/db/migrations/V" + v + "__init.sql";
            InputStream in = getClass().getResourceAsStream(resource);
            if (in == null)
                break;

            String sql;
            try (in) {
                sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                // Don't mark a migration as applied if we couldn't even read it
                throw new SQLException("Could not read migration " + resource, e);
            }

            // Run each migration in a transaction so a failure can't leave
            // the schema half-updated
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
                st.executeUpdate("INSERT INTO schema_version VALUES (" +
                        v + ", datetime('now'))");
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Connection connection() {
        return conn;
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }
}
