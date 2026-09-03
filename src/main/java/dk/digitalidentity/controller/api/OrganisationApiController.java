package dk.digitalidentity.controller.api;

import dk.digitalidentity.mapping.OrganisationUnitMapper;
import dk.digitalidentity.model.api.ErrorEO;
import dk.digitalidentity.model.api.OrganisationUnitCreateEO;
import dk.digitalidentity.model.api.OrganisationUnitEO;
import dk.digitalidentity.model.api.OrganisationUnitUpdateEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.service.OrganisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organisations")
@Tag(name = "Organisations resource")
@RequiredArgsConstructor
public class OrganisationApiController {
    private static final String SYNC_NOTICE = "<br><u>NOTICE! If organisation data is synchronised from FK Organisation (OS2sync), organisation units not present in the synchronised hierarchy will be deactivated by the next synchronisation run</u>";

    private final OrganisationService organisationService;
    private final OrganisationUnitMapper organisationUnitMapper;


    @Operation(summary = "Fetch an organisation unit")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The organisation unit"),
        @ApiResponse(responseCode = "404", description = "Organisation unit not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "{uuid}", produces = "application/json")
    public OrganisationUnitEO read(@PathVariable final String uuid) {
        final OrganisationUnit ou = organisationService.get(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OrganisationUnit not found"));
        return organisationUnitMapper.toEO(ou);
    }

    @Operation(summary = "List all organisation units")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All organisation units"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(produces = "application/json")
    public PageEO<OrganisationUnitEO> list(@Parameter(description = "Page size to fetch, max is 500")  @RequestParam(value = "pageSize", defaultValue = "100") @Max(500) final int pageSize,
                                           @Parameter(description = "The page to fetch, first page is 0") @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero final int page) {
        return organisationUnitMapper.toEO(organisationService.getPaged(pageSize, page));
    }

    @Operation(summary = "Create a new organisation unit", description = "Creates an organisation unit, optionally placed under a parent unit." + SYNC_NOTICE)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "The created organisation unit"),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "409", description = "An organisation unit with the given uuid already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PostMapping(produces = "application/json", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public OrganisationUnitEO create(@Valid @RequestBody final OrganisationUnitCreateEO createEO) {
        final String uuid = StringUtils.trimToNull(createEO.getUuid());
        if (organisationService.get(uuid).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An organisation unit with the given uuid already exists");
        }
        final OrganisationUnit ou = organisationUnitMapper.fromEO(createEO);
        ou.setUuid(uuid != null ? uuid : UUID.randomUUID().toString());
        if (ou.getActive() == null) {
            ou.setActive(true);
        }
        assertValidParent(ou.getUuid(), createEO.getParentUuid());
        try {
            return organisationUnitMapper.toEO(organisationService.create(ou));
        } catch (final DataIntegrityViolationException | PersistenceException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An organisation unit with the given uuid already exists");
        }
    }

    @Operation(summary = "Update an organisation unit", description = "Updates an organisation unit, the client should make a GET request first to ensure they have the newest version, update the fields they need and then call this method with the complete entity. "
        + "Note that there is no version check, concurrent updates of the same organisation unit are last-write-wins." + SYNC_NOTICE)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No content", content = @Content(mediaType = "application/json", schema = @Schema())),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "404", description = "Organisation unit not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PutMapping(value = "{uuid}", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable final String uuid, @Valid @RequestBody final OrganisationUnitUpdateEO updateEO) {
        final OrganisationUnit ou = organisationService.get(uuid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OrganisationUnit not found"));
        assertValidParent(uuid, updateEO.getParentUuid());
        ou.setName(updateEO.getName());
        ou.setParentUuid(updateEO.getParentUuid());
        ou.setActive(updateEO.getActive());
        organisationService.save(ou);
    }

    private void assertValidParent(final String uuid, final String parentUuid) {
        if (parentUuid == null) {
            return;
        }
        // walk the ancestor chain to reject a parent that would make the hierarchy cyclic
        final Set<String> seen = new HashSet<>();
        String current = parentUuid;
        while (current != null) {
            if (current.equals(uuid)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The given parent would create a cycle in the organisation hierarchy");
            }
            if (!seen.add(current)) {
                // a cycle that does not involve the updated unit predates this request
                log.warn("Organisation hierarchy already contains a cycle involving {}", current);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The existing organisation hierarchy contains a cycle");
            }
            final OrganisationUnit ancestor = organisationService.get(current).orElse(null);
            if (ancestor == null) {
                if (current.equals(parentUuid)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent organisation unit not found: " + parentUuid);
                }
                // a dangling ancestor reference in existing data is not the client's fault
                break;
            }
            current = ancestor.getParentUuid();
        }
    }

}
