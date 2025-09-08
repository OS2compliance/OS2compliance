package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.KitosRolesDao;
import dk.digitalidentity.model.dto.enums.KitosField;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetProductLink;
import dk.digitalidentity.model.entity.KitosRole;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.UserProperty;
import dk.digitalidentity.model.entity.enums.ArchiveDuty;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.ContainsAITechnologyEnum;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.SupplierService;
import dk.digitalidentity.service.UserService;
import dk.kitos.api.model.GDPRRegistrationsResponseDTO;
import dk.kitos.api.model.IdentityNamePairResponseDTO;
import dk.kitos.api.model.ItContractResponseDTO;
import dk.kitos.api.model.ItSystemResponseDTO;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.OrganizationUserResponseDTO;
import dk.kitos.api.model.RoleOptionResponseDTO;
import dk.kitos.api.model.TrackingEventResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.NEEDS_CVR_UPDATE_PROPERTY;
import static dk.digitalidentity.integration.kitos.KitosConstants.*;
import static dk.digitalidentity.util.NullSafe.nullSafe;

@Slf4j
@Service
@RequiredArgsConstructor
public class KitosSyncService {
    private final AssetService assetService;
    private final SupplierService supplierService;
    private final UserService userService;
    private final SettingsService settingsService;
    private final KitosRolesDao rolesDao;
    private final ChoiceService choiceService;

    @Transactional
    public void syncDeletedItSystems(final List<TrackingEventResponseDTO> deletionEvents) {
        deletionEvents.stream().map(TrackingEventResponseDTO::getEntityUuid).forEach(this::syncItSystemDeletion);
    }

    @Transactional
    public void syncDeletedItSystemUsages(final List<TrackingEventResponseDTO> deletedUsages) {
        deletedUsages.stream().map(TrackingEventResponseDTO::getEntityUuid).forEach(this::syncItSystemUsageDeletion);
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
    public void syncItSystemUsages(final List<ItSystemUsageResponseDTO> changedItSystemUsages) {
        changedItSystemUsages.forEach(this::syncSingleItSystemUsage);
    }

    @Transactional
    public void syncItSystems(final List<ItSystemResponseDTO> changedItSystems) {
        changedItSystems.forEach(this::syncSingleItSystem);
    }

    private void syncItSystemDeletion(final UUID itSystemUuid) {
        assetService.findByProperty(KITOS_UUID_PROPERTY_KEY, itSystemUuid.toString())
            .ifPresent(this::removeKitosUuid);
    }

    private void syncItSystemUsageDeletion(final UUID usageUuid) {
        assetService.findByProperty(KITOS_USAGE_UUID_PROPERTY_KEY, usageUuid.toString())
            .ifPresent(this::removeKitosUuid);
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
        // Try to look up the user by e-mail if that fails try by name.
        userService.findByEmail(userResponseDTO.getEmail())
            .ifPresentOrElse(
                user -> setKitosUuid(user, kitosUuid),
                () -> syncSingleUserByName(userResponseDTO));
    }

    private void syncSingleUserByName(final OrganizationUserResponseDTO userResponseDTO) {
        final String kitosUuid = userResponseDTO.getUuid().toString();
        final List<User> users = userService.findByName(userResponseDTO.getName());
        if (users.size() == 1) {
            setKitosUuid(users.get(0), kitosUuid);
        } else if (users.isEmpty()) {
            log.info("User with email: {} and name {} not found", userResponseDTO.getEmail(), userResponseDTO.getName());
        } else {
            log.info("Multiple users with name {} found, set correct email in Kitos to look up the user", userResponseDTO.getName());
        }
    }

    private void syncSingleItContract(final ItContractResponseDTO contractResponseDTO) {
        contractResponseDTO.getSystemUsages().stream()
            .map(IdentityNamePairResponseDTO::getUuid)
            .forEach(itSystemUuid -> {
                final Optional<Asset> itSystem = findItSystem(itSystemUuid.toString());
                itSystem.ifPresent(asset -> updateAssetWith(asset, contractResponseDTO));
            });
    }

    private void syncSingleItSystemUsage(final ItSystemUsageResponseDTO itSystemUsageResponseDTO) {
        final String itSystemUuid = nullSafe(() -> itSystemUsageResponseDTO.getSystemContext().getUuid().toString());
        if (itSystemUuid == null) {
            return;
        }
        final Optional<Asset> itSystem = findItSystem(itSystemUuid);
        itSystem.ifPresent(asset -> updateAssetWith(asset, itSystemUsageResponseDTO));
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

		asset.setTerminationNotice(
				nullSafe(() -> itContractResponseDTO.getTermination().getTerms().getNoticePeriodMonths().getName())
		);

		String settingContractDate = settingsService.getString(KitosConstants.KITOS_FIELDS_CONTRACT_DATE, null);
		if (settingContractDate == null || KitosField.SYSTEMS_CONTRACT_FRONTPAGE_FROM.equals(KitosField.valueOf(settingContractDate))) {
			asset.setContractDate(
					nullSafe(() -> itContractResponseDTO.getGeneral().getValidity().getValidFrom().toLocalDate())
			);
		} else if (KitosField.SYSTEMS_CONTRACT_CONCLUDED.equals(KitosField.valueOf(settingContractDate))) {
			// TODO AMALIE Systemer -> Kontrakt -> Indgået
		}

		String settingContractTerminationDate = settingsService.getString(KitosConstants.KITOS_FIELDS_CONTRACT_DATE, null);
		if (settingContractTerminationDate == null || KitosField.SYSTEMS_CONTRACT_FRONTPAGE_TO.equals(KitosField.valueOf(settingContractTerminationDate))) {
			asset.setContractTermination(
					nullSafe(() -> itContractResponseDTO.getGeneral().getValidity().getValidTo().toLocalDate())
			);
		} else if (KitosField.SYSTEMS_CONTRACT_EXPIRES.equals(KitosField.valueOf(settingContractTerminationDate))) {
			// TODO AMALIE Systemer -> Kontrakt -> Udløber
		}

    }

    private void updateAssetWith(final Asset asset, final ItSystemUsageResponseDTO itSystemUsageResponseDTO) {
        final boolean valid = nullSafe(() -> itSystemUsageResponseDTO.getGeneral().getValidity().getValid(), true);
        if (!valid) {
            return;
        }

		// Map enum from integration to internal
		ContainsAITechnologyEnum internalStatus;
		try {
			var dtoEnum = itSystemUsageResponseDTO.getGeneral().getContainsAITechnology();
			if (dtoEnum == null) {
				internalStatus = ContainsAITechnologyEnum.UNDECIDED;
			} else {
				internalStatus = ContainsAITechnologyEnum.valueOf(dtoEnum.name());
			}
		} catch (IllegalArgumentException e) {
			internalStatus = ContainsAITechnologyEnum.UNDECIDED;
		}
		asset.setAiStatus(internalStatus);

		addKitosUsageUuid(asset, itSystemUsageResponseDTO.getUuid().toString());
		setAssetOwner(asset, itSystemUsageResponseDTO);
		setUsersWithRole(asset.getManagers(), itSystemUsageResponseDTO, KITOS_RESPONSIBLE_ROLE_SETTING_KEY);
		setUsersWithRole(asset.getOperationResponsibleUsers(), itSystemUsageResponseDTO, KITOS_OPERATION_RESPONSIBLE_ROLE_SETTING_KEY);
		asset.setArchive(ArchiveDuty.fromApiEnum(itSystemUsageResponseDTO.getArchiving().getArchiveDuty()));

        final GDPRRegistrationsResponseDTO.BusinessCriticalEnum businessCritical = nullSafe(() -> itSystemUsageResponseDTO.getGdpr().getBusinessCritical());
        if (businessCritical != null) {
            if (businessCritical == GDPRRegistrationsResponseDTO.BusinessCriticalEnum.YES && asset.getCriticality() != Criticality.CRITICAL) {
                asset.setCriticality(Criticality.CRITICAL);
            } else if (businessCritical == GDPRRegistrationsResponseDTO.BusinessCriticalEnum.NO && asset.getCriticality() != Criticality.NON_CRITICAL) {
                asset.setCriticality(Criticality.NON_CRITICAL);
            }
        }

		String setting = settingsService.getString(KitosConstants.KITOS_FIELDS_ASSET_LINK_SOURCE, null);
		if (setting != null && KitosField.SYSTEMS_REFS_DOCUMENT.equals(KitosField.valueOf(setting))) {
			asset.getProductLinks().clear();
			asset.getProductLinks().addAll(
				itSystemUsageResponseDTO.getExternalReferences().stream()
						.filter(e -> e.getUrl() != null && !e.getUrl().isBlank())
						.map(e -> new AssetProductLink(null, e.getUrl(), asset))
					.toList()
			);
		}
    }

    private void setUsersWithRole(final List<User> target, final ItSystemUsageResponseDTO itSystemUsageResponseDTO, final String roleSettingKey) {
		final String roleUuid = settingsService.getString(roleSettingKey, "");
		final List<User> usersWithRole = itSystemUsageResponseDTO.getRoles().stream()
				.filter(r -> roleUuid.equalsIgnoreCase(r.getRole().getUuid().toString()))
				.map(r -> r.getUser().getUuid())
				.distinct()
				.map(r -> findUser(r.toString()))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		target.removeIf(u -> !usersWithRole.contains(u));
		for (User u : usersWithRole) {
			if (!target.contains(u)) {
				target.add(u);
			}
		}
    }

	private void setAssetOwner(final Asset asset, final ItSystemUsageResponseDTO itSystemUsageResponseDTO) {
		final String ownerRoleUuid = settingsService.getString(KITOS_OWNER_ROLE_SETTING_KEY, "");
		final UUID ownerUuid = itSystemUsageResponseDTO.getRoles().stream()
				.filter(r -> ownerRoleUuid.equalsIgnoreCase(r.getRole().getUuid().toString()))
				.map(r -> r.getUser().getUuid())
				.findFirst().orElse(null);
		if (ownerUuid != null) {
			final List<User> userEntities = userService.findByPropertyKeyValue(KITOS_UUID_PROPERTY_KEY, ownerUuid.toString());
			if (userEntities.size() == 1) {
				asset.setResponsibleUsers(List.of(userEntities.getFirst()));
			} else if (userEntities.isEmpty()) {
				log.warn("User not found kitos uuid {}", ownerUuid);
			} else {
				log.warn("Unexpected number of users found for kitos uuid {}, found {}", ownerUuid, userEntities.size());
			}
		}
	}

    private Optional<User> findUser(final String uuid) {
        final List<User> userEntities = userService.findByPropertyKeyValue(KITOS_UUID_PROPERTY_KEY, uuid);
        if (userEntities.size() == 1) {
            return Optional.of(userEntities.get(0));
        }
        log.warn("Unexpected number of users found for kitos uuid {}, found {}", uuid, userEntities.size());
        return Optional.empty();
    }

	// TODO: Refactor when eliminating mapping tables in the future
	private void setAssetOperationResponsible(final Asset asset, final ItSystemUsageResponseDTO itSystemUsageResponseDTO) {
		final String operationResponsibleRoleUuid = settingsService.getString(KITOS_OPERATION_RESPONSIBLE_ROLE_SETTING_KEY, "");
		asset.getOperationResponsibleUsers().clear();
		itSystemUsageResponseDTO.getRoles().stream()
				.filter(r -> operationResponsibleRoleUuid.equalsIgnoreCase(r.getRole().getUuid().toString()))
				.map(r -> r.getUser().getUuid())
				.forEach(r -> {
					final Optional<User> user = findUser(r.toString());
					user.ifPresent(value -> {
						// Make sure to only add managers once
						if (asset.getOperationResponsibleUsers().stream().noneMatch(u -> value.getUuid().equals(u.getUuid()))) {
							asset.getOperationResponsibleUsers().add(value);
						}
					});
				});
	}

    private void updateAsset(final Asset asset, final ItSystemResponseDTO responseDTO) {
        asset.setName(responseDTO.getName());
        asset.setDescription(responseDTO.getDescription());
        if (!supplierOverriden(asset)) {
            asset.setSupplier(findOrCreateSupplier(responseDTO));
        }

		setLinksFromKitosSystem(responseDTO, asset);
	}

    private boolean supplierOverriden(final Asset asset) {
        //noinspection DataFlowIssue
        return nullSafe(() -> asset.getSupplier().getProperties().stream().anyMatch(p -> KITOS_UUID_PROPERTY_KEY.equals(p.getKey())), true);
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
		asset.setAiStatus(ContainsAITechnologyEnum.UNDECIDED);
        asset.setAssetType(choiceService.getValue(Constants.CHOICE_LIST_ASSET_IT_SYSTEM_TYPE_ID).orElseThrow());
        asset.setAssetStatus(AssetStatus.NOT_STARTED);
        asset.setSupplier(supplier);
        asset.setCriticality(Criticality.NON_CRITICAL);
        asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NO);
		setLinksFromKitosSystem(responseDTO, asset);

		assetService.create(asset);
    }

	private Supplier findOrCreateSupplier(final ItSystemResponseDTO responseDTO) {
        if (responseDTO.getRightsHolder() == null) {
            return null;
        }
        if (validCvr(responseDTO.getRightsHolder().getCvr())) {
            return supplierService.findByCvr(responseDTO.getRightsHolder().getCvr())
                .orElseGet(() -> createSupplier(responseDTO));
        } else {
            return supplierService.findByName(responseDTO.getRightsHolder().getName())
                .orElseGet(() -> createSupplier(responseDTO));
        }
    }

    private Supplier createSupplier(final ItSystemResponseDTO responseDTO) {
        assert responseDTO.getRightsHolder() != null;
        final Supplier supplier = new Supplier();
        supplier.setCreatedBy(responseDTO.getCreatedBy().getName());
        supplier.setName(responseDTO.getRightsHolder().getName());
        final String cvr = responseDTO.getRightsHolder().getCvr();
        supplier.setCvr(cvr);
        setKitosUuid(supplier, responseDTO.getRightsHolder().getUuid().toString());
        if (validCvr(cvr)) {
            setNeedsCvrUpdate(supplier);
        }
        supplierService.create(supplier);
        return supplier;
    }

    private void setKitosUuid(final User user, final String kitosUuid) {
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
    }

    private void removeKitosUuid(final Asset asset) {
        final List<Property> listCopy = new ArrayList<>(asset.getProperties());
        listCopy.stream()
            .filter(p -> p.getKey().equals(KITOS_UUID_PROPERTY_KEY) || p.getKey().equals(KITOS_USAGE_UUID_PROPERTY_KEY))
            .forEach(property -> {
				Property copyOfProperty = new Property();
				copyOfProperty.setKey(X_KITOS_USAGE_UUID_PROPERTY_KEY);
				copyOfProperty.setValue(property.getValue());
				copyOfProperty.setEntity(asset);
				asset.getProperties().add(copyOfProperty);
				asset.setActive(false);
				asset.getProperties().remove(property);
			});
    }

    private Optional<Asset> findItSystem(final String kitosUuid) {
        return assetService.findByProperty(KITOS_UUID_PROPERTY_KEY, kitosUuid);
    }

    private boolean validCvr(final String cvr) {
        if (StringUtils.isEmpty(cvr)) {
            return false;
        }
        final String digits = StringUtils.getDigits(cvr);
        return digits.length() == 8;
    }

    private static void setNeedsCvrUpdate(final Supplier supplier) {
        supplier.getProperties().add(Property.builder()
            .key(NEEDS_CVR_UPDATE_PROPERTY)
            .value("1")
            .entity(supplier)
            .build());
    }

    private static void addKitosUsageUuid(final Asset asset, final String usageUuid) {
        asset.getProperties().stream()
            .filter(p -> p.getKey().equals(KITOS_USAGE_UUID_PROPERTY_KEY))
            .findFirst()
            .ifPresentOrElse(
                p -> p.setValue(usageUuid),
                () -> asset.getProperties().add(Property.builder()
                    .key(KITOS_USAGE_UUID_PROPERTY_KEY)
                    .value(usageUuid)
                    .entity(asset)
                    .build())
            );
    }

    private static void setKitosUuid(final Supplier supplier, final String kitosUuid) {
        supplier.getProperties().stream()
            .filter(p -> p.getKey().equals(KITOS_UUID_PROPERTY_KEY))
            .findFirst()
            .ifPresentOrElse(
                p -> p.setValue(kitosUuid),
                () -> supplier.getProperties().add(Property.builder()
                    .key(KITOS_UUID_PROPERTY_KEY)
                    .value(kitosUuid)
                    .entity(supplier)
                    .build())
            );
    }

	private void setLinksFromKitosSystem(ItSystemResponseDTO responseDTO, Asset asset) {
		String setting = settingsService.getString(KitosConstants.KITOS_FIELDS_ASSET_LINK_SOURCE, null);
		if (setting != null && KitosField.SYSTEMS_FRONTPAGE_REFS.equals(KitosField.valueOf(setting))) {
			asset.getProductLinks().clear();
			asset.getProductLinks().addAll(
				responseDTO.getExternalReferences().stream()
						.filter(e -> e.getUrl() != null && !e.getUrl().isBlank())
						.map(e -> new AssetProductLink(null, e.getUrl(), asset))
					.collect(Collectors.toList())
			);
		}
	}

}
