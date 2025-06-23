package dk.digitalidentity.controller.api;

import dk.digitalidentity.mapping.AssetMapper;
import dk.digitalidentity.model.api.AssetCreateEO;
import dk.digitalidentity.model.api.AssetEO;
import dk.digitalidentity.model.api.AssetUpdateEO;
import dk.digitalidentity.model.api.ErrorEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.SupplierWriteEO;
import dk.digitalidentity.model.api.UserWriteEO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetProductLink;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.AssetStatus;

import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.NextInspection;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.SupplierService;
import dk.digitalidentity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static dk.digitalidentity.util.NullSafe.nullSafe;

@RestController
@RequestMapping(value = "/api/v1/assets")
@Tag(name = "Assets resource")
@RequiredArgsConstructor
public class AssetApiController {
    private final AssetService assetService;
    private final AssetMapper assetMapper;
    private final UserService userService;
    private final SupplierService supplierService;
    private final ChoiceService choiceService;

    @Operation(summary = "Fetch an asset")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Get an asset"),
        @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @GetMapping(value = "{id}", produces = "application/json")
    public AssetEO read(@PathVariable final Long id) {
        final Asset asset = assetService.get(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
        return assetMapper.toEO(asset);
    }

    @Operation(summary = "List all assets")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All assets"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @GetMapping(produces = "application/json")
    public PageEO<AssetEO> list(@Parameter(description = "Page size to fetch, max is 500")  @RequestParam(value = "pageSize", defaultValue = "100") @Max(500) final int pageSize,
                                @Parameter(description = "The page to fetch, first page is 0") @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero final int page) {
        return assetMapper.toEO(assetService.getPaged(pageSize, page));
    }

    @Operation(summary = "Create a new asset")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "The created asset"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PostMapping(produces = "application/json", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public AssetEO create(@Valid @RequestBody final AssetCreateEO assetCreateEO) {
        final List<User> responsibleUsers = userService.findAllByUuids(nullSafe(() -> assetCreateEO.getSystemOwners().stream().map(s -> s.getUuid()).collect(Collectors.toSet())));
        final Asset asset = assetMapper.fromEO(assetCreateEO);
        asset.setResponsibleUsers(responsibleUsers);
        if (assetCreateEO.getResponsibleUsers() != null) {
            addManagers(assetCreateEO.getResponsibleUsers(), asset);
        }
        if (assetCreateEO.getSupplier() != null) {
            setSupplier(assetCreateEO.getSupplier(), asset);
        }
        if (assetCreateEO.getSubSuppliers() != null) {
            addSubSuppliers(assetCreateEO.getSubSuppliers(), asset);
        }
		if (assetCreateEO.getProductLinks() != null) {
			addProductLinks(assetCreateEO.getProductLinks(), asset);
		}
        return assetMapper.toEO(assetService.create(asset));
    }


    @Operation(summary = "Update an asset", description = "Updates an asset, the client should make a GET request first to ensure they have the newest version, update the fields they need and then call this method with the complete entity. <br><u>NOTICE! properties are used by multiple parties so make sure to never remove unknown properties, and prefix your properties so they are unique</u>")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No content", content = @Content(mediaType = "application/json", schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "409", description = "Conflicting version", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PutMapping(value = "{id}", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable final Long id, @Valid @RequestBody final AssetUpdateEO assetUpdateEO) {
        final Asset asset = assetService.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (asset.getVersion() != assetUpdateEO.getVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Version mismatch");
        }
        final List<User> responsibleUsers = userService.findAllByUuids(nullSafe(() -> assetUpdateEO.getSystemOwners().stream().map(s -> s.getUuid()).collect(Collectors.toSet())));
        final ChoiceList assetTypeChoiceList = choiceService.findChoiceList("asset-type")
            .orElseThrow( () ->new ResponseStatusException(HttpStatus.BAD_REQUEST, "No asset types found"));
        final ChoiceValue assetType = assetTypeChoiceList.getValues().stream()
            .filter(value -> value.getIdentifier().equals(assetUpdateEO.getAssetType().getIdentifier()) )
            .findAny()
            .orElseThrow(() ->new ResponseStatusException(HttpStatus.BAD_REQUEST, "AssetType identifier is not valid"));
        asset.setName(assetUpdateEO.getName());
        asset.setResponsibleUsers(responsibleUsers);
        asset.setDescription(assetUpdateEO.getDescription());
        asset.setAssetType(assetType);
        asset.setDataProcessingAgreementStatus(nullSafe(() -> DataProcessingAgreementStatus.valueOf(assetUpdateEO.getDataProcessingAgreementStatus().name())));
        asset.setDataProcessingAgreementDate(assetUpdateEO.getDataProcessingAgreementDate());
        asset.setDataProcessingAgreementLink(assetUpdateEO.getDataProcessingAgreementLink());
        asset.setSupervisoryModel(nullSafe(() -> ChoiceOfSupervisionModel.valueOf(assetUpdateEO.getSupervisoryModel().name())));
        asset.setNextInspection(nullSafe(() -> NextInspection.valueOf(assetUpdateEO.getNextInspection().name())));
        asset.setNextInspectionDate(assetUpdateEO.getNextInspectionDate());
        asset.setAssetStatus(nullSafe(() -> AssetStatus.valueOf(assetUpdateEO.getAssetStatus().name())));
        asset.setCriticality(nullSafe(() -> Criticality.valueOf(assetUpdateEO.getCriticality().name())));
        asset.setSociallyCritical(assetUpdateEO.isSociallyCritical());
        asset.setEmergencyPlanLink(assetUpdateEO.getEmergencyPlanLink());
        asset.setReEstablishmentPlanLink(assetUpdateEO.getReEstablishmentPlanLink());
        asset.setContractLink(assetUpdateEO.getContractLink());
        asset.setContractDate(assetUpdateEO.getContractDate());
        asset.setContractTermination(assetUpdateEO.getContractTermination());
        asset.setTerminationNotice(assetUpdateEO.getTerminationNotice());
        asset.setArchive(assetUpdateEO.isArchive());
        if (assetUpdateEO.getSupplier() != null) {
            setSupplier(assetUpdateEO.getSupplier(), asset);
        } else {
            assetUpdateEO.setSupplier(null);
        }
        asset.getManagers().clear();
        if (assetUpdateEO.getResponsibleUsers() != null) {
            addManagers(assetUpdateEO.getResponsibleUsers(), asset);
        }
        asset.getSuppliers().clear();
        if (assetUpdateEO.getSubSuppliers() != null) {
            addSubSuppliers(assetUpdateEO.getSubSuppliers(), asset);
        }
		asset.getProductLinks().clear();
		if (assetUpdateEO.getProductLinks() != null) {
			addProductLinks(assetUpdateEO.getProductLinks(), asset);
		}
        asset.getProperties().clear();
        asset.getProperties().addAll(assetMapper.fromEO(assetUpdateEO.getProperties()));
    }

    @Operation(summary = "Delete an asset", description = "Deletes an asset")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No content", content = @Content(mediaType = "application/json", schema = @Schema())),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @DeleteMapping(value = "{id}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final Long id) {
        final Asset asset = assetService.get(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
        assetService.delete(asset);
    }

    private void addSubSuppliers(final List<SupplierWriteEO> assetUpdateEO, final Asset asset) {
        assetUpdateEO.stream()
            .map(s -> supplierService.get(s.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sub supplier not found")))
            .map(s -> AssetSupplierMapping.builder()
                .asset(asset)
                .supplier(s)
                .build())
            .forEach(s -> asset.getSuppliers().add(s));
    }

    private void addManagers(final List<UserWriteEO> assetCreateEO, final Asset asset) {
        final List<User> a = assetCreateEO.stream()
            .map(r -> userService.get(r.getUuid()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsible user not found")))
            .toList();
        asset.getManagers().addAll(a);
    }

	private void addProductLinks(final List<String> productLinksEO, final Asset asset) {
		final List<AssetProductLink> productLinks = productLinksEO.stream()
				.map(l -> new AssetProductLink(null, l, asset))
				.toList();
		asset.getProductLinks().addAll(productLinks);
	}

    private void setSupplier(final SupplierWriteEO assetCreateEO, final Asset asset) {
        final Supplier supplier = supplierService.get(assetCreateEO.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier not found"));
        asset.setSupplier(supplier);
    }

}
