package dk.digitalidentity.wcag;

import dk.digitalidentity.TestContainersConfiguration;
import dk.digitalidentity.TestUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Dumper de gengivne sider til target/wcag-pages, så axe-core kan køres på dem uden en SAML-session.
 * Ikke en assertion-test - kør den med -Dwcag.dump=true når rapporten skal opdateres.
 */
@TestUser
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@ActiveProfiles({"test"})
@TestPropertySource("/application-test.properties")
@EnabledIfSystemProperty(named = "wcag.dump", matches = "true")
public class WcagPageDumpTest {

    private static final List<String> PAGES = List.of(
            "/dashboard", "/assets", "/registers", "/risks", "/tasks", "/documents",
            "/suppliers", "/standards", "/contacts", "/incidents", "/reports",
            "/settings", "/admin", "/admin/tags", "/admin/choicelists", "/catalogs",
            "/dbs/assets", "/dbs/oversight", "/kle", "/auditlog", "/dpia");

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void dumpPages() throws Exception {
        final Path out = Path.of("target", "wcag-pages");
        Files.createDirectories(out);
        final StringBuilder index = new StringBuilder();

        for (final String page : PAGES) {
            final String slug = page.substring(1).replace('/', '_');
            try {
                final MvcResult result = mockMvc.perform(get(page)).andReturn();
                final int status = result.getResponse().getStatus();
                final String body = result.getResponse().getContentAsString();
                if (status == 200 && body.contains("</html>")) {
                    Files.writeString(out.resolve(slug + ".html"), body, StandardCharsets.UTF_8);
                    index.append(slug).append('\t').append(page).append("\t200\n");
                } else {
                    index.append(slug).append('\t').append(page).append('\t').append(status).append("\tsprunget over\n");
                }
            } catch (final Exception e) {
                index.append(slug).append('\t').append(page).append("\tFEJL\t")
                        .append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n');
            }
        }
        Files.writeString(out.resolve("index.tsv"), index.toString(), StandardCharsets.UTF_8);
        System.out.println("WCAG-DUMP:\n" + index);
    }
}
