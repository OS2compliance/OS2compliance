package dk.digitalidentity.service;

import dk.digitalidentity.dao.KitosRolesDao;
import dk.digitalidentity.model.entity.KitosRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KitosService {
    private final KitosRolesDao kitosRolesDao;

    public List<KitosRole> kitosRoles() {
        return kitosRolesDao.findAll();
    }

    public Page<KitosRole> findAll(final Pageable pageable) {
        return kitosRolesDao.findAll(pageable);
    }

    public Page<KitosRole> searchForRole(final String search, final Pageable pageable) {
        return kitosRolesDao.searchForRole(search, pageable);
    }

}
