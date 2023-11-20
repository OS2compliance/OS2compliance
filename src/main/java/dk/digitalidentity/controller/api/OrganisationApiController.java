package dk.digitalidentity.controller.api;

import dk.digitalidentity.mapping.OrganisationUnitMapper;
import dk.digitalidentity.model.api.OrganisationUnitEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.service.OrganisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value = "/api/v1/organisations")
@Tag(name = "Organisations resource")
public class OrganisationApiController {
    private final OrganisationService organisationService;
    private final OrganisationUnitMapper organisationUnitMapper;

    public OrganisationApiController(final OrganisationService organisationService, final OrganisationUnitMapper organisationUnitMapper) {
        this.organisationService = organisationService;
        this.organisationUnitMapper = organisationUnitMapper;
    }

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


}
