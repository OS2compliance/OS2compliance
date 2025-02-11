package dk.digitalidentity.controller.api;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.model.entity.ApiClient;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.ApiClientService;
import dk.digitalidentity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link UserApiController}
 */
@Transactional
@AutoConfigureMockMvc
public class UserApiControllerTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ApiClientService apiClientService;
    @MockitoBean
    private UserService userService;

    @BeforeEach
    public void setup() {
        doReturn(Optional.of(new ApiClient())).when(apiClientService).getClientByApiKey(anyString());
    }

    @Test
    public void canListUsers() throws Exception {
        doReturn(createMockUserPage(0, 0, 2, 3)).when(userService).getPaged(2, 0);
        doReturn(createMockUserPage(1, 2, 1, 3)).when(userService).getPaged(2, 1);
        mockMvc.perform(get("/api/v1/users")
                .header("ApiKey", "dummy")
                .param("pageSize", "2")
                .param("page", "0"))
            .andDo(print()).andExpect(status().isOk())
            .andExpect(content().json("""
                {
                    "totalPages": 2,
                    "page": 0,
                    "totalCount": 3,
                    "count": 2,
                    "content": [
                        {
                            "userId": "userid_0",
                            "name": "Name 0",
                            "email": "user0@mail.somewhere"
                        },
                        {
                            "userId": "userid_1",
                            "name": "Name 1",
                            "email": "user1@mail.somewhere"
                        }
                    ]
                }
"""));
        mockMvc.perform(get("/api/v1/users")
                .header("ApiKey", "dummy")
                .param("pageSize", "2")
                .param("page", "1"))
            .andDo(print()).andExpect(status().isOk())
            .andExpect(content().json("""
                {
                    "totalPages": 3,
                    "page": 1,
                    "totalCount": 3,
                    "count": 1,
                    "content": [
                        {
                            "userId": "userid_2",
                            "name": "Name 2",
                            "email": "user2@mail.somewhere"
                        }
                    ]
                }
"""));

    }

    private static Page<User> createMockUserPage(final int page, final int idx, final int count, final int totalCount) {
        final List<User> users = IntStream.range(idx, idx+count).mapToObj(UserApiControllerTest::createMockUser).toList();
        return new PageImpl<>(users, PageRequest.of(page, count), totalCount);
    }

    private static User createMockUser(final int idx) {
        return User.builder()
            .name("Name " + idx)
            .userId("userid_" + idx)
            .active(true)
            .email("user"+idx+"@mail.somewhere")
            .build();
    }

}
