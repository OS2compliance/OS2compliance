package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.dao.KitosRolesDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.KitosRole;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.UserProperty;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.AssetType;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.SupplierService;
import dk.kitos.api.model.IdentityNamePairResponseDTO;
import dk.kitos.api.model.ItContractResponseDTO;
import dk.kitos.api.model.ItSystemResponseDTO;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.OrganizationUserResponseDTO;
import dk.kitos.api.model.RoleOptionResponseDTO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dk.digitalidentity.Constants.NEEDS_CVR_UPDATE_PROPERTY;
import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_OWNER_ROLE_SETTING_KEY;
import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_RESPONSIBLE_ROLE_SETTING_KEY;
import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_UUID_PROPERTY_KEY;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Slf4j
@Service
public class KitosSyncService {
    private final AssetService assetService;
    private final SupplierService supplierService;
    private final UserDao userDao;
    private final SettingsService settingsService;
    private final KitosRolesDao rolesDao;

    public KitosSyncService(final AssetService assetService, final SupplierService supplierService, final UserDao userDao, final SettingsService settingsService, final KitosRolesDao rolesDao) {
        this.assetService = assetService;
        this.supplierService = supplierService;
        this.userDao = userDao;
        this.settingsService = settingsService;
        this.rolesDao = rolesDao;
    }

    @Transactional
    public void syncUsers(final List<OrganizationUserResponseDTO> users) {
        users.forEach(this::syncSingleUser);
    }

    @Transactional
    public void syncRoles(final List<RoleOptionResponseDTO> roles) {
        rolesDao.deleteAll();
        roles.forEach(this::syncSingleRole);
    }

    @Transactional
    public void syncItContracts(final List<ItContractResponseDTO> itContracts) {
        itContracts.forEach(this::syncSingleItContract);
    }

    @Transactional
    public void syncItSystemUsages(final List<ItSystemUsageResponseDTO> changedItSystemUsages, final List<OrganizationUserResponseDTO> users) {
        changedItSystemUsages.forEach(itSystem -> syncSingleItSystemUsage(itSystem, users));
    }

    @Transactional
    public void syncItSystems(final List<ItSystemResponseDTO> changedItSystems) {
        changedItSystems.forEach(this::syncSingleItSystem);
    }

    private void syncSingleRole(final RoleOptionResponseDTO roleOptionResponseDTO) {
        rolesDao.save(KitosRole.builder()
            .uuid(roleOptionResponseDTO.getUuid().toString())
            .description(roleOptionResponseDTO.getDescription())
            .name(roleOptionResponseDTO.getName())
            .build());
    }

    private void syncSingleUser(final OrganizationUserResponseDTO userResponseDTO) {
        final String kitosUuid = userResponseDTO.getUuid().toString();
        userDao.findFirstByEmailEqualsIgnoreCaseAndActiveIsTrue(userResponseDTO.getEmail())
            .ifPresentOrElse(
                user -> {
                    final UserProperty userProperty = user.getProperties().stream()
                        .filter(p -> p.getKey().equals(KITOS_UUID_PROPERTY_KEY))
                        .findFirst().orElseGet(() -> {
                            final UserProperty newProperty = UserProperty.builder()
                                .key(KITOS_UUID_PROPERTY_KEY)
                                .build();
                            newProperty.setUser(user);
                            user.getProperties().add(newProperty);
                            return newProperty;
                        });
                    userProperty.setValue(kitosUuid);
                },
                () -> log.warn("User with email: {} not found", userResponseDTO.getEmail()));
    }


    private void syncSingleItContract(final ItContractResponseDTO contractResponseDTO) {
        contractResponseDTO.getSystemUsages().stream()
            .map(IdentityNamePairResponseDTO::getUuid)
            .forEach(itSystemUuid -> {
                final Optional<Asset> itSystem = findItSystem(itSystemUuid.toString());
                itSystem.ifPresent(asset -> updateAssetWith(asset, contractResponseDTO));
            });
    }


    private void syncSingleItSystemUsage(final ItSystemUsageResponseDTO itSystemUsageResponseDTO,
                                         final List<OrganizationUserResponseDTO> users) {
        final String itSystemUuid = nullSafe(() -> itSystemUsageResponseDTO.getSystemContext().getUuid().toString());
        if (itSystemUuid == null) {
            return;
        }
        final Optional<Asset> itSystem = findItSystem(itSystemUuid);
        itSystem.ifPresent(asset -> updateAssetWith(asset, itSystemUsageResponseDTO, users));
    }

    private void syncSingleItSystem(final ItSystemResponseDTO responseDTO) {
        final Optional<Asset> itSystem = findItSystem(responseDTO.getUuid().toString());
        if (itSystem.isPresent()) {
            updateAsset(itSystem.get(), responseDTO);
        } else {
            createAsset(responseDTO);
        }
    }

    private void updateAssetWith(final Asset asset, final ItContractResponseDTO itContractResponseDTO) {
        if (!itContractResponseDTO.getGeneral().getValidity().getValid()) {
            return;
        }
        asset.setContractDate(
            nullSafe(() -> itContractResponseDTO.getGeneral().getValidity().getValidFrom().toLocalDate())
        );
        asset.setContractTermination(
            nullSafe(() -> itContractResponseDTO.getGeneral().getValidity().getValidTo().toLocalDate())
        );
    }

    private void updateAssetWith(final Asset asset, final ItSystemUsageResponseDTO itSystemUsageResponseDTO,
                                 final List<OrganizationUserResponseDTO> users) {
        setAssetOwner(asset, itSystemUsageResponseDTO, users);
        setAssetManagers(asset, itSystemUsageResponseDTO, users);
    }

    private void setAssetManagers(final Asset asset, final ItSystemUsageResponseDTO itSystemUsageResponseDTO, final List<OrganizationUserResponseDTO> users) {
        final String responsibleRoleUuid = settingsService.getString(KITOS_RESPONSIBLE_ROLE_SETTING_KEY, "");
        itSystemUsageResponseDTO.getRoles().stream()
            .filter(r -> responsibleRoleUuid.equalsIgnoreCase(r.getRole().getUuid().toString()))
            .map(r -> r.getUser().getUuid())
            .forEach(r -> {
                final Optional<User> user = findUser(r.toString(), users);
                user.ifPresent(value -> {
                    // Make sure to only add managers once
                    if (asset.getManagers().stream().noneMatch(u -> value.getUuid().equals(u.getUuid()))) {
                        asset.getManagers().add(value);
                    }
                });
            });
    }

    private Optional<User> findUser(final String uuid, final List<OrganizationUserResponseDTO> users) {
        final List<User> userEntities = userDao.findByPropertyKeyValue(KITOS_UUID_PROPERTY_KEY, uuid);
        if (userEntities.size() == 1) {
            return Optional.of(userEntities.get(0));
        }
        log.warn("Unexpected number of users found for kitos uuid {}, found {}", uuid, userEntities.size());
        return Optional.empty();
    }

    private void setAssetOwner(final Asset asset, final ItSystemUsageResponseDTO itSystemUsageResponseDTO, final List<OrganizationUserResponseDTO> users) {
        final String ownerRoleUuid = settingsService.getString(KITOS_OWNER_ROLE_SETTING_KEY, "");
        final UUID ownerUuid = itSystemUsageResponseDTO.getRoles().stream()
            .filter(r -> ownerRoleUuid.equalsIgnoreCase(r.getRole().getUuid().toString()))
            .map(r -> r.getUser().getUuid())
            .findFirst().orElse(null);
        if (ownerUuid != null) {
            final List<User> userEntities = userDao.findByPropertyKeyValue(KITOS_UUID_PROPERTY_KEY, ownerUuid.toString());
            if (userEntities.size() == 1) {
                asset.setResponsibleUser(userEntities.get(0));
            } else if (userEntities.isEmpty()) {
                log.warn("User not found kitos uuid {}", ownerUuid);
            } else {
                log.warn("Unexpected number of users found for kitos uuid {}, found {}", ownerUuid, userEntities.size());
            }
        }
    }

    private void updateAsset(final Asset asset, final ItSystemResponseDTO responseDTO) {
        asset.setName(responseDTO.getName());
        asset.setDescription(responseDTO.getDescription());
        asset.setSupplier(findOrCreateSupplier(responseDTO));
    }

    private void createAsset(final ItSystemResponseDTO responseDTO) {
        final Supplier supplier = findOrCreateSupplier(responseDTO);
        if (supplier == null) {
            log.warn("Supplier for " + responseDTO.getName() + " not found skipping record");
            return;
        }
        final Asset asset = new Asset();
        asset.getProperties().add(
            Property.builder()
                .key(KITOS_UUID_PROPERTY_KEY)
                .value(responseDTO.getUuid().toString())
                .entity(asset)
                .build());
        asset.setCreatedBy(responseDTO.getCreatedBy().getName());
        asset.setUpdatedAt(responseDTO.getLastModified().toLocalDateTime());
        asset.setName(responseDTO.getName());
        asset.setDescription(responseDTO.getDescription());
        asset.setAssetType(AssetType.IT_SYSTEM);
        asset.setAssetStatus(AssetStatus.NOT_STARTED);
        asset.setSupplier(supplier);
        asset.setCriticality(Criticality.NON_CRITICAL);
        asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NO);
        assetService.create(asset);
    }

    private Supplier findOrCreateSupplier(final ItSystemResponseDTO responseDTO) {
        if (responseDTO.getRightsHolder() == null || responseDTO.getRightsHolder().getCvr() == null) {
            return null;
        }
        return supplierService.findByCvr(responseDTO.getRightsHolder().getCvr())
            .orElseGet(() -> createSupplier(responseDTO));
    }

    private Supplier createSupplier(final ItSystemResponseDTO responseDTO) {
        assert responseDTO.getRightsHolder() != null;
        final Supplier supplier = new Supplier();
        supplier.setCreatedBy(responseDTO.getCreatedBy().getName());
        supplier.setName(responseDTO.getRightsHolder().getName());
        supplier.setCvr(responseDTO.getRightsHolder().getCvr());
        supplier.getProperties().add(Property.builder()
            .key(KITOS_UUID_PROPERTY_KEY)
            .value(responseDTO.getRightsHolder().getUuid().toString())
            .entity(supplier)
            .build());
        supplier.getProperties().add(Property.builder()
            .key(NEEDS_CVR_UPDATE_PROPERTY)
            .value("1")
            .entity(supplier)
            .build());
        supplierService.create(supplier);
        return supplier;
    }

    private Optional<Asset> findItSystem(final String kitosUuid) {
        return assetService.findByKitosUuid(kitosUuid);
    }

}
