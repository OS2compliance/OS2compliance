package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.RiskGridDao;
import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.event.ThreatAssessmentUpdatedEvent;
import dk.digitalidentity.mapping.RiskMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.dto.enums.SetFieldType;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import dk.digitalidentity.report.DocsReportGeneratorComponent;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.PrecautionService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.RISK_ASSESSMENT_TEMPLATE_DOC;
import static dk.digitalidentity.report.DocxService.PARAM_RISK_ASSESSMENT_ID;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@SuppressWarnings("ClassEscapesDefinedScope")
@Slf4j
@RestController
@RequestMapping("rest/risks")
@RequireUser
@RequiredArgsConstructor
public class RiskRestController {
    private final ApplicationEventPublisher eventPublisher;
    private final RegisterService registerService;
    private final AssetService assetService;
    private final ThreatAssessmentService threatAssessmentService;
    private final DocsReportGeneratorComponent docsReportGeneratorComponent;
    private final RelationService relationService;
    private final RiskGridDao riskGridDao;
    private final RiskMapper mapper;
    private final UserService userService;
    private final PrecautionService precautionService;

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
        final Page<RiskGrid> risks;
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

    record ResponsibleUserDTO(String uuid, String name, String userId) {}
    record ResponsibleUsersWithElementNameDTO(String elementName, List<ResponsibleUserDTO> users) {}
    @GetMapping("register")
    public ResponsibleUsersWithElementNameDTO getRegisterResponsibleUserAndName(@RequestParam final long registerId) {
        final Register register = registerService.findById(registerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (register.getResponsibleUsers() == null || register.getResponsibleUsers().isEmpty()) {
            return new ResponsibleUsersWithElementNameDTO(register.getName(),new ArrayList<>());
        }

        final List<ResponsibleUserDTO> users = register.getResponsibleUsers().stream().map(r -> new ResponsibleUserDTO(r.getUuid(), r.getName(), r.getUserId())).collect(Collectors.toList());
        return new ResponsibleUsersWithElementNameDTO(register.getName(), users);
    }

    record RiskUIDTO(String elementName, int rf, int of, int ri, int oi, int rt, int ot, ResponsibleUsersWithElementNameDTO users) {}
    @GetMapping("asset")
    public RiskUIDTO getRelatedAsset(@RequestParam final Set<Long> assetIds) {
        final List<Asset> assets = assetService.findAllById(assetIds);
        final ResponsibleUsersWithElementNameDTO users = !assets.isEmpty() ? getUser(assets.get(0)) : null;
        final dk.digitalidentity.service.model.RiskDTO riskDTO = threatAssessmentService.calculateRiskFromRegisters(assets.stream()
            .map(Relatable::getId).collect(Collectors.toList()));
        final String elementName = assets.isEmpty() ? null : assets.stream().map(Relatable::getName).collect(Collectors.joining(", "));
        return new RiskUIDTO(elementName, riskDTO.getRf(), riskDTO.getOf(), riskDTO.getRi(), riskDTO.getOi(), riskDTO.getRt(), riskDTO.getOt(), users);
    }

    @Transactional
    @PostMapping("{id}/mailReport")
    public ResponseEntity<?> mailReportToSystemOwner(@PathVariable final long id) throws IOException {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final List<EmailEvent> emailEvents = buildEmailEventsToRelatedResponsible(threatAssessment);
        if (emailEvents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No responsible found");
        }
        try (final XWPFDocument myDocument = docsReportGeneratorComponent.generateDocument(RISK_ASSESSMENT_TEMPLATE_DOC,
            Map.of(PARAM_RISK_ASSESSMENT_ID, "" + id))) {
            final File outputFile = File.createTempFile(UUID.randomUUID().toString(), ".docx");
            myDocument.write(new FileOutputStream(outputFile));
            emailEvents.forEach(e -> e.getAttachments().add(
                new EmailEvent.EmailAttachement(outputFile.getAbsolutePath(),
                    "Ledelsesrapport for risikovurdering af " + threatAssessment.getName() + ".docx"))
            );
        }
        emailEvents.forEach(eventPublisher::publishEvent);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    record SetFieldDTO(@NotNull SetFieldType setFieldType, @NotNull ThreatDatabaseType dbType, Long id, String identifier, @NotNull String value) {}
    @PostMapping("{id}/threats/setfield")
    public ResponseEntity<HttpStatus> setField(@PathVariable final long id, @Valid @RequestBody final SetFieldDTO dto) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (threatAssessment.getThreatAssessmentResponses() == null) {
            threatAssessment.setThreatAssessmentResponses(new ArrayList<>());
        }

        final ThreatAssessmentResponse response = getRelevantResponse(threatAssessment, dto.dbType, dto.id, dto.identifier);
        if (response == null) {
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

        final ThreatAssessment savedThreatAssessment = threatAssessmentService.save(threatAssessment);
        threatAssessmentService.setThreatAssessmentColor(savedThreatAssessment);
        eventPublisher.publishEvent(ThreatAssessmentUpdatedEvent.builder().threatAssessmentId(id).build());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    record SetPrecautionsDTO(@NotNull ThreatDatabaseType threatType, Long threatId, String threatIdentifier, @NotNull List<Long> precautionIds) {}
    @PostMapping("{id}/threats/setPrecautions")
    public ResponseEntity<HttpStatus> setPrecautions(@PathVariable final long id, @Valid @RequestBody final SetPrecautionsDTO dto) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (threatAssessment.getThreatAssessmentResponses() == null) {
            threatAssessment.setThreatAssessmentResponses(new ArrayList<>());
        }

        final ThreatAssessmentResponse response = getRelevantResponse(threatAssessment, dto.threatType, dto.threatId, dto.threatIdentifier);
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // create response precaution relations
        final List<Relation> existingPrecautionRelations = relationService.findRelatedToWithType(response, RelationType.PRECAUTION);
        for (final long value : dto.precautionIds) {
            final Optional<Precaution> precautionOptional = precautionService.get(value);
            if (precautionOptional.isPresent() && existingPrecautionRelations.stream().noneMatch(r -> (r.getRelationAId() == value && r.getRelationAType().equals(RelationType.PRECAUTION)) || (r.getRelationBId() == value && r.getRelationBType().equals(RelationType.PRECAUTION)))) {
                relationService.addRelation(precautionOptional.get(), response);
            }
        }

        // delete response precaution relations
        final List<Relation> toDelete = new ArrayList<>();
        for (final Relation existingPrecautionRelation : existingPrecautionRelations) {
            final long idToLookFor = existingPrecautionRelation.getRelationAType().equals(RelationType.PRECAUTION) ? existingPrecautionRelation.getRelationAId() : existingPrecautionRelation.getRelationBId();
            if (!dto.precautionIds.contains(idToLookFor)) {
                toDelete.add(existingPrecautionRelation);
            }
        }
        relationService.deleteAll(toDelete);

        // update asset precaution relations
        final List<Relation> relatedAssets = relationService.findRelatedToWithType(threatAssessment, RelationType.ASSET);
        for (final Relation assetRelation : relatedAssets) {
            final Optional<Asset> assetOptional = assetService.get(assetRelation.getRelationAType().equals(RelationType.ASSET) ? assetRelation.getRelationAId() : assetRelation.getRelationBId());
            if (assetOptional.isPresent()) {
                final Asset asset = assetOptional.get();
                final List<Relation> existingAssetPrecautionRelations = relationService.findRelatedToWithType(asset, RelationType.PRECAUTION);
                for (final long value : dto.precautionIds) {
                    final Optional<Precaution> precautionOptional = precautionService.get(value);
                    if (precautionOptional.isPresent() && existingAssetPrecautionRelations.stream().noneMatch(r -> (r.getRelationAId() == value && r.getRelationAType().equals(RelationType.PRECAUTION)) || (r.getRelationBId() == value && r.getRelationBType().equals(RelationType.PRECAUTION)))) {
                        relationService.addRelation(precautionOptional.get(), asset);
                    }
                }
            }
        }


        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ThreatAssessmentResponse getRelevantResponse(final ThreatAssessment threatAssessment, final ThreatDatabaseType threatType, final Long threatId, final String threatIdentifier) {
        ThreatAssessmentResponse response = null;
        if (threatType.equals(ThreatDatabaseType.CATALOG)) {
            final ThreatCatalogThreat threat = threatAssessment.getThreatCatalog().getThreats().stream().filter(t -> t.getIdentifier().equals(threatIdentifier)).findAny().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            response = threatAssessment.getThreatAssessmentResponses().stream()
                .filter(r -> r.getThreatCatalogThreat() != null && r.getThreatCatalogThreat().getIdentifier().equals(threat.getIdentifier()))
                .findAny()
                .orElse(null);
            if (response == null) {
                response = threatAssessmentService.createResponse(threatAssessment, threat, null);
            }
        } else if (threatType.equals(ThreatDatabaseType.CUSTOM)) {
            final CustomThreat threat = threatAssessment.getCustomThreats().stream().filter(t -> t.getId().equals(threatId)).findAny().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            response = threatAssessment.getThreatAssessmentResponses().stream()
                .filter(r -> r.getCustomThreat() != null && Objects.equals(r.getCustomThreat().getId(), threat.getId()))
                .findAny()
                .orElse(null);
            if (response == null) {
                response = threatAssessmentService.createResponse(threatAssessment, null, threat);
            }
        }

        return response;
    }

    @Transactional
    @DeleteMapping("{id}/threats/{threatId}")
    public ResponseEntity<HttpStatus> deleteCustomThread(@PathVariable final long id, @PathVariable final long threatId) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final CustomThreat customThreat = threatAssessment.getCustomThreats().stream()
            .filter(c -> c.getId() == threatId)
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        threatAssessment.getThreatAssessmentResponses().stream()
            .filter(r -> r.getCustomThreat() != null && r.getCustomThreat().getId() == threatId)
            .findFirst().ifPresent(r -> {
                relationService.deleteRelatedTo(r.getId());
                threatAssessment.getThreatAssessmentResponses().remove(r);
            });
        threatAssessment.getCustomThreats().remove(customThreat);
        threatAssessmentService.save(threatAssessment);
        eventPublisher.publishEvent(ThreatAssessmentUpdatedEvent.builder().threatAssessmentId(id).build());

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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

    private ResponsibleUsersWithElementNameDTO getUser(final Asset asset) {
        if (asset.getResponsibleUsers() == null || asset.getResponsibleUsers().isEmpty()) {
            return new ResponsibleUsersWithElementNameDTO(asset.getName(), new ArrayList<>());
        }
        final List<ResponsibleUserDTO> users = asset.getResponsibleUsers().stream().map(r -> new ResponsibleUserDTO(r.getUuid(), r.getName(), r.getUserId())).collect(Collectors.toList());
        return new ResponsibleUsersWithElementNameDTO(asset.getName(), users);
    }

    private boolean containsField(final String fieldName) {
        return fieldName.equals("name") || fieldName.equals("type") || fieldName.equals("responsibleUser.name") || fieldName.equals("responsibleOU.name")
                || fieldName.equals("date") || fieldName.equals("tasks") || fieldName.equals("assessment") || fieldName.equals("assessmentOrder");
    }

    /**
     * Will create email events, where the receiver is the related asset/register's responsible user
     */
    private List<EmailEvent> buildEmailEventsToRelatedResponsible(final ThreatAssessment threatAssessment) {
        final User user = userService.currentUser();
        final String responsibleUserName = user != null ? user.getName() : "";
        if (threatAssessment.getThreatAssessmentType() == ThreatAssessmentType.ASSET) {
            final List<EmailEvent> events = new ArrayList<>();
            final List<Asset> assets =  relationService.findRelatedToWithType(threatAssessment, RelationType.ASSET).stream()
                .map(a -> a.getRelationAType() == RelationType.ASSET ? a.getRelationAId() : a.getRelationBId())
                .map(assetService::findById)
                .filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toList());
            for (final Asset asset : assets) {
                if (asset.getResponsibleUsers() != null) {
                    for (final User responsibleUser : asset.getResponsibleUsers()) {
                        if (responsibleUser.getEmail() != null) {
                            events.add(EmailEvent.builder()
                                .email(responsibleUser.getEmail())
                                .subject("Risikovurdering for " + asset.getName())
                                .message(responsibleUserName + " har sendt dig en risikovurdering for systemet " + asset.getName())
                                .build());
                        }
                    }
                }
            }

            return  events;
        } else if (threatAssessment.getThreatAssessmentType() == ThreatAssessmentType.REGISTER) {
            final List<EmailEvent> events = new ArrayList<>();
            final List<Register> registers = relationService.findRelatedToWithType(threatAssessment, RelationType.REGISTER).stream()
                .map(a -> a.getRelationAType() == RelationType.REGISTER ? a.getRelationAId() : a.getRelationBId())
                .map(registerService::findById)
                .filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toList());

            for (final Register register : registers) {
                if (register.getResponsibleUsers() != null) {
                    for (final User responsibleUser : register.getResponsibleUsers()) {
                        if (responsibleUser.getEmail() != null) {
                            events.add(EmailEvent.builder()
                                .email(responsibleUser.getEmail())
                                .subject("Risikovurdering for " + register.getName())
                                .message(responsibleUserName + " har sendt dig en risikovurdering for behandlingsaktiviteten " + register.getName())
                                .build());
                        }
                    }
                }
            }

            return events;
        }
        return Collections.emptyList();
    }

}
