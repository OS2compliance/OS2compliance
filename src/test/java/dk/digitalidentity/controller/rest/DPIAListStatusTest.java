package dk.digitalidentity.controller.rest;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The grid reads the status cell as {@code reportApprovalStatus}, so the DTO has to expose it under
 * that name - and a DPIA that was never sent for signing has no {@code dpia_report} row at all.
 */
@Transactional
@AutoConfigureMockMvc
public class DPIAListStatusTest extends BaseIntegrationTest {
    private static final String TEST_USER_UUID = "ff6fc101-aeb2-486e-8d39-5d8e718abdec";
    private static final long DPIA_NEVER_SENT = 980_001L;
    private static final long DPIA_WAITING = 980_002L;
    private static final long DPIA_REPORT_ID = 980_003L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserDao userDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        userDao.save(User.builder()
                .active(true)
                .uuid(TEST_USER_UUID)
                .userId("kbp")
                .name("Kaspar Bach Pedersen")
                .build());

        insertDpia(DPIA_NEVER_SENT, "Aldrig sendt til signering");
        insertDpia(DPIA_WAITING, "Sendt til signering");
        jdbcTemplate.update("""
                INSERT INTO dpia_report (id, dpia_id, report_approver_uuid, report_approver_name, dpia_report_approval_status)
                VALUES (?, ?, ?, 'Kaspar Bach Pedersen(kbp)', 'WAITING')
                """, DPIA_REPORT_ID, DPIA_WAITING, TEST_USER_UUID);
    }

    @Test
    public void statusIsExposedUnderTheKeyTheGridReads() throws Exception {
        mockMvc.perform(post("/rest/dpia/list").with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)].reportApprovalStatus".formatted(DPIA_WAITING))
                        .value("Afventer"));
    }

    @Test
    public void neverSentDpiaReportsNotSent() throws Exception {
        mockMvc.perform(post("/rest/dpia/list").with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)].reportApprovalStatus".formatted(DPIA_NEVER_SENT))
                        .value("Ikke sendt"));
    }

    private void insertDpia(final long id, final String name) {
        jdbcTemplate.update("""
                INSERT INTO dpia (id, created_at, updated_at, created_by, name, relation_type, version)
                VALUES (?, '2026-08-25 10:00:00', '2026-08-25 10:00:00', 'test', ?, 'DPIA', 0)
                """, id, name);
    }
}
