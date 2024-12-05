package dk.digitalidentity.dao;

import dk.digitalidentity.model.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettingDao extends JpaRepository<Setting, Long> {
	Optional<Setting> findBySettingKey(final String key);
    List<Setting> findByEditableTrue();
    List<Setting> findByAssociationAndEditableTrue(final String association);
	boolean existsBySettingKey(String key);
    List<Setting> findByAssociation(final String association);
}
