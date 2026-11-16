package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DocumentRevisionInterval;
import dk.digitalidentity.model.entity.enums.DocumentStatus;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
public class DocumentsControllerEditTest extends BaseIntegrationTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private DocumentDao documentDao;
	@Autowired private UserDao userDao;
	@Autowired private ChoiceValueDao choiceValueDao;

	private User outsider;
	private Document document;

	@BeforeEach
	void setup() {
		final User responsible = user("Anna Ansvarlig");
		outsider = user("Uden Tilknytning");

		document = new Document();
		document.setName("Dokument");
		document.setDocumentType(choiceValueDao.findByIdentifier("document-type-other-123456").orElseThrow());
		document.setStatus(DocumentStatus.NOT_STARTED);
		document.setRevisionInterval(DocumentRevisionInterval.NONE);
		document.setResponsibleUser(responsible);
		document = documentDao.save(document);
	}

	@Test
	public void superuserCanEditDocumentOfOthers() throws Exception {
		mockMvc.perform(edit().with(as(outsider, SecurityUtil.getSuperUserRoles())))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	public void userCannotEditDocumentOfOthers() throws Exception {
		mockMvc.perform(edit().with(as(outsider, SecurityUtil.getUserRoles())))
				.andExpect(status().isForbidden());
	}

	private MockHttpServletRequestBuilder edit() {
		return post("/documents/edit").with(csrf().asHeader())
				.param("id", String.valueOf(document.getId()))
				.param("name", document.getName())
				.param("description", "Ny beskrivelse")
				.param("documentTypeId", String.valueOf(document.getDocumentType().getId()))
				.param("status", document.getStatus().name())
				.param("revisionInterval", document.getRevisionInterval().name())
				.param("responsibleUser", document.getResponsibleUser().getUuid())
				.param("includeInYearWheel", "false");
	}

	private static RequestPostProcessor as(final User user, final Set<String> roles) {
		final Set<SamlGrantedAuthority> authorities = roles.stream().map(SamlGrantedAuthority::new).collect(Collectors.toSet());
		final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user.getUuid(), null, authorities);
		auth.setDetails(TokenUser.builder()
				.cvr("1234556")
				.username(user.getUuid())
				.build());
		return authentication(auth);
	}

	private User user(final String name) {
		final User user = new User();
		user.setName(name);
		user.setActive(true);
		return userDao.save(user);
	}
}
