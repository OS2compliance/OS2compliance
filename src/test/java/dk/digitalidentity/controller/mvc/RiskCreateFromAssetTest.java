package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.ContainsAITechnologyEnum;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
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

/**
 * Ansvarlig og leder på et aktiv kan oprette risikovurderinger for aktivet uden superbrugerrollen,
 * men kun for aktiver de selv er tilknyttet.
 */
@Transactional
@AutoConfigureMockMvc
public class RiskCreateFromAssetTest extends BaseIntegrationTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private AssetDao assetDao;
	@Autowired private UserDao userDao;
	@Autowired private ChoiceValueDao choiceValueDao;

	private User responsible;
	private User manager;
	private User outsider;
	private Asset asset;
	private Asset otherAsset;

	@BeforeEach
	void setup() {
		responsible = user("Anna Ansvarlig");
		manager = user("Leo Leder");
		outsider = user("Uden Tilknytning");

		asset = asset("Eget aktiv");
		asset.getResponsibleUsers().add(responsible);
		asset.getManagers().add(manager);
		otherAsset = asset("Andet aktiv");
	}

	@Test
	public void responsibleCanCreateForOwnAsset() throws Exception {
		mockMvc.perform(createFor(asset).with(as(responsible, SecurityUtil.getUserRoles())))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	public void managerCanCreateForOwnAsset() throws Exception {
		mockMvc.perform(createFor(asset).with(as(manager, SecurityUtil.getUserRoles())))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	public void limitedUserCanCreateForOwnAsset() throws Exception {
		mockMvc.perform(createFor(asset).with(as(responsible, SecurityUtil.getLimitedUserRoles())))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	public void userWithoutRelationIsRejected() throws Exception {
		mockMvc.perform(createFor(asset).with(as(outsider, SecurityUtil.getUserRoles())))
				.andExpect(status().isForbidden());
	}

	@Test
	public void addingAnAssetTheUserIsNotRelatedToIsRejected() throws Exception {
		mockMvc.perform(createFor(asset, otherAsset).with(as(responsible, SecurityUtil.getUserRoles())))
				.andExpect(status().isForbidden());
	}

	@Test
	public void userCannotCreateOtherTypes() throws Exception {
		mockMvc.perform(post("/risks/create").with(csrf().asHeader())
						.param("threatAssessmentType", "REGISTER")
						.param("name", "Behandlingsaktivitet")
						.param("registered", "true")
						.param("selectedRegister", "1")
						.with(as(responsible, SecurityUtil.getUserRoles())))
				.andExpect(status().isForbidden());
	}

	@Test
	public void superuserCanCreateForAnyAsset() throws Exception {
		mockMvc.perform(createFor(otherAsset).with(as(outsider, SecurityUtil.getSuperUserRoles())))
				.andExpect(status().is3xxRedirection());
	}

	private MockHttpServletRequestBuilder createFor(final Asset... assets) {
		final MockHttpServletRequestBuilder request = post("/risks/create").with(csrf().asHeader())
				.param("threatAssessmentType", "ASSET")
				.param("name", "Risikovurdering")
				.param("registered", "true");
		for (final Asset selected : assets) {
			request.param("selectedAsset", String.valueOf(selected.getId()));
		}
		return request;
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

	private Asset asset(final String name) {
		final Asset asset = new Asset();
		asset.setName(name);
		asset.setAssetType(choiceValueDao.findByIdentifier(Constants.CHOICE_LIST_ASSET_IT_SYSTEM_TYPE_ID).orElseThrow());
		asset.setAssetStatus(AssetStatus.NOT_STARTED);
		asset.setAiStatus(ContainsAITechnologyEnum.UNDECIDED);
		asset.setCriticality(Criticality.CRITICAL);
		asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NOT_RELEVANT);
		return assetDao.save(asset);
	}
}
