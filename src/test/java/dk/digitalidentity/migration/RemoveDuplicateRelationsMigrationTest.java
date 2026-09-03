package dk.digitalidentity.migration;

import dk.digitalidentity.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the actual {@code V1_120__remove_duplicate_relations.sql} against the database with fixtures
 * planted, so the migration's effect is covered and not just its syntax. Flyway has already applied it
 * once at context startup, but on a database with nothing to clean up — re-running it is safe because
 * it only ever deletes rows it identifies as duplicates.
 * <p>
 * The ids are far above anything the shared TABLE generator hands out during tests, so the fixtures
 * cannot collide with rows created by other tests.
 */
public class RemoveDuplicateRelationsMigrationTest extends BaseIntegrationTest {
    private static final String MIGRATION = "db/migration/V1_120__remove_duplicate_relations.sql";

    private static final long TASK_ID = 900_001L;
    private static final long ASSET_ID = 900_002L;
    private static final long REGISTER_ID = 900_003L;
    /** Deliberately the same numeric id as the task, but a different type — a real id collision. */
    private static final long COLLIDING_SUPPLIER_ID = 900_001L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearFixtures() {
        jdbcTemplate.update("DELETE FROM relation_properties WHERE relation_id >= 800000");
        jdbcTemplate.update("DELETE FROM relations WHERE id >= 800000");
    }

    @Test
    void keepsOneRowPerPairAndLeavesUnrelatedRowsAlone() throws IOException {
        // Two rows for the same pair, the second with A/B reversed
        insertRelation(800_001L, "TASK", TASK_ID, "ASSET", ASSET_ID);
        insertRelation(800_002L, "ASSET", ASSET_ID, "TASK", TASK_ID);
        // A genuine, non-duplicated relation that must survive untouched
        insertRelation(800_003L, "REGISTER", REGISTER_ID, "ASSET", ASSET_ID);

        runMigration();

        // The newest of the duplicate pair is kept, because the denormalized names on it are the
        // freshest — see the migration's own reasoning.
        assertThat(remainingIds()).containsExactly(800_002L, 800_003L);
    }

    @Test
    void doesNotCollapseRelationsThatOnlyShareAnId() throws IOException {
        // Task 900001 and Supplier 900001 are different entities that happen to share an id, which is
        // possible because several Relatable tables have their own auto_increment PK. Both relations
        // are legitimate and must survive.
        insertRelation(800_010L, "TASK", TASK_ID, "ASSET", ASSET_ID);
        insertRelation(800_011L, "SUPPLIER", COLLIDING_SUPPLIER_ID, "ASSET", ASSET_ID);

        runMigration();

        assertThat(remainingIds()).containsExactly(800_010L, 800_011L);
    }

    @Test
    void keepsTheRowCarryingRelationPropertiesEvenWhenItIsTheOldest() throws IOException {
        insertRelation(800_020L, "REGISTER", REGISTER_ID, "ASSET", ASSET_ID);
        insertRelation(800_021L, "REGISTER", REGISTER_ID, "ASSET", ASSET_ID);
        // riskScale is the user's weighting of this asset in the register's risk assessment. It hangs
        // off the oldest row here, and ON DELETE CASCADE would take it with the row.
        insertRelationProperty(800_020L, "riskScale", "25");

        runMigration();

        assertThat(remainingIds()).containsExactly(800_020L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT prop_value FROM relation_properties WHERE relation_id = 800020", String.class))
                .isEqualTo("25");
    }

    @Test
    void leavesGroupAloneWhenSeveralRowsCarryProperties() throws IOException {
        insertRelation(800_030L, "REGISTER", REGISTER_ID, "ASSET", ASSET_ID);
        insertRelation(800_031L, "REGISTER", REGISTER_ID, "ASSET", ASSET_ID);
        // Two competing user-entered values — there is no safe way to pick one, so nothing is deleted.
        insertRelationProperty(800_030L, "riskScale", "25");
        insertRelationProperty(800_031L, "riskScale", "75");

        runMigration();

        assertThat(remainingIds()).containsExactly(800_030L, 800_031L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM relation_properties WHERE relation_id >= 800000", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void isIdempotentAndANoOpWithoutDuplicates() throws IOException {
        insertRelation(800_040L, "TASK", TASK_ID, "ASSET", ASSET_ID);
        insertRelation(800_041L, "TASK", TASK_ID, "ASSET", ASSET_ID);

        runMigration();
        final List<Long> afterFirstRun = remainingIds();
        runMigration();

        assertThat(remainingIds()).isEqualTo(afterFirstRun).hasSize(1);
    }

    private void runMigration() throws IOException {
        final String sql = new String(new ClassPathResource(MIGRATION).getContentAsByteArray(),
                StandardCharsets.UTF_8);
        jdbcTemplate.execute(sql);
    }

    private List<Long> remainingIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM relations WHERE id >= 800000 ORDER BY id", Long.class);
    }

    private void insertRelation(final long id, final String aType, final long aId,
            final String bType, final long bId) {
        jdbcTemplate.update("""
                INSERT INTO relations (id, relation_a_id, relation_a_type, relation_b_id, relation_b_type)
                VALUES (?, ?, ?, ?, ?)
                """, id, aId, aType, bId, bType);
    }

    private void insertRelationProperty(final long relationId, final String key, final String value) {
        jdbcTemplate.update("""
                INSERT INTO relation_properties (prop_key, prop_value, relation_id) VALUES (?, ?, ?)
                """, key, value, relationId);
    }
}
