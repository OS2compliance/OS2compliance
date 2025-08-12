package dk.digitalidentity.controller.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.dao.grid.DPIAGridDao;
import dk.digitalidentity.event.EmailEvent;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.RiskDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIAReport;
import dk.digitalidentity.model.entity.DPIAResponseSection;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessmentScreening;
import dk.digitalidentity.model.entity.DataProtectionImpactScreeningAnswer;
import dk.digitalidentity.model.entity.EmailTemplate;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.S3Document;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DPIAReportReportApprovalStatus;
import dk.digitalidentity.model.entity.enums.DPIAScreeningConclusion;
import dk.digitalidentity.model.entity.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.model.entity.enums.EmailTemplateType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.model.entity.grid.DPIAGrid;
import dk.digitalidentity.model.entity.grid.RiskGrid;
import dk.digitalidentity.security.RequireSuperuserOrAdministrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.DPIAResponseSectionAnswerService;
import dk.digitalidentity.service.DPIAResponseSectionService;
import dk.digitalidentity.service.DPIAService;
import dk.digitalidentity.service.DPIATemplateQuestionService;
import dk.digitalidentity.service.DPIATemplateSectionService;
import dk.digitalidentity.service.EmailTemplateService;
import dk.digitalidentity.service.ExcelExportService;
import dk.digitalidentity.service.OrganisationService;
import dk.digitalidentity.service.S3DocumentService;
import dk.digitalidentity.service.S3Service;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.htmlcleaner.BrowserCompactXmlSerializer;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dk.digitalidentity.service.FilterService.buildPageable;
import static dk.digitalidentity.service.FilterService.validateSearchFilters;

@Slf4j
@RestController
@RequestMapping("rest/dpia")
@RequireUser
@RequiredArgsConstructor
public class DPIARestController {
	private final DPIAGridDao dpiaGridDao;
	private final DPIAService dpiaService;
	private final ChoiceDPIADao choiceDPIADao;
    private final AssetService assetService;
	private final DPIATemplateQuestionService dpiaTemplateQuestionService;
	private final DPIATemplateSectionService dpiaTemplateSectionService;
	private final DPIAResponseSectionService dpiaResponseSectionService;
	private final DPIAResponseSectionAnswerService dpiaResponseSectionAnswerService;
	private final UserService userService;
	private final S3Service s3Service;
	private final S3DocumentService s3DocumentService;
	private final EmailTemplateService emailTemplateService;
	private final Environment environment;
	private final ApplicationEventPublisher eventPublisher;
	private final OrganisationService organisationService;
	private final ExcelExportService excelExportService;

	public record DPIAListDTO(long id, String name, String responsibleUserName, String responsibleOUName, LocalDate userUpdatedDate, int taskCount, ThreatAssessmentReportApprovalStatus status, DPIAScreeningConclusion screeningConclusion, Boolean isExternal) {
	}

	@PostMapping("list")
	public Object list(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "50") int limit,
			@RequestParam(value = "order", required = false) String sortColumn,
			@RequestParam(value = "dir", defaultValue = "ASC") String sortDirection,
			@RequestParam(value = "export", defaultValue = "false") boolean export,
			@RequestParam(value = "fileName", defaultValue = "export.xlsx") String fileName,
			@RequestParam Map<String, String> filters,
			HttpServletResponse response
	) throws IOException {

		// For export mode, get ALL records (no pagination)
		if (export) {
			Page<DPIAGrid> allDPIAs = dpiaGridDao.findAllWithColumnSearch(
					validateSearchFilters(filters, DPIAGrid.class),
					buildPageable(page, Integer.MAX_VALUE, sortColumn, sortDirection),
					DPIAGrid.class
			);

			List<DPIAListDTO> allData = allDPIAs.getContent().stream().map(dpiaGrid ->
				new DPIAListDTO(
						dpiaGrid.getId(),
						dpiaGrid.getName(),
						dpiaGrid.getResponsibleUserName()	,
						dpiaGrid.getResponsibleOuName(),
						dpiaGrid.getUserUpdatedDate(),
						dpiaGrid.getTaskCount(),
						dpiaGrid.getReportApprovalStatus(),
						dpiaGrid.getScreeningConclusion(),
						dpiaGrid.isExternal()
				))
				.toList();
			excelExportService.exportToExcel(allData, fileName, response);
			return null;
		}

		// Normal mode - return paginated JSON
		Page<DPIAGrid> dpiaGrids =  dpiaGridDao.findAllWithColumnSearch(
				validateSearchFilters(filters, DPIAGrid.class),
				buildPageable(page, limit, sortColumn, sortDirection),
				DPIAGrid.class
		);

		assert dpiaGrids != null;
		return new PageDTO<>(dpiaGrids.getTotalElements(), dpiaGrids.stream().map(dpia ->
						new DPIAListDTO(
								dpia.getId(),
								dpia.getName(),
								dpia.getResponsibleUserName()	,
								dpia.getResponsibleOuName(),
								dpia.getUserUpdatedDate(),
								dpia.getTaskCount(),
								dpia.getReportApprovalStatus(),
								dpia.getScreeningConclusion(),
								dpia.isExternal()
						))
				.toList());
	}

	@DeleteMapping("delete/{id}")
	@RequireSuperuserOrAdministrator
	@ResponseStatus(value = HttpStatus.OK)
	@Transactional
	public void delete(@PathVariable final Long id) {
		dpiaService.delete(id);
	}

	public record DPIAScreeningUpdateDTO(Long dpiaId, String answer, String choiceIdentifier) {
	}

	@Transactional
	@PostMapping("screening/update")
	public ResponseEntity<HttpStatus> dpia(@RequestBody final DPIAScreeningUpdateDTO dpiaScreeningUpdateDTO) {
		final DPIA dpia = dpiaService.find(dpiaScreeningUpdateDTO.dpiaId);
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !isResponsibleForAsset(dpia.getAssets())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		final DataProtectionImpactAssessmentScreening dpiaScreening = dpia.getDpiaScreening();

		final DataProtectionImpactScreeningAnswer foundAnswer = dpiaScreening.getDpiaScreeningAnswers().stream()
				.filter(a -> a.getChoice().getIdentifier().equalsIgnoreCase(dpiaScreeningUpdateDTO.choiceIdentifier))
				.findFirst()
				.orElseGet(() -> {
					final DataProtectionImpactScreeningAnswer newAnswer = DataProtectionImpactScreeningAnswer.builder()
							.assessment(dpiaScreening)
							.choice(choiceDPIADao.findByIdentifier(dpiaScreeningUpdateDTO.choiceIdentifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)))
							.build();
					dpiaScreening.getDpiaScreeningAnswers().add(newAnswer);
					return newAnswer;
				});
		foundAnswer.setAnswer(dpiaScreeningUpdateDTO.answer);

		//calculate and set new screening conclusion
		dpiaScreening.setConclusion(dpiaService.calculateScreeningConclusion(dpiaScreening.getDpiaScreeningAnswers()));

		return new ResponseEntity<>(HttpStatus.OK);
	}

    public record CommentUpdateDTO(Long dpiaId, String comment){}
    @PostMapping("comment/update")
    public ResponseEntity<HttpStatus> updateDPIAComment(@RequestBody final CommentUpdateDTO commentUpdateDTO) {
        final DPIA dpia = dpiaService.find(commentUpdateDTO.dpiaId);
        final List<Asset> assets = dpia.getAssets();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !isResponsibleForAsset(assets)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        dpia.setComment(commentUpdateDTO.comment);
		dpiaService.save(dpia);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public record QualityAssuranceUpdateDTO (Long dpiaId, Set<String> dpiaQualityCheckValues) {}
    @Transactional
	@PostMapping("qualityassurance/update")
	public ResponseEntity<HttpStatus> dpia(@RequestBody final QualityAssuranceUpdateDTO qualityAssuranceUpdateDTO) {
		final DPIA dpia = dpiaService.find(qualityAssuranceUpdateDTO.dpiaId);
		final List<Asset> assets = dpia.getAssets();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !isResponsibleForAsset(assets)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

        dpia.setChecks(qualityAssuranceUpdateDTO.dpiaQualityCheckValues);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record CreateDPIAResponse(Long dpiaId) {}
    public record CreateDPIAFormDTO (String title, List<Long> assetIds, @JsonFormat(pattern="dd/MM-yyyy") LocalDate userUpdatedDate, String responsibleUserUuid, String responsibleOuUuid){}
    @PostMapping("create")
    public ResponseEntity<CreateDPIAResponse> createDpia (@RequestBody final  CreateDPIAFormDTO createDPIAFormDTO) throws IOException {
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final List<Asset> assets = assetService.findAllById(createDPIAFormDTO.assetIds);
		if (assets.isEmpty()) {throw new IllegalArgumentException("Must choose at least one asset");}
        if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !isResponsibleForAsset(assets)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        DPIA dpia = dpiaService.create(assets, createDPIAFormDTO.title, createDPIAFormDTO.userUpdatedDate, createDPIAFormDTO.responsibleUserUuid, createDPIAFormDTO.responsibleOuUuid);

        return new ResponseEntity<>(new CreateDPIAResponse(dpia.getId()), HttpStatus.OK);
    }

	public record EditDPIADTO(String title, List<Long> assetIds, @JsonFormat(pattern="dd/MM-yyyy") LocalDate userUpdatedDate, String responsibleUserUuid, String responsibleOuUuid){}
	@Transactional
	@RequireSuperuserOrAdministrator
	@PostMapping("{dpiaId}/edit")
	public ResponseEntity<HttpStatus> createExternalDpia(@PathVariable Long dpiaId,  @RequestBody final EditDPIADTO editDPIADTO) {
		List<Asset> assets	= assetService.findAllById(editDPIADTO.assetIds);
		if (assets.isEmpty()) {throw new IllegalArgumentException("Must choose at least one asset");}

		DPIA dpia = dpiaService.find(dpiaId);

		dpia.setUserUpdatedDate(editDPIADTO.userUpdatedDate);
		if(!editDPIADTO.title.isBlank()) {
			dpia.setName(editDPIADTO.title);
		}
		dpia.setAssets(assets);

		if (editDPIADTO.responsibleUserUuid != null) {
			User user = userService.findByUuid(editDPIADTO.responsibleUserUuid).orElse(null);
			dpia.setResponsibleUser(user);
		}
		if (editDPIADTO.responsibleOuUuid != null) {
			OrganisationUnit ou = organisationService.get(editDPIADTO.responsibleOuUuid).orElse(null);
			dpia.setResponsibleOu(ou);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

    public record CreateExternalDPIADTO(Long dpiaId, String title, List<Long> assetIds, String link, @JsonFormat(pattern="dd/MM-yyyy") LocalDate userUpdatedDate, String responsibleUserUuid, String responsibleOuUuid) {
    }
    @PostMapping("external/create")
    public ResponseEntity<HttpStatus> createExternalDpia(@RequestBody final CreateExternalDPIADTO createExternalDPIADTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Asset> assets;
        DPIA dpia = null;
        if (!createExternalDPIADTO.assetIds.isEmpty()) {
            assets = assetService.findAllById(createExternalDPIADTO.assetIds);
			if (assets.isEmpty()) {throw new IllegalArgumentException("Must choose at least one asset");}
        } else {
            dpia = dpiaService.find(createExternalDPIADTO.dpiaId);
            assets = dpia.getAssets();
        }

		if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER))
				&& !assets.stream()
				.flatMap(a -> a.getResponsibleUsers().stream()
						.map(User::getUuid)).toList()
				.contains(SecurityUtil.getPrincipalUuid())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

        if (createExternalDPIADTO.dpiaId != null) {
			//Update
            if (dpia == null) {
                dpia = dpiaService.find(createExternalDPIADTO.dpiaId);
            }
            dpia.setExternalLink(createExternalDPIADTO.link);
			dpia.setUserUpdatedDate(createExternalDPIADTO.userUpdatedDate);
			if (createExternalDPIADTO.responsibleUserUuid != null) {
				User user = userService.findByUuid(createExternalDPIADTO.responsibleUserUuid).orElse(null);
				dpia.setResponsibleUser(user);
			}
			if (createExternalDPIADTO.responsibleOuUuid != null) {
				OrganisationUnit ou = organisationService.get(createExternalDPIADTO.responsibleOuUuid).orElse(null);
				dpia.setResponsibleOu(ou);
			}
			if (createExternalDPIADTO.title != null && !createExternalDPIADTO.title.isBlank()) {
				dpia.setName(createExternalDPIADTO.title);
			}
            dpiaService.save(dpia);
        } else {
			//Create
            dpiaService.createExternal(assets,createExternalDPIADTO.link, createExternalDPIADTO.title, createExternalDPIADTO.userUpdatedDate, createExternalDPIADTO.responsibleUserUuid, createExternalDPIADTO.responsibleOuUuid);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

	record DPIASetFieldDTO(long id, String fieldName, String value) {}
	@PutMapping("{dpiaId}/response/setfield")
	public void setDPIAResponseField(@RequestBody final DPIASetFieldDTO dto, @PathVariable final long dpiaId) throws IOException {
		final DPIA dpia = dpiaService.find(dpiaId);
		final List<Asset> assets = dpia.getAssets();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !isResponsibleForAsset(assets)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		switch (dto.fieldName) {
			case "selected":
				DPIAResponseSection matchSelected = dpia.getDpiaResponseSections().stream().filter(s -> s.getDpiaTemplateSection().getId() == dto.id).findAny().orElse(null);
				if (matchSelected == null) {
					matchSelected = new DPIAResponseSection();
					DPIATemplateSection dpiaTemplateSection = dpiaTemplateSectionService.findById(dto.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
					matchSelected.setDpia(dpia);
					matchSelected.setDpiaTemplateSection(dpiaTemplateSection);
					matchSelected.setSelected(true);
					matchSelected = dpiaResponseSectionService.save(matchSelected);
					dpia.getDpiaResponseSections().add(matchSelected);
				}

				matchSelected.setSelected(Boolean.parseBoolean(dto.value));
				dpiaResponseSectionService.save(matchSelected);
				break;
			case "response":
				DPIATemplateQuestion dpiaTemplateQuestion = dpiaTemplateQuestionService.findById(dto.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
				DPIAResponseSection matchResponseSection = dpia.getDpiaResponseSections().stream().filter(s -> s.getDpiaTemplateSection().getId() == dpiaTemplateQuestion.getDpiaTemplateSection().getId()).findAny().orElse(null);
				if (matchResponseSection == null) {
					matchResponseSection = new DPIAResponseSection();
					matchResponseSection.setDpia(dpia);
					matchResponseSection.setDpiaTemplateSection(dpiaTemplateQuestion.getDpiaTemplateSection());
					matchResponseSection.setSelected(true);
					matchResponseSection = dpiaResponseSectionService.save(matchResponseSection);
					dpia.getDpiaResponseSections().add(matchResponseSection);
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

		assets.forEach(assetService::save);
	}

	@PutMapping("{dpiaId}/setfield")
	public void setDPIASectionField(@RequestBody final DPIASetFieldDTO dto, @PathVariable long dpiaId) {
		final DPIA dpia = dpiaService.find(dpiaId);
		final List<Asset> assets = dpia.getAssets();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !isResponsibleForAsset(assets)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		if (dto.fieldName.equals("conclusion")) {
			dpia.setConclusion(dto.value);
		} else if (dto.fieldName.equals("checkedThreatAssessmentIds")) {
			dpia.setCheckedThreatAssessmentIds(dto.value);
		}
		dpiaService.save(dpia);
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

	public record MailReportDTO(String message, String sendTo, boolean sign) {
	}
	@Transactional
	@PostMapping("{dpiaId}/mailReport")
	public ResponseEntity<?> mailReport(@PathVariable final long dpiaId, @RequestBody final MailReportDTO dto) throws IOException {
		final DPIA dpia = dpiaService.find(dpiaId);
		List<Asset> assets = dpia.getAssets();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication.getAuthorities().stream().noneMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)) && !isResponsibleForAsset(assets)) {
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

		byte[] byteData = assetService.getDPIAPdf(dpia);
		String uuid = UUID.randomUUID().toString();

		List<Asset> savedAssets = new ArrayList<>();
		if (dto.sign) {
			DPIAReport dpiaReport = new DPIAReport();
			dpiaReport.setDpia(dpia);
			dpiaReport.setDpiaReportApprovalStatus(DPIAReportReportApprovalStatus.WAITING);
			dpiaReport.setReportApproverUuid(responsibleUser.getUuid());
			dpiaReport.setReportApproverName(responsibleUser.getName() + "(" + responsibleUser.getUserId() + ")");
			String key = s3Service.upload(uuid + ".pdf", byteData);
			s3Document = new S3Document();
			s3Document.setS3FileKey(key);
			s3Document = s3DocumentService.save(s3Document);
			dpiaReport.setDpiaReportS3Document(s3Document);
			dpia.getDpiaReports().add(dpiaReport);
			assets.forEach(asset ->
				savedAssets.add(assetService.save(asset))
			);
		}

		File pdfFile = File.createTempFile(uuid, ".pdf");
		try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
			fos.write(byteData);
		}
		emailEvent.getAttachments().add(new EmailEvent.EmailAttachement(pdfFile.getAbsolutePath(),
				"Konsekvensanalyse vedr " + dpia.getName() + ".pdf"));

		final String loggedInUserName = user != null ? user.getName() : "";
		final String recipient = responsibleUser.getEmail();
		final String messageFromSender = dto.message.replace("\n", "<br/>");
		final String objectName = dpia.getName();
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
			emailEvent.setTemplateType(template.getTemplateType());
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

	private boolean isResponsibleForAsset(List<Asset> assets) {
		return assets.stream().flatMap(a ->
				a.getResponsibleUsers().stream()
						.map(User::getUuid))
				.toList()
				.contains(SecurityUtil.getPrincipalUuid());
	}
}
