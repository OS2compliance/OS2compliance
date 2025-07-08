package dk.digitalidentity.dao.kle;

import dk.digitalidentity.model.entity.kle.KLEMainGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface KLEMainGroupDao extends JpaRepository<KLEMainGroup, String> {

	@Modifying
	@Query("UPDATE KLEMainGroup k SET k.deleted = true WHERE k.mainGroupNumber NOT IN :mainGroupNumbers AND k.deleted = false")
	@Transactional
	void softDeleteByMainGroupNumbers(Collection<String> mainGroupNumbers);
}
