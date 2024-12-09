package dk.digitalidentity.service;

import dk.digitalidentity.dao.RoleDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleDao roleDao;

    public List<Role> getAssetRoles (Long assetId) {
        return roleDao.findByAsset_Id(assetId);
    }
}
