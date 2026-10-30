package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.AiAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiAssessmentDao extends JpaRepository<AiAssessment, Long> {

	Optional<AiAssessment> findByAssetId(final Long assetId);
}
