package dk.digitalidentity.controller.rest;

import dk.digitalidentity.dao.grid.RiskGridDao;
import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.event.ThreatAssessmentUpdatedEvent;
import dk.digitalidentity.mapping.RiskMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.dto.enums.ReportFormat;
import dk.digitalidentity.model.dto.enums.SetFieldType;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.S3Document;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.model.entity.enums.ThreatDatabaseType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import dk.digitalidentity.report.DocsReportGeneratorComponent;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.EmailTemplateService;
import dk.digitalidentity.service.OrganisationService;
import dk.digitalidentity.service.PrecautionService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.S3DocumentService;
import dk.digitalidentity.service.S3Service;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.RISK_ASSESSMENT_TEMPLATE_DOC;
import static dk.digitalidentity.report.DocxService.PARAM_RISK_ASSESSMENT_ID;
import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

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
    private final S3Service s3Service;
    private final S3DocumentService s3DocumentService;
    private final Environment environment;
    private final EmailTemplateService emailTemplateService;
	private final OrganisationService organisationService;

    @PostMapping("list")
    public PageDTO<RiskDTO> list(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "limit", defaultValue = "50") int limit,
        @RequestParam(value = "order", required = false) String sortColumn,
        @RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
        @RequestParam Map<String, String> filters // Dynamic filters for search fields
    ) {
        Page<RiskGrid> risks =  riskGridDao.findAllWithColumnSearch(
            validateSearchFilters(filters, RiskGrid.class),
            null,
            buildPageable(page, limit, sortColumn, sortDirection),
            RiskGrid.class
        );

        assert risks != null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new PageDTO<>(risks.getTotalElements(), mapper.toDTO(risks.getContent(), authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), SecurityUtil.getPrincipalUuid()));
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

    record MailReportDTO(String message, String sendTo, ReportFormat format, boolean sign) {}
    @Transactional
    @PostMapping("{id}/mailReport")
    public ResponseEntity<?> mailReportToSystemOwner(@PathVariable final long id, @RequestBody final MailReportDTO dto) throws IOException {
        ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        ThreatAssessment finalThreatAssessment = threatAssessment;
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !finalThreatAssessment.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        final User responsibleUser = userService.findByUuid(dto.sendTo).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Den valgte bruger kunne ikke findes, og rapporten kan derfor ikke sendes."));
        if (responsibleUser.getEmail() == null || responsibleUser.getEmail().isBlank()) {
            return new ResponseEntity<>("Den valgte bruger har ikke nogen email tilknyttet, og rapporten kan derfor ikke sendes.", HttpStatus.BAD_REQUEST);
        }
        if (dto.format == null) {
            return new ResponseEntity<>("Der skal vælges et format", HttpStatus.BAD_REQUEST);
        }

        final User user = userService.currentUser();
        S3Document s3Document = null;

        final EmailEvent emailEvent = EmailEvent.builder()
            .email(responsibleUser.getEmail())
            .build();

        if (dto.format.equals(ReportFormat.WORD)) {
            try (final XWPFDocument myDocument = docsReportGeneratorComponent.generateDocument(RISK_ASSESSMENT_TEMPLATE_DOC,
                Map.of(PARAM_RISK_ASSESSMENT_ID, "" + id))) {
                final File outputFile = File.createTempFile(UUID.randomUUID().toString(), ".docx");
                myDocument.write(new FileOutputStream(outputFile));
                emailEvent.getAttachments().add(new EmailEvent.EmailAttachement(outputFile.getAbsolutePath(),
                    "Ledelsesrapport for risikovurdering af " + threatAssessment.getName() + ".docx"));
            }
        } else if (dto.format.equals(ReportFormat.PDF)) {
            byte[] byteData = threatAssessmentService.getThreatAssessmentPdf(threatAssessment);
            String uuid = UUID.randomUUID().toString();

            if (dto.sign) {
                String key = s3Service.upload(uuid + ".pdf", byteData);
                s3Document = new S3Document();
                s3Document.setS3FileKey(key);
                s3Document = s3DocumentService.save(s3Document);
                threatAssessment.setThreatAssessmentReportS3Document(s3Document);
                threatAssessment.setThreatAssessmentReportApprovalStatus(ThreatAssessmentReportApprovalStatus.WAITING);
                threatAssessment.setThreatAssessmentReportApprover(responsibleUser);
                threatAssessment = threatAssessmentService.save(threatAssessment);
            }

            File pdfFile = File.createTempFile(uuid, ".pdf");
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                fos.write(byteData);
            }
            emailEvent.getAttachments().add(new EmailEvent.EmailAttachement(pdfFile.getAbsolutePath(),
                "Ledelsesrapport for risikovurdering af " + threatAssessment.getName() + ".pdf"));
        }

        final String loggedInUserName = user != null ? user.getName() : "";
        final String recipient = responsibleUser.getEmail();
        final String messageFromSender = dto.message.replace("\n", "<br/>");
        final String objectName = threatAssessment.getName();
        EmailTemplate template = dto.sign
            ? emailTemplateService.findByTemplateType(EmailTemplateType.RISK_REPORT_TO_SIGN)
            : emailTemplateService.findByTemplateType(EmailTemplateType.RISK_REPORT);

        if (template.isEnabled()) {
            String link = dto.sign
                ? "<a href=\"" + environment.getProperty("di.saml.sp.baseUrl") + "/sign/preview/" + s3Document.getId() + "\">"
                + environment.getProperty("di.saml.sp.baseUrl") + "/sign/view/" + s3Document.getId() + "</a>"
                : "";

            String title = formatTemplateString(template.getTitle(), recipient, objectName, messageFromSender, loggedInUserName, link);
            String message = formatTemplateString(template.getMessage(), recipient, objectName, messageFromSender, loggedInUserName, link);

            emailEvent.setMessage(message);
            emailEvent.setSubject(title);
        } else {
            log.info("Email template with type " + template.getTemplateType() + " is disabled. Email was not sent.");
        }

        eventPublisher.publishEvent(emailEvent);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private String formatTemplateString(String templateString, String recipient, String objectName, String messageFromSender, String sender, String link) {
        return templateString.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient)
            .replace(EmailTemplatePlaceholder.OBJECT_PLACEHOLDER.getPlaceholder(), objectName)
            .replace(EmailTemplatePlaceholder.MESSAGE_FROM_SENDER.getPlaceholder(), messageFromSender)
            .replace(EmailTemplatePlaceholder.SENDER.getPlaceholder(), sender)
            .replace(EmailTemplatePlaceholder.LINK_PLACEHOLDER.getPlaceholder(), link);
    }

    record SetFieldDTO(@NotNull SetFieldType setFieldType, @NotNull ThreatDatabaseType dbType, Long id, String identifier, @NotNull String value) {}
    @PostMapping("{id}/threats/setfield")
    public ResponseEntity<HttpStatus> setField(@PathVariable final long id, @Valid @RequestBody final SetFieldDTO dto) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !threatAssessment.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !threatAssessment.getResponsibleUser().getUuid().equals(SecurityUtil.getPrincipalUuid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

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

    @RequireSuperuserOrAdministrator
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

    public record CreateExternalRiskassessmentDTO(Long riskId, Set<Long> assetIds, String link, String name, ThreatAssessmentType type, Long registerId, String responsibleUserUuid, String responsibleOuUuid) {
    }
    @PostMapping("external/create")
    public ResponseEntity<HttpStatus> createExternalDpia(@RequestBody final CreateExternalRiskassessmentDTO createExternalDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		User responsibleUser = null;
		OrganisationUnit responsibleOu = null;
		if (createExternalDTO.responsibleUserUuid != null) {
			responsibleUser = userService.findByUuid(createExternalDTO.responsibleUserUuid).orElse(null);
		}
		if (createExternalDTO.responsibleOuUuid != null) {
			responsibleOu = organisationService.get(createExternalDTO.responsibleOuUuid).orElse(null);
		}

        ThreatAssessment threatAssessment = null;
        if (createExternalDTO.riskId != null) {
            //editing existing
            threatAssessment = threatAssessmentService.findById(createExternalDTO.riskId)
                .orElseThrow();
            threatAssessment.setName(createExternalDTO.name);
            threatAssessment.setExternalLink(createExternalDTO.link);
			threatAssessment.setResponsibleUser(responsibleUser);
			threatAssessment.setResponsibleOu(responsibleOu);
			if (createExternalDTO.type.equals(ThreatAssessmentType.ASSET)) {
				relateAssets(createExternalDTO.assetIds, threatAssessment);
			} else if (createExternalDTO.type.equals(ThreatAssessmentType.REGISTER)) {
				relateRegister(createExternalDTO.registerId, threatAssessment);
			}
		} else {
            //creating new
            threatAssessment = new ThreatAssessment();
            threatAssessment.setExternalLink(createExternalDTO.link);
            threatAssessment.setFromExternalSource(true);
            threatAssessment.setName(createExternalDTO.name);
            threatAssessment.setThreatAssessmentType(createExternalDTO.type);
			threatAssessment.setResponsibleUser(responsibleUser);
			threatAssessment.setResponsibleOu(responsibleOu);
            threatAssessment = threatAssessmentService.save(threatAssessment);
            if (createExternalDTO.type.equals(ThreatAssessmentType.ASSET)) {
                relateAssets(createExternalDTO.assetIds, threatAssessment);
            } else if (createExternalDTO.type.equals(ThreatAssessmentType.REGISTER)) {
                relateRegister(createExternalDTO.registerId, threatAssessment);
            }
        }
        threatAssessmentService.save(threatAssessment);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void relateAssets(final Set<Long> selectedAsset, final ThreatAssessment savedThreatAssessment) {
        final List<Asset> relatedAssets = assetService.findAllById(selectedAsset);
        relatedAssets.forEach(asset -> relationService.addRelation(savedThreatAssessment, asset));
        if (savedThreatAssessment.isInherit()) {
            inheritRisk(savedThreatAssessment, relatedAssets);
        }
    }

    private void relateRegister(final Long selectedRegister, final ThreatAssessment savedThreatAssessment) {
        final Register register = registerService.findById(selectedRegister).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en behandlingsaktivitet, når typen behandlingsaktivitet er valgt."));
        relationService.addRelation(savedThreatAssessment, register);
    }

    private void inheritRisk(final ThreatAssessment savedThreatAssesment, final List<Asset> assets) {
        final dk.digitalidentity.service.model.RiskDTO riskDTO = threatAssessmentService.calculateRiskFromRegisters(assets.stream().map(Relatable::getId).collect(Collectors.toList()));
        savedThreatAssesment.setInheritedConfidentialityRegistered(riskDTO.getRf());
        savedThreatAssesment.setInheritedIntegrityRegistered(riskDTO.getRi());
        savedThreatAssesment.setInheritedAvailabilityRegistered(riskDTO.getRt());
        savedThreatAssesment.setInheritedConfidentialityOrganisation(riskDTO.getOf());
        savedThreatAssesment.setInheritedIntegrityOrganisation(riskDTO.getOi());
        savedThreatAssesment.setInheritedAvailabilityOrganisation(riskDTO.getOt());

        for (final ThreatCatalogThreat threat : savedThreatAssesment.getThreatCatalog().getThreats()) {
            final ThreatAssessmentResponse response = new ThreatAssessmentResponse();
            response.setName(threat.getDescription());
            response.setConfidentialityRegistered(riskDTO.getRf());
            response.setIntegrityRegistered(riskDTO.getRi());
            response.setAvailabilityRegistered(riskDTO.getRt());
            response.setConfidentialityOrganisation(riskDTO.getOf());
            response.setIntegrityOrganisation(riskDTO.getOi());
            response.setAvailabilityOrganisation(riskDTO.getOt());
            response.setMethod(ThreatMethod.NONE);
            response.setThreatCatalogThreat(threat);
            response.setThreatAssessment(savedThreatAssesment);
            savedThreatAssesment.getThreatAssessmentResponses().add(response);
        }
        threatAssessmentService.save(savedThreatAssesment);
    }
}
