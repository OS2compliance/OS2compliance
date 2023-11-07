package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.dao.grid.RiskGridDao;
import dk.digitalidentity.mapping.RiskMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.dto.enums.SetFieldType;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.RiskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@Slf4j
@RestController
@RequestMapping("rest/risks")
@RequireUser
public class RiskRestController {
    @Autowired
    private RegisterDao registerDao;
    @Autowired
    private AssetDao assetDao;
    @Autowired
    private RiskService riskService;
    @Autowired
    private ThreatAssessmentDao threatAssessmentDao;

    private final RiskGridDao riskGridDao;
    private final RiskMapper mapper;

    public RiskRestController(final RiskGridDao riskGridDao, final RiskMapper mapper) {
        this.riskGridDao = riskGridDao;
        this.mapper = mapper;
    }

    @PostMapping("list")
    public PageDTO<RiskDTO> list(
            @RequestParam(name = "search", required = false) final String search,
            @RequestParam(name = "page", required = false) final Integer page,
            @RequestParam(name = "size", required = false) final Integer size,
            @RequestParam(name = "order", required = false) final String order,
            @RequestParam(name = "dir", required = false) final String dir
    ) {
        Sort sort = null;
        if (isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        }
        final Pageable sortAndPage = sort != null ?  PageRequest.of(page, size, sort) : PageRequest.of(page, size);
        Page<RiskGrid> risks = null;
        if (StringUtils.isNotEmpty(search)) {
            final List<String> searchableProperties = Arrays.asList("name", "responsibleUser.name", "responsibleOU.name", "date", "localizedEnums");
            risks = riskGridDao.findAllCustom(searchableProperties, search, sortAndPage, RiskGrid.class);
        } else {
            // Fetch paged and sorted
            risks = riskGridDao.findAll(sortAndPage);
        }
        assert risks != null;
        return new PageDTO<>(risks.getTotalElements(), mapper.toDTO(risks.getContent()));
    }

    record ResponsibleUserDTO(String elementName, String uuid, String name, String userId) {}
    @GetMapping("register")
    public ResponsibleUserDTO getRegisterResponsibleUserAndName(@RequestParam final long registerId) {
        final Register register = registerDao.findById(registerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (register.getResponsibleUser() == null) {
            return new ResponsibleUserDTO(register.getName(),null, null, null);
        }
        return new ResponsibleUserDTO(register.getName(), register.getResponsibleUser().getUuid(), register.getResponsibleUser().getName(), register.getResponsibleUser().getUserId());
    }

    record RiskUIDTO(String elementName, int rf, int of, int ri, int oi, int rt, int ot, ResponsibleUserDTO user) {}
    @GetMapping("asset")
    public RiskUIDTO getAssetResponsibleUserAndName(@RequestParam final long assetId) {
        final Asset asset = assetDao.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ResponsibleUserDTO user = getUser(asset);
        final dk.digitalidentity.service.model.RiskDTO riskDTO = riskService.calculateRiskFromRegisters(asset);
        return new RiskUIDTO(asset.getName(), riskDTO.getRf(), riskDTO.getOf(), riskDTO.getRi(), riskDTO.getOi(), riskDTO.getRt(), riskDTO.getOt(), user);
    }

    record SetFieldDTO(@NotNull SetFieldType setFieldType, @NotNull ThreatDatabaseType dbType, Long id, String identifier, @NotNull String value) {}
    @PostMapping("{id}/threats/setfield")
    public ResponseEntity<HttpStatus> setField(@PathVariable final long id, @Valid @RequestBody final SetFieldDTO dto) {
        final ThreatAssessment threatAssessment = threatAssessmentDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (threatAssessment.getThreatAssessmentResponses() == null) {
            threatAssessment.setThreatAssessmentResponses(new ArrayList<>());
        }

        ThreatAssessmentResponse response = null;
        if (dto.dbType().equals(ThreatDatabaseType.CATALOG)) {
            final ThreatCatalogThreat threat = threatAssessment.getThreatCatalog().getThreats().stream().filter(t -> t.getIdentifier().equals(dto.identifier())).findAny().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            response = threatAssessment.getThreatAssessmentResponses().stream().filter(r -> r.getThreatCatalogThreat() != null && r.getThreatCatalogThreat().getIdentifier().equals(threat.getIdentifier())).findAny().orElse(null);
            if (response == null) {
                response = createResponse(threatAssessment, threat, null);
            }
        } else if (dto.dbType().equals(ThreatDatabaseType.CUSTOM)) {
            final CustomThreat threat = threatAssessment.getCustomThreats().stream().filter(t -> t.getId().equals(dto.id())).findAny().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            response = threatAssessment.getThreatAssessmentResponses().stream().filter(r -> r.getCustomThreat() != null && r.getCustomThreat().getId() == threat.getId()).findAny().orElse(null);
            if (response == null) {
                response = createResponse(threatAssessment, null, threat);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        switch (dto.setFieldType()) {
            case NOT_RELEVANT -> handleNotRelevant(Boolean.parseBoolean(dto.value()), response);
            case PROBABILITY -> response.setProbability(Integer.parseInt(dto.value()));
            case RF -> response.setConfidentialityRegistered(Integer.parseInt(dto.value()));
            case RI -> response.setIntegrityRegistered(Integer.parseInt(dto.value()));
            case RT -> response.setAvailabilityRegistered(Integer.parseInt(dto.value()));
            case OF -> response.setConfidentialityOrganisation(Integer.parseInt(dto.value()));
            case OI -> response.setIntegrityOrganisation(Integer.parseInt(dto.value()));
            case OT -> response.setAvailabilityOrganisation(Integer.parseInt(dto.value()));
            case PROBLEM -> response.setProblem(dto.value());
            case EXISTING_MEASURES -> response.setExistingMeasures(dto.value());
            case METHOD -> response.setMethod(ThreatMethod.valueOf(dto.value()));
            case ELABORATION -> response.setElaboration(dto.value());
            case RESIDUAL_RISK_PROBABILITY -> response.setResidualRiskProbability(Integer.parseInt(dto.value()));
            case RESIDUAL_RISK_CONSEQUENCE -> response.setResidualRiskConsequence(Integer.parseInt(dto.value()));
        }

        final ThreatAssessment savedThreatAssessment = threatAssessmentDao.save(threatAssessment);
        riskService.setThreatAssessmentColor(savedThreatAssessment);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void handleNotRelevant(final boolean notRelevant, final ThreatAssessmentResponse response) {
        response.setNotRelevant(notRelevant);

        if (notRelevant) {
            response.setProbability(null);
            response.setConfidentialityRegistered(null);
            response.setIntegrityRegistered(null);
            response.setAvailabilityRegistered(null);
            response.setConfidentialityOrganisation(null);
            response.setIntegrityOrganisation(null);
            response.setAvailabilityOrganisation(null);
        }
    }

    private ThreatAssessmentResponse createResponse(final ThreatAssessment threatAssessment, final ThreatCatalogThreat threatCatalogThreat, final CustomThreat customThreat) {
        final ThreatAssessmentResponse response = new ThreatAssessmentResponse();
        response.setMethod(ThreatMethod.NONE);
        response.setThreatAssessment(threatAssessment);
        response.setCustomThreat(customThreat);
        response.setThreatCatalogThreat(threatCatalogThreat);
        threatAssessment.getThreatAssessmentResponses().add(response);
        return response;
    }

    private ResponsibleUserDTO getUser(final Asset asset) {
        if (asset.getResponsibleUser() == null) {
            return new ResponsibleUserDTO(asset.getName(), null, null, null);
        }
        return new ResponsibleUserDTO(asset.getName(), asset.getResponsibleUser().getUuid(), asset.getResponsibleUser().getName(), asset.getResponsibleUser().getUserId());
    }

    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("type") || fieldName.equals("user") || fieldName.equals("ou")
                || fieldName.equals("date") || fieldName.equals("tasks") || fieldName.equals("assessment");
    }

}
