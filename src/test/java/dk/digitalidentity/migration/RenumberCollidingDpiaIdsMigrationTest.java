package dk.digitalidentity.migration;

import dk.digitalidentity.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the actual {@code V1_124__renumber_colliding_dpia_ids.sql} against planted collisions. Every
 * statement has to run on one connection - the migration keeps its working set in temporary tables
 * and the locked {@code next_val} in a user variable, both per-session - which is how Flyway does it.
 * The fixture ids sit above what the shared generator hands out during tests, and
 * {@code hibernate_sequences} is put back afterwards, because the migration raises it and the
 * container is shared.
 */
public class RenumberCollidingDpiaIdsMigrationTest extends BaseIntegrationTest {
    private static final String MIGRATION = "db/migration/V1_124__renumber_colliding_dpia_ids.sql";

    private static final long DPIA_WITH_INCIDENT = 990_001L;
    private static final long DPIA_WITH_ASSET = 990_002L;
    private static final long DPIA_WITH_USER_PROPERTY = 990_003L;
    private static final long UNTOUCHED_DPIA = 990_004L;
    private static final long UNRELATED_ASSET = 990_010L;
    private static final long TASK_LINKING_DPIA = 990_011L;
    private static final long TAG_ID = 990_012L;
    private static final long NEXT_VAL_BEFORE_MIGRATION = 990_000L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long nextValSnapshot;

    @BeforeEach
    void plantFixtures() {
        nextValSnapshot = readNextVal();
        deleteFixtures();

        insertRelatable("dpia", DPIA_WITH_INCIDENT, "DPIA", "Muni DDH-chatrobot");
        insertRelatable("dpia", DPIA_WITH_ASSET, "DPIA", "Danmarks Statistik");
        insertRelatable("dpia", DPIA_WITH_USER_PROPERTY, "DPIA", "DPIA med egen note");
        insertRelatable("dpia", UNTOUCHED_DPIA, "DPIA", "Fredelig DPIA");
        insertRelatable("incidents", DPIA_WITH_INCIDENT, "INCIDENT", "Oplysninger til forkert modtager");
        insertAsset(DPIA_WITH_ASSET, "Mit liv - min sundhed App");
        insertAsset(DPIA_WITH_USER_PROPERTY, "Aktiv med note");
        insertAsset(UNRELATED_ASSET, "Uskyldigt aktiv");
        insertRelatable("tasks", TASK_LINKING_DPIA, "TASK", "DPIA for Danmarks Statistik");

        insertProperty("kitos_uuid", "ef19fc75-66f3-4079-a4e9-65f1eb7b7e4e", DPIA_WITH_ASSET);
        insertProperty("kitos_usage_uuid", "af3082fa-424d-400e-9789-1df13ed4569d", DPIA_WITH_ASSET);
        // A key no code writes, so it cannot be attributed to either side of the collision
        insertProperty("telefon", "12345678", DPIA_WITH_USER_PROPERTY);
        insertProperty("linked_dpia", "" + DPIA_WITH_ASSET, TASK_LINKING_DPIA);

        insertRelation(990_100L, "INCIDENT", DPIA_WITH_INCIDENT, "ASSET", UNRELATED_ASSET);
        insertRelation(990_101L, "DPIA", DPIA_WITH_INCIDENT, "ASSET", UNRELATED_ASSET);
        insertRelation(990_102L, "ASSET", UNRELATED_ASSET, "DPIA", DPIA_WITH_ASSET);

        jdbcTemplate.update("INSERT INTO tags (id, value, color_hex_code, year_wheel) VALUES (?, 'GDPR', '#FF0000', 0)", TAG_ID);
        jdbcTemplate.update("INSERT INTO dpia_asset (dpia_id, asset_id) VALUES (?, ?)", DPIA_WITH_ASSET, UNRELATED_ASSET);
        jdbcTemplate.update("""
                INSERT INTO dpia_report (dpia_id, report_approver_uuid, report_approver_name, dpia_report_approval_status)
                VALUES (?, 'uuid-1', 'Godkender', 'READY')
                """, DPIA_WITH_ASSET);
        jdbcTemplate.update("INSERT INTO dpia_response_section (dpia_id, selected) VALUES (?, 1)", DPIA_WITH_ASSET);
        jdbcTemplate.update("INSERT INTO dpia_screening (dpia_id, conclusion) VALUES (?, 'Noedvendig')", DPIA_WITH_ASSET);
        jdbcTemplate.update("INSERT INTO dpia_tag (dpia_id, tag_id) VALUES (?, ?)", DPIA_WITH_ASSET, TAG_ID);

        writeNextVal(NEXT_VAL_BEFORE_MIGRATION);
    }

    @AfterEach
    void removeFixtures() {
        deleteFixtures();
        if (nextValSnapshot == null) {
            jdbcTemplate.update("DELETE FROM hibernate_sequences WHERE sequence_name = 'default'");
        } else {
            writeNextVal(nextValSnapshot);
        }
    }

    @Test
    void movesTheDpiaAndLeavesTheOtherSideOfTheCollisionAlone() throws IOException {
        runMigration();

        final long moved = dpiaIdByName("Muni DDH-chatrobot");
        assertThat(moved).isNotEqualTo(DPIA_WITH_INCIDENT);
        assertThat(idsIn("incidents")).contains(DPIA_WITH_INCIDENT);
        assertThat(idsIn("assets")).contains(DPIA_WITH_ASSET);
        assertThat(collidingIds()).doesNotContain(DPIA_WITH_INCIDENT, DPIA_WITH_ASSET);
    }

    @Test
    void dragsEveryReferenceAlong() throws IOException {
        runMigration();

        final long moved = dpiaIdByName("Danmarks Statistik");
        assertThat(moved).isNotEqualTo(DPIA_WITH_ASSET);
        assertThat(dpiaIdIn("dpia_asset")).isEqualTo(moved);
        assertThat(dpiaIdIn("dpia_report")).isEqualTo(moved);
        assertThat(dpiaIdIn("dpia_response_section")).isEqualTo(moved);
        assertThat(dpiaIdIn("dpia_screening")).isEqualTo(moved);
        assertThat(dpiaIdIn("dpia_tag")).isEqualTo(moved);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT relation_b_id FROM relations WHERE id = 990102", Long.class)).isEqualTo(moved);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT prop_value FROM properties WHERE prop_key = 'linked_dpia' AND entity_id = ?",
                String.class, TASK_LINKING_DPIA)).isEqualTo("" + moved);
        // Typed INCIDENT, so it points at the incident that shares the id and must not be rewritten
        assertThat(jdbcTemplate.queryForObject(
                "SELECT relation_a_id FROM relations WHERE id = 990100", Long.class))
                .isEqualTo(DPIA_WITH_INCIDENT);
        assertThat(jdbcTemplate.queryForList("""
                SELECT entity_id FROM properties WHERE prop_key IN ('kitos_uuid', 'kitos_usage_uuid')
                """, Long.class)).containsOnly(DPIA_WITH_ASSET);
    }

    @Test
    void leavesTheCollisionAloneWhenAPropertyCannotBeAttributed() throws IOException {
        runMigration();

        assertThat(dpiaIdByName("DPIA med egen note")).isEqualTo(DPIA_WITH_USER_PROPERTY);
        assertThat(collidingIds()).contains(DPIA_WITH_USER_PROPERTY);
    }

    @Test
    void leavesDpiasWithoutACollisionAlone() throws IOException {
        runMigration();

        assertThat(dpiaIdByName("Fredelig DPIA")).isEqualTo(UNTOUCHED_DPIA);
    }

    @Test
    void pushesTheGeneratorClearOfEveryIdInUse() throws IOException {
        runMigration();

        // The next block handed out is [next_val - 48 .. next_val + 1], so it may only start above
        // the highest id in use - otherwise a live row gets its id handed out again
        final long nextVal = readNextVal();
        assertThat(nextVal - 48).isGreaterThan(highestRelatableId());
    }

    @Test
    void isIdempotent() throws IOException {
        runMigration();
        final List<Long> afterFirstRun = idsIn("dpia");
        final long nextValAfterFirstRun = readNextVal();

        runMigration();

        assertThat(idsIn("dpia")).isEqualTo(afterFirstRun);
        assertThat(readNextVal()).isEqualTo(nextValAfterFirstRun);
    }

    private void runMigration() throws IOException {
        // Comments before the split: they hold semicolons, and splitting first tears one in half
        final String sql = new String(new ClassPathResource(MIGRATION).getContentAsByteArray(),
                StandardCharsets.UTF_8).replaceAll("--[^\n]*", "");
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                for (final String chunk : sql.split(";")) {
                    if (!chunk.isBlank()) {
                        statement.execute(chunk);
                    }
                }
            }
            return null;
        });
    }

    private void deleteFixtures() {
        jdbcTemplate.update("DELETE FROM dpia_tag WHERE dpia_id >= 990000");
        jdbcTemplate.update("DELETE FROM dpia_report WHERE dpia_id >= 990000");
        jdbcTemplate.update("DELETE FROM dpia_response_section WHERE dpia_id >= 990000");
        jdbcTemplate.update("DELETE FROM dpia_screening WHERE dpia_id >= 990000");
        jdbcTemplate.update("DELETE FROM dpia_asset WHERE dpia_id >= 990000");
        jdbcTemplate.update("DELETE FROM tags WHERE id >= 990000");
        jdbcTemplate.update("DELETE FROM relations WHERE id >= 990000");
        jdbcTemplate.update("DELETE FROM properties WHERE entity_id >= 990000");
        jdbcTemplate.update("DELETE FROM dpia WHERE id >= 990000");
        jdbcTemplate.update("DELETE FROM incidents WHERE id >= 990000");
        jdbcTemplate.update("DELETE FROM assets WHERE id >= 990000");
        jdbcTemplate.update("DELETE FROM tasks WHERE id >= 990000");
    }

    private void insertRelatable(final String table, final long id, final String type, final String name) {
        jdbcTemplate.update("""
                INSERT INTO %s (id, created_at, updated_at, created_by, name, relation_type, version)
                VALUES (?, '2025-06-12 10:00:00', '2025-06-12 10:00:00', 'test', ?, ?, 0)
                """.formatted(table), id, name, type);
    }

    private void insertAsset(final long id, final String name) {
        jdbcTemplate.update("""
                INSERT INTO assets (id, created_at, updated_at, created_by, name, relation_type, version,
                                    data_processing_agreement_status, asset_type)
                VALUES (?, '2025-06-16 09:00:00', '2025-06-16 09:00:00', 'test', ?, 'ASSET', 0, 'NOT_STARTED', 1)
                """, id, name);
    }

    private void insertProperty(final String key, final String value, final long entityId) {
        jdbcTemplate.update("INSERT INTO properties (prop_key, prop_value, entity_id) VALUES (?, ?, ?)",
                key, value, entityId);
    }

    private void insertRelation(final long id, final String aType, final long aId,
            final String bType, final long bId) {
        jdbcTemplate.update("""
                INSERT INTO relations (id, relation_a_id, relation_a_type, relation_b_id, relation_b_type)
                VALUES (?, ?, ?, ?, ?)
                """, id, aId, aType, bId, bType);
    }

    private long dpiaIdByName(final String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM dpia WHERE name = ?", Long.class, name);
    }

    private long dpiaIdIn(final String childTable) {
        return jdbcTemplate.queryForObject(
                "SELECT dpia_id FROM %s WHERE dpia_id >= 990000".formatted(childTable), Long.class);
    }

    private List<Long> idsIn(final String table) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM %s WHERE id >= 990000 ORDER BY id".formatted(table), Long.class);
    }

    private List<Long> collidingIds() {
        return jdbcTemplate.queryForList("""
                SELECT id FROM (SELECT id FROM dpia UNION ALL SELECT id FROM assets
                                UNION ALL SELECT id FROM incidents UNION ALL SELECT id FROM tasks) alle
                GROUP BY id HAVING COUNT(*) > 1
                """, Long.class);
    }

    private long highestRelatableId() {
        return jdbcTemplate.queryForObject("""
                SELECT MAX(id) FROM (SELECT id FROM dpia UNION ALL SELECT id FROM assets
                                     UNION ALL SELECT id FROM incidents UNION ALL SELECT id FROM tasks) alle
                """, Long.class);
    }

    private Long readNextVal() {
        return jdbcTemplate.query("SELECT next_val FROM hibernate_sequences WHERE sequence_name = 'default'",
                rs -> rs.next() ? rs.getLong(1) : null);
    }

    private void writeNextVal(final long value) {
        final int updated = jdbcTemplate.update(
                "UPDATE hibernate_sequences SET next_val = ? WHERE sequence_name = 'default'", value);
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO hibernate_sequences (sequence_name, next_val) VALUES ('default', ?)", value);
        }
    }
}
