package dk.digitalidentity.controller.rest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dk.digitalidentity.dao.AssetOversightDao;
import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIAReport;
import dk.digitalidentity.model.entity.DPIAResponseSection;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessmentScreening;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.S3Document;
import dk.digitalidentity.model.entity.enums.ChoiceOfSupervisionModel;
import dk.digitalidentity.model.entity.enums.DPIAReportReportApprovalStatus;
import dk.digitalidentity.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.model.entity.enums.NextInspection;
import dk.digitalidentity.security.RequireSuperuser;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.AssetOversightService;
import dk.digitalidentity.service.DPIAResponseSectionAnswerService;
import dk.digitalidentity.service.DPIAResponseSectionService;
import dk.digitalidentity.service.DPIATemplateQuestionService;
import dk.digitalidentity.service.DPIATemplateSectionService;
import dk.digitalidentity.service.EmailTemplateService;
import dk.digitalidentity.service.S3DocumentService;
import dk.digitalidentity.service.S3Service;
import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.BrowserCompactXmlSerializer;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dk.digitalidentity.dao.grid.AssetGridDao;
import dk.digitalidentity.mapping.AssetMapper;
import dk.digitalidentity.model.dto.AssetDTO;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.grid.AssetGrid;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.UserService;
import dk.digitalidentity.util.ReflectionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("rest/assets")
@RequireUser
@RequiredArgsConstructor
public class AssetsRestController {
    private final AssetService assetService;
	private final AssetGridDao assetGridDao;
	private final AssetMapper mapper;
    private final UserService userService;
    private final DPIATemplateQuestionService dpiaTemplateQuestionService;
    private final DPIATemplateSectionService dpiaTemplateSectionService;
    private final DPIAResponseSectionService dpiaResponseSectionService;
    private final DPIAResponseSectionAnswerService dpiaResponseSectionAnswerService;
    private final S3Service s3Service;
    private final S3DocumentService s3DocumentService;
    private final EmailTemplateService emailTemplateService;
    private final Environment environment;
    private final ApplicationEventPublisher eventPublisher;
    private final AssetOversightService assetOversightService;

    @PostMapping("list")
	public PageDTO<AssetDTO> list(@RequestParam(name = "search", required = false) final String search,
                                  @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
                                  @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
                                  @RequestParam(name = "order", required = false) final String order,
                                  @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
		if (StringUtils.isNotEmpty(order) && containsField(order)) {
			final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
			sort = Sort.by(direction, order);
		} else {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }
		final Pageable sortAndPage = PageRequest.of(page, size, sort);
		Page<AssetGrid> assets = null;
		if (StringUtils.isNotEmpty(search)) {
			final List<String> searchableProperties = Arrays.asList("name", "supplier", "responsibleUserNames", "updatedAt", "localizedEnums");
			// search and page
			assets = assetGridDao.findAllCustom(searchableProperties, search, sortAndPage, AssetGrid.class);
		} else {
			// Fetch paged and sorted
			assets = assetGridDao.findAll(sortAndPage);
		}

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent(), authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), authentication.getPrincipal().toString()));
	}

    @PostMapping("list/{id}")
    public PageDTO<AssetDTO> list(@PathVariable(name = "id", required = true) final String uuid,
                                @RequestParam(name = "search", required = false) final String search,
                                @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
                                @RequestParam(name = "size", required = false, defaultValue = "50") final Integer size,
                                @RequestParam(name = "order", required = false) final String order,
                                @RequestParam(name = "dir", required = false) final String dir) {
        Sort sort = null;
        final User user = userService.findByUuid(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !authentication.getPrincipal().equals(uuid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (StringUtils.isNotEmpty(order) && containsField(order)) {
            final Sort.Direction direction = Sort.Direction.fromOptionalString(dir).orElse(Sort.Direction.ASC);
            sort = Sort.by(direction, order);
        } else {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }
        final Pageable sortAndPage = PageRequest.of(page, size, sort);
        Page<AssetGrid> assets = null;
        if (StringUtils.isNotEmpty(search)) {
            final List<String> searchableProperties = Arrays.asList("name", "supplier", "responsibleUserNames", "updatedAt", "localizedEnums");
            // search and page
            assets = assetGridDao.findAllForResponsibleUser(searchableProperties, search, sortAndPage, AssetGrid.class, user);
        } else {
            // Fetch paged and sorted
            assets = assetGridDao.findAllByResponsibleUserUuidsContaining(user.getUuid(), sortAndPage);
        }

        return new PageDTO<>(assets.getTotalElements(), mapper.toDTO(assets.getContent(), authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)), authentication.getPrincipal().toString()));
    }

    @PutMapping("{id}/setfield")
    public void setAssetField(@PathVariable("id") final Long id, @RequestParam("name") final String fieldName,
                                        @RequestParam(value = "value", required = false) final String value) {
        canSetFieldGuard(fieldName);
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        ReflectionHelper.callSetterWithParam(Asset.class, asset, fieldName, value);
        assetService.save(asset);
    }

    @PutMapping("{id}/dpiascreening/setfield")
    public void setDpiaScreeningField(@PathVariable("id") final Long id, @RequestParam("name") final String fieldName,
        @RequestParam(value = "value", required = false) final String value) {
        canSetFieldDPIAScreeningGuard(fieldName);
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        ReflectionHelper.callSetterWithParam(DataProtectionImpactAssessmentScreening.class, asset.getDpiaScreening(), fieldName, value);
        assetService.save(asset);
    }


    @PutMapping("{id}/oversightresponsible")
    public void setOversightResponsible(@PathVariable("id") final Long id, @RequestParam("userUuid") final String userUuid) {
        final Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final User user = userService.findByUuid(userUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        asset.setOversightResponsibleUser(user);
        if (asset.getSupervisoryModel() != ChoiceOfSupervisionModel.DBS) {
            assetOversightService.setAssetsToDbsOversight(Collections.singletonList(asset));
        } else {
            assetOversightService.createOrUpdateAssociatedOversightCheck(asset);
        }
    }

    record DPIASetFieldDTO(long id, String fieldName, String value) {}
    @RequireSuperuser
    @PutMapping("dpia/schema/section/setfield")
    public void setDPIASectionField( @RequestBody final DPIASetFieldDTO dto) {
        canSetDPIASectionFieldGuard(dto.fieldName);
        final DPIATemplateSection dpiaTemplateSection = dpiaTemplateSectionService.findById(dto.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ReflectionHelper.callSetterWithParam(DPIATemplateSection.class, dpiaTemplateSection, dto.fieldName, dto.value);
        dpiaTemplateSectionService.save(dpiaTemplateSection);
    }

    @RequireSuperuser
    @PostMapping("dpia/schema/section/{id}/up")
    public ResponseEntity<?> reorderUp(@PathVariable("id") final long id) {
        reorderSections(id, false);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuser
    @PostMapping("dpia/schema/section/{id}/down")
    public ResponseEntity<?> reorderDown(@PathVariable("id") final long id) {
        reorderSections(id, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuser
    @PostMapping("dpia/schema/question/{id}/up")
    public ResponseEntity<?> reorderQuestionUp(@PathVariable("id") final long id) {
        reorderQuestions(id, false);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuser
    @PostMapping("dpia/schema/question/{id}/down")
    public ResponseEntity<?> reorderQuestionDown(@PathVariable("id") final long id) {
        reorderQuestions(id, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuser
    @DeleteMapping("dpia/schema/question/{id}/delete")
    public ResponseEntity<?> deleteQuestion(@PathVariable("id") final long id) {
        final DPIATemplateQuestion question = dpiaTemplateQuestionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        question.setDeleted(true);
        dpiaTemplateQuestionService.save(question);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Transactional
    @DeleteMapping("oversight/{oversightId}")
    public ResponseEntity<?> deleteOversight(@PathVariable("oversightId") Long oversightId) {
        final AssetOversight assetOversight = assetService.getOversight(oversightId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER) && assetOversight.getResponsibleUser().getUuid().equals(authentication.getPrincipal().toString()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        assetOversightService.delete(assetOversight);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequireSuperuser
    @Transactional
    @DeleteMapping("{assetId}/subsupplier/{subSupplierId}")
    public ResponseEntity<?> subSupplierDelete(@PathVariable("subSupplierId") final Long subSupplierId,
                                               @PathVariable("assetId") final Long assetId) {
        final Asset asset = assetService.get(assetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final AssetSupplierMapping subSupplier = asset.getSuppliers().stream().filter(s -> Objects.equals(s.getId(), subSupplierId)).findAny()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        asset.getSuppliers().remove(subSupplier);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("{assetId}/dpia/response/setfield")
    public void setDPIAResponseField( @RequestBody final DPIASetFieldDTO dto, @PathVariable final long assetId) throws IOException {
        final Asset asset = assetService.findById(assetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (asset.getDpia() == null) {
            DPIA dpia = new DPIA();
            dpia.setAsset(asset);
            asset.setDpia(dpia);
        }

        switch (dto.fieldName) {
            case "selected":
                DPIAResponseSection matchSelected = asset.getDpia().getDpiaResponseSections().stream().filter(s -> s.getDpiaTemplateSection().getId() == dto.id).findAny().orElse(null);
                if (matchSelected == null) {
                    matchSelected = new DPIAResponseSection();
                    DPIATemplateSection dpiaTemplateSection = dpiaTemplateSectionService.findById(dto.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    matchSelected.setDpia(asset.getDpia());
                    matchSelected.setDpiaTemplateSection(dpiaTemplateSection);
                    matchSelected.setSelected(true);
                    matchSelected = dpiaResponseSectionService.save(matchSelected);
                    asset.getDpia().getDpiaResponseSections().add(matchSelected);
                }

                matchSelected.setSelected(Boolean.parseBoolean(dto.value));
                dpiaResponseSectionService.save(matchSelected);
                break;
            case "response":
                DPIATemplateQuestion dpiaTemplateQuestion = dpiaTemplateQuestionService.findById(dto.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                DPIAResponseSection matchResponseSection = asset.getDpia().getDpiaResponseSections().stream().filter(s -> s.getDpiaTemplateSection().getId() == dpiaTemplateQuestion.getDpiaTemplateSection().getId()).findAny().orElse(null);
                if (matchResponseSection == null) {
                    matchResponseSection = new DPIAResponseSection();
                    matchResponseSection.setDpia(asset.getDpia());
                    matchResponseSection.setDpiaTemplateSection(dpiaTemplateQuestion.getDpiaTemplateSection());
                    matchResponseSection.setSelected(true);
                    matchResponseSection = dpiaResponseSectionService.save(matchResponseSection);
                    asset.getDpia().getDpiaResponseSections().add(matchResponseSection);
                }

                DPIAResponseSectionAnswer matchResponse = matchResponseSection.getDpiaResponseSectionAnswers().stream().filter(a -> a.getDpiaTemplateQuestion().getId() == dto.id).findAny().orElse(null);
                if (matchResponse == null) {
                    matchResponse = new DPIAResponseSectionAnswer();
                    matchResponse.setDpiaResponseSection(matchResponseSection);
                    matchResponse.setDpiaTemplateQuestion(dpiaTemplateQuestion);
                    matchResponse = dpiaResponseSectionAnswerService.save(matchResponse);
                    matchResponseSection.getDpiaResponseSectionAnswers().add(matchResponse);
                }

                String xhtml = toXHTML(dto.value);
                matchResponse.setResponse(xhtml);
                dpiaResponseSectionAnswerService.save(matchResponse);
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        assetService.save(asset);
    }

    @PutMapping("{assetId}/dpia/setfield")
    public void setDPIASectionField( @RequestBody final DPIASetFieldDTO dto, @PathVariable long assetId) {
        Asset asset = assetService.findById(assetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (dto.fieldName.equals("conclusion")) {
            asset.getDpia().setConclusion(dto.value);
        } else if (dto.fieldName.equals("checkedThreatAssessmentIds")) {
            asset.getDpia().setCheckedThreatAssessmentIds(dto.value);
        }
        assetService.save(asset);
    }

    record MailReportDTO(String message, String sendTo, boolean sign) {}
    @Transactional
    @PostMapping("{id}/mailReport")
    public ResponseEntity<?> mailReport(@PathVariable final long id, @RequestBody final MailReportDTO dto) throws IOException {
        Asset asset = assetService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !asset.getResponsibleUsers().stream().map(User::getUuid).toList().contains(authentication.getPrincipal().toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        final User responsibleUser = userService.findByUuid(dto.sendTo).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Den valgte bruger kunne ikke findes, og rapporten kan derfor ikke sendes."));
        if (responsibleUser.getEmail() == null || responsibleUser.getEmail().isBlank()) {
            return new ResponseEntity<>("Den valgte bruger har ikke nogen email tilknyttet, og rapporten kan derfor ikke sendes.", HttpStatus.BAD_REQUEST);
        }

        final User user = userService.currentUser();
        S3Document s3Document = null;

        final EmailEvent emailEvent = EmailEvent.builder()
            .email(responsibleUser.getEmail())
            .build();

        byte[] byteData = assetService.getDPIAPdf(asset);
        String uuid = UUID.randomUUID().toString();

        if (dto.sign) {
            DPIAReport dpiaReport = new DPIAReport();
            dpiaReport.setDpia(asset.getDpia());
            dpiaReport.setDpiaReportApprovalStatus(DPIAReportReportApprovalStatus.WAITING);
            dpiaReport.setReportApproverUuid(responsibleUser.getUuid());
            dpiaReport.setReportApproverName(responsibleUser.getName() + "(" + responsibleUser.getUserId() + ")");
            String key = s3Service.upload(uuid + ".pdf", byteData);
            s3Document = new S3Document();
            s3Document.setS3FileKey(key);
            s3Document = s3DocumentService.save(s3Document);
            dpiaReport.setDpiaReportS3Document(s3Document);
            asset.getDpia().getDpiaReports().add(dpiaReport);
            asset = assetService.save(asset);
        }

        File pdfFile = File.createTempFile(uuid, ".pdf");
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            fos.write(byteData);
        }
        emailEvent.getAttachments().add(new EmailEvent.EmailAttachement(pdfFile.getAbsolutePath(),
            "Konsekvensanalyse vedr " + asset.getName() + ".pdf"));

        final String loggedInUserName = user != null ? user.getName() : "";
        final String recipient = responsibleUser.getEmail();
        final String messageFromSender = dto.message.replace("\n", "<br/>");
        final String objectName = asset.getName();
        EmailTemplate template = dto.sign
            ? emailTemplateService.findByTemplateType(EmailTemplateType.DPIA_REPORT_TO_SIGN)
            : emailTemplateService.findByTemplateType(EmailTemplateType.DPIA_REPORT);

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

    private void reorderQuestions(final long id, final boolean backwards) {
        final DPIATemplateQuestion question = dpiaTemplateQuestionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final List<DPIATemplateQuestion> allQuestionsInSection = dpiaTemplateQuestionService.findAll().stream()
            .filter(q -> !q.isDeleted() && q.getDpiaTemplateSection().getId() == question.getDpiaTemplateSection().getId())
            .sorted(sortDPIATemplateQuestionComparator(backwards))
            .toList();
        if (!allQuestionsInSection.isEmpty()) {
            DPIATemplateQuestion last = null;
            for (final DPIATemplateQuestion currentQuestion : allQuestionsInSection) {
                if (last != null && currentQuestion.getId() == id) {
                    final Long newKey = last.getSortKey();
                    last.setSortKey(currentQuestion.getSortKey());
                    currentQuestion.setSortKey(newKey);
                    dpiaTemplateQuestionService.save(last);
                    dpiaTemplateQuestionService.save(currentQuestion);
                    break;
                }
                last = currentQuestion;
            }
        }

    }

    private static Comparator<DPIATemplateQuestion> sortDPIATemplateQuestionComparator(final boolean backwards) {
        final Comparator<DPIATemplateQuestion> comparator = Comparator.comparing(DPIATemplateQuestion::getSortKey);
        return backwards ? comparator.reversed() : comparator;
    }

    private void reorderSections(final long id, final boolean backwards) {
        final List<DPIATemplateSection> allSections = dpiaTemplateSectionService.findAll().stream()
            .sorted(sortDPIATemplateSectionComparator(backwards))
            .toList();
        if (!allSections.isEmpty()) {
            DPIATemplateSection last = null;
            for (final DPIATemplateSection currentSection : allSections) {
                if (last != null && currentSection.getId() == id) {
                    final Long newKey = last.getSortKey();
                    last.setSortKey(currentSection.getSortKey());
                    currentSection.setSortKey(newKey);
                    dpiaTemplateSectionService.save(last);
                    dpiaTemplateSectionService.save(currentSection);
                    break;
                }
                last = currentSection;
            }
        }

    }

    private static Comparator<DPIATemplateSection> sortDPIATemplateSectionComparator(final boolean backwards) {
        final Comparator<DPIATemplateSection> comparator = Comparator.comparing(DPIATemplateSection::getSortKey);
        return backwards ? comparator.reversed() : comparator;
    }

    private void canSetDPIASectionFieldGuard(final String fieldName) {
        if (!(fieldName.equals("hasOptedOut"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void canSetFieldGuard(final String fieldName) {
        if (!(fieldName.equals("threatAssessmentOptOut") ||
            fieldName.equals("threatAssessmentOptOutReason") ||
            fieldName.equals("dpiaOptOutReason") ||
            fieldName.equals("dpiaOptOut"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void canSetFieldDPIAScreeningGuard(final String fieldName) {
        if (!(fieldName.equals("consequenceLink"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

	private boolean containsField(final String fieldName) {
		return fieldName.equals("assessmentOrder")
				|| fieldName.equals("supplier")
				|| fieldName.equals("risk")
				|| fieldName.equals("name")
				|| fieldName.equals("assetType")
				|| fieldName.equals("responsibleUserNames")
				|| fieldName.equals("registers")
				|| fieldName.equals("updatedAt")
				|| fieldName.equals("criticality")
				|| fieldName.equals("assetStatusOrder")
                || fieldName.equals("hasThirdCountryTransfer");
	}

    /**
     * editor does not generate valid XHTML. At least the <br/> and <img/> tags are not closed,
     * so we need to close them, otherwise our PDF processing will fail.
     */
    private String toXHTML(String html) throws IOException {
        CleanerProperties properties = new CleanerProperties();
        properties.setOmitXmlDeclaration(true);
        TagNode tagNode = new HtmlCleaner(properties).clean(html);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new BrowserCompactXmlSerializer(properties).writeToStream(tagNode, bos);

        return (bos.toString(StandardCharsets.UTF_8));
    }
}
