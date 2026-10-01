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
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.SupplierService;
import dk.digitalidentity.service.UserService;
import dk.kitos.api.model.IdentityNamePairResponseDTO;
import dk.kitos.api.model.ItContractResponseDTO;
import dk.kitos.api.model.ItSystemResponseDTO;
import dk.kitos.api.model.ItSystemUsageResponseDTO;
import dk.kitos.api.model.ContractValidityResponseDTO;
import dk.kitos.api.model.ItContractValidationErrorChoice;
import dk.kitos.api.model.LifeCycleStatusChoice;
import dk.kitos.api.model.OrganizationUserResponseDTO;
import dk.kitos.api.model.RoleOptionResponseDTO;
import dk.kitos.api.model.TrackingEventResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final KitosClientService kitosClientService;
    private final PlatformTransactionManager transactionManager;

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

    // Not @Transactional: a Kitos lookup per usage would hold a database connection across network
    // round trips, so the lookups run first and only the writes are wrapped.
    public void syncItContracts(final List<ItContractResponseDTO> itContracts) {
        final Map<UUID, List<ItContractResponseDTO>> contractsByUsage = groupByItSystemUsage(itContracts);
        final Set<UUID> runningOutsideBatch = usagesWithRunningContractOutsideBatch(contractsByUsage);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            contractsByUsage.forEach((usageUuid, contracts) ->
                syncContractsForSystemUsage(usageUuid, contracts, runningOutsideBatch.contains(usageUuid))));
    }

    // Only usages where everything in the batch has ended, and only when the asset is known, so an
    // unknown usage costs no network call.
    private Set<UUID> usagesWithRunningContractOutsideBatch(final Map<UUID, List<ItContractResponseDTO>> contractsByUsage) {
        final Set<UUID> runningOutsideBatch = new LinkedHashSet<>();
        contractsByUsage.forEach((usageUuid, contracts) -> {
            if (contracts.stream().allMatch(KitosSyncService::hasEnded)
                && findAssetByUsage(usageUuid).isPresent()
                && hasRunningContractOutsideBatch(contracts, usageUuid)) {
                runningOutsideBatch.add(usageUuid);
            }
        });
        return runningOutsideBatch;
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

    /** A usage's contracts are judged together: an ended contract only counts if no other one is running. */
    private Map<UUID, List<ItContractResponseDTO>> groupByItSystemUsage(final List<ItContractResponseDTO> contracts) {
        final Map<UUID, List<ItContractResponseDTO>> contractsByUsage = new LinkedHashMap<>();
        for (final ItContractResponseDTO contract : contracts) {
            for (final IdentityNamePairResponseDTO usage : nullSafe(contract::getSystemUsages, List.<IdentityNamePairResponseDTO>of())) {
                contractsByUsage.computeIfAbsent(usage.getUuid(), uuid -> new ArrayList<>()).add(contract);
            }
        }
        return contractsByUsage;
    }

    private Optional<Asset> findAssetByUsage(final UUID systemUsageUuid) {
        return assetService.findByProperty(KITOS_USAGE_UUID_PROPERTY_KEY, systemUsageUuid.toString());
    }

    private void syncContractsForSystemUsage(final UUID systemUsageUuid, final List<ItContractResponseDTO> contracts,
                                             final boolean runningContractOutsideBatch) {
        final Optional<Asset> asset = findAssetByUsage(systemUsageUuid);
        if (asset.isEmpty()) {
            return;
        }

        final Optional<ItContractResponseDTO> runningContract = contracts.stream()
            .filter(contract -> !hasEnded(contract))
            .findFirst();
        if (runningContract.isPresent()) {
            setContractEnded(asset.get(), false);
            mapContractData(asset.get(), runningContract.get());
            return;
        }

        if (runningContractOutsideBatch) {
            // No mapping: the running contract's dates must not be overwritten by an ended one.
            setContractEnded(asset.get(), false);
            return;
        }

        final ItContractResponseDTO lastEnded = latestEnded(contracts);
        mapContractData(asset.get(), lastEnded);
        // The Kitos link is deliberately kept: breaking it re-imports the asset as a new one if a contract runs again.
        final boolean alreadyEnded = hasContractEnded(asset.get());
        setContractEnded(asset.get(), true);
        assetService.setActive(asset.get(), false);
        if (!alreadyEnded) {
            log.info("All Kitos contracts for asset id={} have ended (last one {}), marking asset inactive",
                asset.get().getId(), lastEnded.getUuid());
        }
    }

    private static boolean hasContractEnded(final Asset asset) {
        return asset.getProperties().stream().anyMatch(p -> p.getKey().equals(KITOS_CONTRACT_ENDED_PROPERTY_KEY));
    }

    /** Marks that every contract has ended, so the lifecycle sync does not raise the asset back to active. */
    private static void setContractEnded(final Asset asset, final boolean ended) {
        final Optional<Property> existing = asset.getProperties().stream()
            .filter(p -> p.getKey().equals(KITOS_CONTRACT_ENDED_PROPERTY_KEY))
            .findFirst();
        if (ended && existing.isEmpty()) {
            asset.getProperties().add(Property.builder()
                .key(KITOS_CONTRACT_ENDED_PROPERTY_KEY)
                .value("1")
                .entity(asset)
                .build());
        } else if (!ended) {
            existing.ifPresent(asset.getProperties()::remove);
        }
    }

    /** Narrower than Kitos' valid flag, which is also false before a contract starts and when its parent is invalid. */
    private static boolean hasEnded(final ItContractResponseDTO contract) {
        final ContractValidityResponseDTO validity = nullSafe(() -> contract.getGeneral().getValidity());
        if (validity == null || !Boolean.FALSE.equals(validity.getValid())) {
            return false;
        }
        final List<ItContractValidationErrorChoice> validationErrors = validity.getValidationErrors();
        if (validationErrors != null && !validationErrors.isEmpty()) {
            return validationErrors.contains(ItContractValidationErrorChoice.END_DATE_PASSED)
                || validationErrors.contains(ItContractValidationErrorChoice.TERMINATION_PERIOD_EXCEEDED);
        }
        // No reason given: fall back to the end date so a contract that has not started is not read as ended.
        final LocalDate validTo = nullSafe(() -> validity.getValidTo().toLocalDate());
        return validTo != null && validTo.isBefore(LocalDate.now());
    }

    /** A failed or capped lookup assumes a running contract, so a Kitos outage never deactivates a system in use. */
    private boolean hasRunningContractOutsideBatch(final List<ItContractResponseDTO> batch, final UUID systemUsageUuid) {
        final Set<UUID> batchUuids = batch.stream().map(ItContractResponseDTO::getUuid).collect(Collectors.toSet());
        try {
            final List<ItContractResponseDTO> siblings = kitosClientService.fetchContractsForSystemUsage(systemUsageUuid);
            if (siblings.size() >= KitosConstants.MAX_PAGE_SIZE) {
                log.warn("Contract list for it-system usage {} was capped at {}, leaving asset active",
                    systemUsageUuid, KitosConstants.MAX_PAGE_SIZE);
                return true;
            }
            return siblings.stream()
                .filter(contract -> !batchUuids.contains(contract.getUuid()))
                .anyMatch(contract -> !hasEnded(contract));
        } catch (final RuntimeException ex) {
            // RuntimeException, not RestClientException: KitosAuthException is not one.
            log.warn("Could not look up contracts for it-system usage {}, leaving asset active", systemUsageUuid, ex);
            return true;
        }
    }

    /** The contract with the latest end date, so the asset shows the agreement that ran last. */
    private static ItContractResponseDTO latestEnded(final List<ItContractResponseDTO> contracts) {
        return contracts.stream()
            .max(Comparator.comparing(contract -> nullSafe(() -> contract.getGeneral().getValidity().getValidTo().toLocalDate()),
                Comparator.nullsFirst(Comparator.naturalOrder())))
            .orElseThrow(() -> new IllegalStateException("latestEnded called with an empty contract list"));
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

    /** Also for ended contracts: the data stays available in Kitos after "valid to" has passed. */
    private void mapContractData(final Asset asset, final ItContractResponseDTO itContractResponseDTO) {
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

		String settingContractTerminationDate = settingsService.getString(KitosConstants.KITOS_FIELDS_CONTRACT_END, null);
		if (settingContractTerminationDate == null || KitosField.SYSTEMS_CONTRACT_FRONTPAGE_TO.equals(KitosField.valueOf(settingContractTerminationDate))) {
			asset.setContractTermination(
					nullSafe(() -> itContractResponseDTO.getGeneral().getValidity().getValidTo().toLocalDate())
			);
		} else if (KitosField.SYSTEMS_CONTRACT_EXPIRES.equals(KitosField.valueOf(settingContractTerminationDate))) {
			// TODO AMALIE Systemer -> Kontrakt -> Udløber
		}

    }

    private void updateAssetWith(final Asset asset, final ItSystemUsageResponseDTO itSystemUsageResponseDTO) {
        final LifeCycleStatusChoice lifeCycleStatus =
            nullSafe(() -> itSystemUsageResponseDTO.getGeneral().getValidity().getLifeCycleStatus());

        final boolean valid = nullSafe(() -> itSystemUsageResponseDTO.getGeneral().getValidity().getValid(), true);

        // Only "not in use" deactivates; undecided or missing phases are common and must not take systems out
        final boolean activeByLifecycle = valid && lifeCycleStatus != LifeCycleStatusChoice.NOT_IN_USE;
        // Kitos still reports the system as operational after the last contract ran out, so the contract
        // verdict wins here - otherwise the two paths would toggle active and mail on every sync.
        if (!activeByLifecycle || !hasContractEnded(asset)) {
            assetService.setActive(asset, activeByLifecycle);
        }

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

        // Business critical moved from GDPR to General in the Kitos v2 API
        switch (nullSafe(() -> itSystemUsageResponseDTO.getGeneral().getIsBusinessCritical())) {
            case YES -> asset.setCriticality(Criticality.CRITICAL);
            case NO -> asset.setCriticality(Criticality.NON_CRITICAL);
            case null, default -> { }
        }

		String setting = settingsService.getString(KitosConstants.KITOS_FIELDS_ASSET_LINK_SOURCE, null);
		if (setting != null && KitosField.SYSTEMS_REFS_DOCUMENT.equals(KitosField.valueOf(setting))) {
			final List<String> urls = itSystemUsageResponseDTO.getExternalReferences().stream()
					.filter(e -> e.getUrl() != null && !e.getUrl().isBlank())
					.map(e -> e.getUrl())
					.toList();
			setProductLinksIfChanged(asset, urls);
		}
    }

	private void setProductLinksIfChanged(final Asset asset, final List<String> urls) {
		final Set<String> desired = new HashSet<>(urls);
		final Set<String> existing = asset.getProductLinks().stream()
				.map(AssetProductLink::getUrl)
				.collect(Collectors.toSet());
		if (existing.equals(desired)) {
			return;
		}
		asset.getProductLinks().clear();
		asset.getProductLinks().addAll(
				urls.stream()
						.map(url -> new AssetProductLink(null, url, asset))
						.toList()
		);
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
		final List<User> desired;
		if (ownerUuid != null) {
			final List<User> userEntities = userService.findByPropertyKeyValue(KITOS_UUID_PROPERTY_KEY, ownerUuid.toString());
			if (userEntities.size() == 1) {
				desired = List.of(userEntities.getFirst());
			} else if (userEntities.isEmpty()) {
				log.warn("User not found kitos uuid {}", ownerUuid);
				return;
			} else {
				log.warn("Unexpected number of users found for kitos uuid {}, found {}", ownerUuid, userEntities.size());
				return;
			}
		} else {
			desired = Collections.emptyList();
		}
		final Set<String> existingUuids = asset.getResponsibleUsers().stream().map(User::getUuid).collect(Collectors.toSet());
		final Set<String> desiredUuids = desired.stream().map(User::getUuid).collect(Collectors.toSet());
		if (!existingUuids.equals(desiredUuids)) {
			asset.getResponsibleUsers().clear();
			asset.getResponsibleUsers().addAll(desired);
		}
	}

    private Optional<User> findUser(final String uuid) {
        final List<User> userEntities = userService.findByPropertyKeyValue(KITOS_UUID_PROPERTY_KEY, uuid);
        if (userEntities.size() == 1) {
            return Optional.of(userEntities.get(0));
        } else if (userEntities.size() > 1) {
			log.warn("Multiple users found for kitos uuid {}, found {}, selected user with shortest user id", uuid, userEntities.size());
			return userEntities.stream()
					.min(Comparator.comparingInt(u -> u.getUserId().length()));
		}
        log.warn("No users could be found for kitos uuid {}", uuid);
        return Optional.empty();
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
				asset.getProperties().remove(property);
			});
        assetService.setActive(asset, false);
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
			final List<String> urls = responseDTO.getExternalReferences().stream()
					.filter(e -> e.getUrl() != null && !e.getUrl().isBlank())
					.map(e -> e.getUrl())
					.toList();
			setProductLinksIfChanged(asset, urls);
		}
	}

}
