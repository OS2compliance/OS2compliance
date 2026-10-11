package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Tabeller oprettet uden eksplicit charset arvede serverens default (utf8mb3 på nogle drift-clustre).
public class V1_139__convert_tables_to_utf8mb4 extends BaseJavaMigration {
	private static final String TARGET = "CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci";
	// DDL committer implicit, så droppede FK'er gemmes her og genskabes ved en genkørsel efter fejl.
	private static final String FK_BACKUP = "utf8mb4_migration_fk_backup";

	private record ForeignKey(String table, String name) {}

	@Override
	public void migrate(Context context) throws SQLException {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.execute("ALTER DATABASE " + TARGET);

			List<String> tables = column(statement, """
					SELECT DISTINCT t.table_name
					FROM information_schema.tables t
					LEFT JOIN information_schema.columns c
					  ON c.table_schema = t.table_schema AND c.table_name = t.table_name
					WHERE t.table_schema = DATABASE()
					  AND t.table_type = 'BASE TABLE'
					  AND t.table_name NOT IN ('flyway_schema_history', '%s')
					  AND (t.table_collation <> 'utf8mb4_danish_ci' OR c.collation_name <> 'utf8mb4_danish_ci')
					""".formatted(FK_BACKUP));
			statement.execute("CREATE TABLE IF NOT EXISTS " + FK_BACKUP
					+ " (table_name VARCHAR(64) NOT NULL, constraint_name VARCHAR(64) NOT NULL, definition TEXT NOT NULL,"
					+ " PRIMARY KEY (table_name, constraint_name)) " + TARGET);

			// Fremmednøgler på tekstkolonner blokerer konverteringen; FOREIGN_KEY_CHECKS=0 undgås, da Galera ikke er garanteret at replikere det med DDL'en.
			if (!tables.isEmpty()) {
				String inList = "'" + String.join("','", tables) + "'";
				try (ResultSet rs = statement.executeQuery("""
						SELECT k.table_name, k.constraint_name,
						       CONCAT('FOREIGN KEY (',
						              GROUP_CONCAT(CONCAT('`', k.column_name, '`') ORDER BY k.ordinal_position),
						              ') REFERENCES `', k.referenced_table_name, '` (',
						              GROUP_CONCAT(CONCAT('`', k.referenced_column_name, '`') ORDER BY k.ordinal_position),
						              ') ON DELETE ', r.delete_rule, ' ON UPDATE ', r.update_rule)
						FROM information_schema.key_column_usage k
						JOIN information_schema.referential_constraints r
						  ON r.constraint_schema = k.constraint_schema AND r.constraint_name = k.constraint_name AND r.table_name = k.table_name
						JOIN information_schema.columns c
						  ON c.table_schema = k.table_schema AND c.table_name = k.table_name AND c.column_name = k.column_name
						WHERE k.table_schema = DATABASE()
						  AND k.referenced_table_name IS NOT NULL
						  AND (k.table_name IN (%s) OR k.referenced_table_name IN (%s))
						GROUP BY k.table_name, k.constraint_name, k.referenced_table_name, r.delete_rule, r.update_rule
						HAVING MAX(c.collation_name) IS NOT NULL
						""".formatted(inList, inList));
				     PreparedStatement backup = connection.prepareStatement(
						     "INSERT IGNORE INTO " + FK_BACKUP + " (table_name, constraint_name, definition) VALUES (?, ?, ?)")) {
					while (rs.next()) {
						backup.setString(1, rs.getString(1));
						backup.setString(2, rs.getString(2));
						backup.setString(3, rs.getString(3));
						backup.executeUpdate();
					}
				}
			}

			Map<ForeignKey, String> foreignKeys = new LinkedHashMap<>();
			try (ResultSet rs = statement.executeQuery("SELECT table_name, constraint_name, definition FROM " + FK_BACKUP)) {
				while (rs.next()) {
					foreignKeys.put(new ForeignKey(rs.getString(1), rs.getString(2)), rs.getString(3));
				}
			}

			for (ForeignKey fk : foreignKeys.keySet()) {
				if (foreignKeyExists(connection, fk)) {
					statement.execute("ALTER TABLE `" + fk.table() + "` DROP FOREIGN KEY `" + fk.name() + "`");
				}
			}
			for (String table : tables) {
				statement.execute("ALTER TABLE `" + table + "` CONVERT TO " + TARGET);
			}
			for (Map.Entry<ForeignKey, String> fk : foreignKeys.entrySet()) {
				if (!foreignKeyExists(connection, fk.getKey())) {
					statement.execute("ALTER TABLE `" + fk.getKey().table() + "` ADD CONSTRAINT `" + fk.getKey().name() + "` " + fk.getValue());
				}
			}

			statement.execute("DROP TABLE " + FK_BACKUP);
		}
	}

	private static boolean foreignKeyExists(Connection connection, ForeignKey fk) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("""
				SELECT 1 FROM information_schema.referential_constraints
				WHERE constraint_schema = DATABASE() AND table_name = ? AND constraint_name = ?
				""")) {
			ps.setString(1, fk.table());
			ps.setString(2, fk.name());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	private static List<String> column(Statement statement, String sql) throws SQLException {
		List<String> result = new ArrayList<>();
		try (ResultSet rs = statement.executeQuery(sql)) {
			while (rs.next()) {
				result.add(rs.getString(1));
			}
		}
		return result;
	}
}
