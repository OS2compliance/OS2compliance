package dk.digitalidentity.dao.grid;

import dk.digitalidentity.BaseIntegrationTest;
import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.service.ChoiceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static dk.digitalidentity.Constants.CHOICE_LIST_REGISTER_STATUS_NOT_STARTED_ID;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class RegisterGridTagsTest extends BaseIntegrationTest {

	@Autowired private RegisterDao registerDao;
	@Autowired private RegisterGridDao registerGridDao;
	@Autowired private TagDao tagDao;
	@Autowired private OrganisationUnitDao organisationUnitDao;
	@Autowired private ChoiceService choiceService;
	@PersistenceContext private EntityManager em;

	@Test
	public void tagsAreListedOnceWhenRegisterHasSeveralDepartments() {
		em.createNativeQuery("SET SESSION sql_mode = REPLACE(@@sql_mode, 'ONLY_FULL_GROUP_BY', '')").executeUpdate();
		final Tag tag = tagDao.save(Tag.builder().value("Tag A").build());

		final Register register = new Register();
		register.setName("Behandlingsaktivitet med to afdelinger");
		register.setStatus(choiceService.getValue(CHOICE_LIST_REGISTER_STATUS_NOT_STARTED_ID).orElseThrow());
		register.setDepartments(List.of(
				organisationUnitDao.save(OrganisationUnit.builder().uuid("a0000000-0000-0000-0000-000000000001").name("Afdeling 1").active(true).build()),
				organisationUnitDao.save(OrganisationUnit.builder().uuid("a0000000-0000-0000-0000-000000000002").name("Afdeling 2").active(true).build())));
		register.setTags(Set.of(tag));
		final long id = registerDao.save(register).getId();
		em.flush();

		assertThat(registerGridDao.findById(id).orElseThrow().getTagIds()).isEqualTo(String.valueOf(tag.getId()));
	}
}
