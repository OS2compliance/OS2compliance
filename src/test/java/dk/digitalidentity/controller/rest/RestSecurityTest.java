package dk.digitalidentity.controller.rest;

import dk.digitalidentity.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfiguration.class)
@ActiveProfiles({"test", "locallogin"})
@TestPropertySource("/application-test.properties")
public class RestSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void failsWhenUnauthenticated() throws Exception {
        assertUrlProtected("/rest/assets");
        assertUrlProtected("/rest/choice-lists");
        assertUrlProtected("/rest/cvr");
        assertUrlProtected("/rest/documents");
        assertUrlProtected("/rest/kitos");
        assertUrlProtected("/rest/measures");
        assertUrlProtected("/rest/ous");
        assertUrlProtected("/rest/registers");
        assertUrlProtected("/rest/relatable");
        assertUrlProtected("/rest/risks");
        assertUrlProtected("/rest/standards");
        assertUrlProtected("/rest/suppliers");
        assertUrlProtected("/rest/tasks");
        assertUrlProtected("/rest/users");
    }


    private void assertUrlProtected(final String url) throws Exception {
        mockMvc.perform(get(url))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }
}
