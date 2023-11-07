package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.model.entity.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link TagController}
 */
@Transactional
@AutoConfigureMockMvc
public class TagControllerTest extends BaseIntegrationTest {
    @Autowired
    private TagDao tagDao;
    @Autowired
    private MockMvc mockMvc;
    @BeforeEach
    public void setup() {
        tagDao.save(Tag.builder().value("tag1").build());
        tagDao.save(Tag.builder().value("æøå1234").build());
        tagDao.save(Tag.builder().value("ost").build());
        tagDao.save(Tag.builder().value("plappe").build());
    }

    @Test
    public void canSearch() throws Exception {
        final var response = mockMvc.perform(get("/tags/autocomplete").with(csrf())
                        .queryParam("search", "1")
                        .queryParam("id", "result"))
                .andDo(print()).andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getContentAsString())
                .isEqualToIgnoringWhitespace("<datalist id=\"result\" >\n" +
                        "    <option>tag1</option>\n" +
                        "    <option>æøå1234</option>\n" +
                        "</datalist>");
    }

    @Test
    public void canCreateTags() throws Exception {
        mockMvc.perform(post("/tags/newTag").with(csrf()))
                .andDo(print()).andExpect(status().isCreated());
        assertThat(tagDao.findByValue("newTag")).isPresent();
    }


}
