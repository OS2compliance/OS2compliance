package dk.digitalidentity.dao.kle;

import dk.digitalidentity.model.entity.kle.KLEGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

public interface KLEGroupDao extends JpaRepository<KLEGroup, String> {

	Set<KLEGroup> findAllByDeletedFalseAndGroupNumberIn(Collection<String> groupNumbers);

	@Modifying
	@Query("UPDATE KLEGroup k SET k.deleted = true WHERE k.groupNumber NOT IN :groupNumbers AND k.deleted = false")
	@Transactional
	void softDeleteByGroupNumbers(Collection<String> groupNumbers);

	Set<KLEGroup> findByMainGroup_MainGroupNumberIn(Collection<String> mainGroupNumbers);

}
