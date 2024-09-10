package dk.digitalidentity.dao.view;

import dk.digitalidentity.model.entity.view.ResponsibleUserView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ResponsibleUserViewDao extends JpaRepository<ResponsibleUserView, String> {

    List<ResponsibleUserView> findByActiveFalse();
    ResponsibleUserView findByUuid(String uuid);
    List<ResponsibleUserView> findAllByUuidIn(final Collection<String> uuids);
}
