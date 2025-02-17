package dk.digitalidentity.controller.rest;


import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link dk.digitalidentity.controller.rest.Admin.UserRestController}
 */
@Transactional
@AutoConfigureMockMvc
public class UserRestControllerTest extends BaseIntegrationTest {
    @Autowired
    private UserDao userDao;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        createUser("ff6fc101-aeb2-486e-8d39-5d8e718abdec", "kbp", "Kaspar Bach Pedersen");
        createUser("ccd731da-a468-412d-9fdf-820fdc1dac03", "jts", "Jeg Test Svendsen");
        createUser("56b1e7a1-7b8c-400c-beb6-0abc0c89ad86", "jjo", "Jonna Jonasen");
        createUser("e20749d3-a292-42e0-92fe-ed58e55a4a1a", "ååø", "Åse Åsesen Østerby");
    }

    @Test
    public void emptySearchReturnsAll() throws Exception {
        mockMvc.perform(get("/rest/users/autocomplete")
                        .queryParam("search", "")
                        .queryParam("id", "result"))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(content()
                        .json("""
                                {
                                    "totalCount": 4,
                                    "content": [
                                        {
                                            "uuid": "56b1e7a1-7b8c-400c-beb6-0abc0c89ad86",
                                            "userId": "jjo",
                                            "name": "Jonna Jonasen"
                                        },
                                        {
                                            "uuid": "ccd731da-a468-412d-9fdf-820fdc1dac03",
                                            "userId": "jts",
                                            "name": "Jeg Test Svendsen"
                                        },
                                        {
                                            "uuid": "e20749d3-a292-42e0-92fe-ed58e55a4a1a",
                                            "userId": "ååø",
                                            "name": "Åse Åsesen Østerby"
                                        },
                                        {
                                            "uuid": "ff6fc101-aeb2-486e-8d39-5d8e718abdec",
                                            "userId": "kbp",
                                            "name": "Kaspar Bach Pedersen"
                                        }
                                    ]
                                }"""));
    }

    @Test
    public void canSearchInitials() throws Exception {
        mockMvc.perform(get("/rest/users/autocomplete")
                        .with(csrf().asHeader())
                        .queryParam("search", "jts")
                        .queryParam("id", "result"))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(content()
                        .json("""
                                {
                                    "totalCount": 1,
                                    "content": [
                                        {
                                            "uuid": "ccd731da-a468-412d-9fdf-820fdc1dac03",
                                            "userId": "jts",
                                            "name": "Jeg Test Svendsen"
                                        }
                                    ]
                                }"""));
    }

    @Test
    public void canSearchName() throws Exception {
        final var response = mockMvc.perform(get("/rest/users/autocomplete")
                        .queryParam("search", "Øst")
                        .queryParam("id", "result"))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(content()
                        .json("""
                                {
                                    "totalCount": 1,
                                    "content": [
                                        {
                                            "uuid": "e20749d3-a292-42e0-92fe-ed58e55a4a1a",
                                            "userId": "ååø",
                                            "name": "Åse Åsesen Østerby"
                                        }
                                    ]
                                }"""));
    }

    private void createUser(final String uuid, final String initials, final String name) {
        userDao.save(User.builder()
                .active(true)
                .uuid(uuid)
                .userId(initials)
                .name(name)
                .build());
    }

}
