package dk.digitalidentity.controller.api;

import dk.digitalidentity.mapping.SupplierMapper;
import dk.digitalidentity.model.api.ErrorEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.SupplierCreateEO;
import dk.digitalidentity.model.api.SupplierEO;
import dk.digitalidentity.model.api.SupplierUpdateEO;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.SupplierStatus;
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

import static dk.digitalidentity.util.NullSafe.nullSafe;

@RestController
@RequestMapping(value = "/api/v1/suppliers")
@Tag(name = "Supplier resource")
@RequiredArgsConstructor
public class SupplierApiController {
    private final SupplierService supplierService;
    private final SupplierMapper supplierMapper;
    private final UserService userService;


    @Operation(summary = "Fetch a supplier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Get a supplier"),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @GetMapping(value = "{id}", produces = "application/json")
    public SupplierEO read(@PathVariable final Long id) {
        final Supplier supplier = supplierService.get(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        return supplierMapper.toEO(supplier);
    }

    @Operation(summary = "Fetch all suppliers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All suppliers"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @GetMapping(produces = "application/json")
    public PageEO<SupplierEO> list(@Parameter(description = "Page size to fetch, max is 500") @RequestParam(value = "pageSize", defaultValue = "100") @Max(500) final int pageSize,
                                   @Parameter(description = "The page to fetch, first page is 0") @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero final int page) {
        return supplierMapper.toEO(supplierService.getPaged(pageSize, page));
    }

    @Operation(summary = "Create a new supplier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "The created supplier"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PostMapping(produces = "application/json", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierEO create(@Valid @RequestBody final SupplierCreateEO supplierCreateEO) {
        final User responsibleUser = userService.get(nullSafe(() -> supplierCreateEO.getResponsibleUser().getUuid()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsible user not valid"));
        final Supplier supplier = supplierMapper.fromEO(supplierCreateEO);
        supplier.setResponsibleUser(responsibleUser);
        return supplierMapper.toEO(supplierService.create(supplier));
    }

    @Operation(summary = "Update a supplier", description = "Updates a supplier, the client should make a GET request first to ensure they have the newest version, update the fields they need and then call this method with the complete entity. <br><u>NOTICE! properties are used by multiple parties so make sure to never remove unknown properties, and prefix your properties so they are unique</u>")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No content", content = @Content(mediaType = "application/json", schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "409", description = "Conflicting version", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PutMapping(value = "{id}", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable final Long id, @Valid @RequestBody final SupplierUpdateEO supplierUpdateEO) {
        final Supplier supplier = supplierService.get(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        if (supplier.getVersion() != supplierUpdateEO.getVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Version mismatch");
        }
        final User responsibleUser = userService.get(nullSafe(() -> supplierUpdateEO.getResponsibleUser().getUuid()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsible user not valid"));
        supplier.setVersion(supplierUpdateEO.getVersion());
        supplier.setName(supplierUpdateEO.getName());
        supplier.setResponsibleUser(responsibleUser);
        supplier.setStatus(SupplierStatus.valueOf(supplierUpdateEO.getStatus().name()));
        supplier.setCvr(supplierUpdateEO.getCvr());
        supplier.setZip(supplierUpdateEO.getZip());
        supplier.setCity(supplierUpdateEO.getCity());
        supplier.setAddress(supplierUpdateEO.getAddress());
        supplier.setContact(supplierUpdateEO.getContact());
        supplier.setPhone(supplierUpdateEO.getPhone());
        supplier.setEmail(supplierUpdateEO.getEmail());
        supplier.setCountry(supplierUpdateEO.getCountry());
        supplier.setDescription(supplierUpdateEO.getDescription());
        supplier.getProperties().clear();
        supplier.getProperties().addAll(supplierMapper.fromEO(supplierUpdateEO.getProperties()));
        supplierService.update(supplier);
    }

    @Operation(summary = "Delete a supplier", description = "Deletes a supplier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No content", content = @Content(mediaType = "application/json", schema = @Schema())),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @DeleteMapping(value = "{id}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final Long id) {
        final Supplier supplier = supplierService.get(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        supplierService.delete(supplier);
    }

}
