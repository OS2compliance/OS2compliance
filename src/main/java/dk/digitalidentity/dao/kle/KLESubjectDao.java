package dk.digitalidentity.dao.kle;

import dk.digitalidentity.model.entity.kle.KLESubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface KLESubjectDao extends JpaRepository<KLESubject, String> {

	@Modifying
	@Query("UPDATE KLESubject k SET k.deleted = true WHERE k.subjectNumber NOT IN :subjectNumbers AND k.deleted = false")
	@Transactional
	void softDeleteBySubjectNumbers(Collection<String> subjectNumbers);
}
