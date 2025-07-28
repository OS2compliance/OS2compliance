package dk.digitalidentity.dao.kle;

import dk.digitalidentity.model.entity.kle.KLELegalReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

public interface KLELegalReferenceDao extends JpaRepository<KLELegalReference, String> {

	@Modifying
	@Query("UPDATE KLELegalReference k SET k.deleted = true WHERE k.accessionNumber NOT IN :accessionNumbers AND k.deleted = false")
	@Transactional
	void softDeleteByAccessionNumbers(Collection<String> accessionNumbers);

	Set<KLELegalReference> findByDeletedFalseAndAccessionNumberIn(Collection<String> accessionNumbers);
}
