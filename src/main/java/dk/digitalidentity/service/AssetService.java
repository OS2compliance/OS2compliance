package dk.digitalidentity.service;

import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.AssetOversightDao;
import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.dao.DataProcessingDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.ChoiceDPIA;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIAResponseSection;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.DataProtectionImpactScreeningAnswer;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.TransferImpactAssessment;
import dk.digitalidentity.model.entity.enums.EstimationDTO;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.model.PlaceholderInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.ASSOCIATED_ASSET_DPIA_PROPERTY;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetDao assetDao;
    private final AssetOversightDao assetOversightDao;
    private final RelationService relationService;
    private final DataProcessingDao dataProcessingDao;
    private final TaskService taskService;
    private final UserService userService;
    private final TemplateEngine templateEngine;
    private final DPIATemplateSectionService dpiaTemplateSectionService;
    private final ChoiceService choiceService;
    private final ChoiceDPIADao choiceDPIADao;
	private final S3Service s3Service;

	public Optional<AssetOversight> getOversight(final Long oversightId) {
        return assetOversightDao.findById(oversightId);
    }

    public Optional<Asset> get(final Long id) {
        return assetDao.findByIdAndDeletedFalse(id);
    }

    public Page<Asset> getPaged(final int pageSize, final int page) {
        return assetDao.findAll(Pageable.ofSize(pageSize).withPage(page));
    }

    public List<Asset> getAllSortedByName() {
        return assetDao.findByDeletedFalse(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<Asset> findAllById(final Collection<Long> ids) {
        return assetDao.findAllByIdInAndDeletedFalse(ids);
    }

    public Optional<Asset> findById(final Long id) {
        return assetDao.findByIdAndDeletedFalse(id);
    }

    public Asset create(final Asset asset) {
        final Asset saved = assetDao.save(asset);

        if (saved.getDataProcessing() == null) {
            saved.setDataProcessing(new DataProcessing());
        }
        if (saved.getTia() == null) {
            saved.setTia(new TransferImpactAssessment());
            saved.getTia().setAsset(asset);
        }
        addDefaultSubSupplier(saved);
        return saved;
    }

    public void update(final Asset asset) {
        addDefaultSubSupplier(asset);
        assetDao.saveAndFlush(asset);
    }

    public void delete(final Asset asset) {
        assetDao.delete(asset);
    }

    public Optional<Asset> findByProperty(final String key, final String value) {
        return assetDao.findByPropertyValue(key, value);
    }

    public List<Asset> findAllByRelations(final List<Relation> relations) {
        final List<Long> lookupIds = relations.stream()
            .map(r -> r.getRelationAType() == RelationType.ASSET
                ? r.getRelationAId()
                : r.getRelationBId())
            .toList();
        return assetDao.findAllById(lookupIds);
    }

    public List<Asset> findRelatedTo(final Register register) {
        final List<Relation> relations = relationService.findRelatedToWithType(register, RelationType.ASSET);
        return findAllByRelations(relations);
    }

    public List<Asset> findBySupplier(final Supplier supplier) {
        return assetDao.findBySupplierAndDeletedFalse(supplier);
    }

    public boolean isEditable(final Asset asset) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
            .anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)
                || r.getAuthority().equals(Roles.ADMINISTRATOR))
                || asset.getResponsibleUsers().stream()
            .anyMatch(user -> user.getUuid().equals(SecurityUtil.getPrincipalUuid()));
    }

	public boolean isEditable(final List<Asset> assets) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAuthorities().stream()
				.anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)
						|| r.getAuthority().equals(Roles.ADMINISTRATOR))
				|| assets.stream().flatMap(a->a.getResponsibleUsers().stream())
				.anyMatch(user -> user.getUuid().equals(SecurityUtil.getPrincipalUuid()));
	}

    /**
     * Will find the main supplier {@link AssetSupplierMapping} if none is found a default placeholder will be returned
     */
    public AssetSupplierMapping findMainSupplier(final Asset asset) {
        if (asset.getSupplier() == null) {
            return AssetSupplierMapping.builder().asset(asset).build();
        }
        return asset.getSuppliers().stream()
            .filter(s -> Objects.equals(s.getSupplier().getId(), asset.getSupplier().getId()))
            .findFirst().orElse(AssetSupplierMapping.builder().asset(asset).build());
    }


    /** if NextInspection is of type DATE it just returns the inputted value*/
    public LocalDate getNextInspectionByInterval(final Asset asset, final LocalDate date) {
        if (asset.getNextInspection() == null) {
            return null;
        }
        return switch (asset.getNextInspection()) {
            case DBS -> null;
            case DATE -> //should never actually hit this case as we check prior.
                date;
            case MONTH -> date.plusMonths(1);
            case QUARTER -> date.plusMonths(3);
            case HALF_YEAR -> date.plusMonths(6);
            case YEAR -> date.plusYears(1);
            case EVERY_2_YEARS -> date.plusYears(2);
            case EVERY_3_YEARS -> date.plusYears(3);
        };
    }

    @Transactional
    public void deleteById(final Asset asset) {
        relationService.deleteRelatedTo(asset.getId());
        dataProcessingDao.delete(asset.getDataProcessing());
        assetDao.delete(asset);
    }


    private static void addDefaultSubSupplier(final Asset saved) {
        if (saved.getSupplier() != null && saved.getSuppliers().isEmpty()) {
            saved.getSuppliers().add(AssetSupplierMapping.builder()
                .asset(saved)
                .supplier(saved.getSupplier())
                .build());
        }
    }

    public Asset save(final Asset asset) {
        return assetDao.save(asset);
    }

    @Transactional
    public void updateNextRevisionAssociatedTask(final DPIA dpia) {
			findAssociatedCheck(dpia)
				.ifPresent(t -> dpia.setNextRevision(t.getNextDeadline()));
    }

    public Optional<Task> findAssociatedCheck(final DPIA dpia) {
        final List<Task> tasks = taskService.findTaskWithProperty(ASSOCIATED_ASSET_DPIA_PROPERTY, "" + dpia.getId());
        if (tasks == null || tasks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tasks.get(0));
    }

    @Transactional
    public Task createOrUpdateAssociatedCheck(DPIA dpia) {
        final LocalDate deadline = dpia.getNextRevision();
        if (deadline != null && dpia.getRevisionInterval() != null) {
            final Task task = findAssociatedCheck(dpia).orElseGet(() -> createAssociatedCheck(dpia));
			String name = "DPIA for " + dpia.getAssets().getFirst().getName();
			name +=  (dpia.getAssets().size() > 1) ? " med flere" : "";
			task.setName(name);
            task.setNextDeadline(dpia.getNextRevision());
            task.setResponsibleUser(dpia.getResponsibleUser() != null ? dpia.getResponsibleUser() : userService.currentUser());
            task.setDescription("Revider DPIA for " + String.join(", ",  dpia.getAssets().stream().map(Relatable::getName).toList()));
            setTaskRevisionInterval(dpia, task);
            return task;
        }
        return null;
    }

    private Task createAssociatedCheck(final DPIA dpia) {
        final List<Asset> assets = dpia.getAssets();
        final Task task = new Task();

		if(assets.size() > 1) {
			task.setName("DPIA for " + assets.getFirst().getName() + " med flere");
		} else {
        	task.setName("DPIA for " + assets.getFirst().getName());
		}

        task.setCreatedAt(LocalDateTime.now());
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(ASSOCIATED_ASSET_DPIA_PROPERTY)
            .value("" + dpia.getId())
            .build()
        );
        task.setTaskType(TaskType.CHECK);
		task.setResponsibleUser(dpia.getResponsibleUser() != null ? dpia.getResponsibleUser() : userService.currentUser());
        task.setNextDeadline(dpia.getNextRevision());
        task.setNotifyResponsible(true);
        final Task savedTask = taskService.saveTask(task);
        relationService.addRelation(dpia, task);
        return savedTask;
    }

    private static void setTaskRevisionInterval(final DPIA dpia, final Task task) {
        switch(dpia.getRevisionInterval()) {
            case YEARLY -> task.setRepetition(TaskRepetition.YEARLY);
            case EVERY_SECOND_YEAR -> task.setRepetition(TaskRepetition.EVERY_SECOND_YEAR);
            case EVERY_THIRD_YEAR -> task.setRepetition(TaskRepetition.EVERY_THIRD_YEAR);
            case NONE -> task.setRepetition(TaskRepetition.NONE);
        }
    }

    public PlaceholderInfo getDPIAResponsePlaceholderInfo(Asset asset) {
        DataProcessing dataProcessing = asset.getDataProcessing();
        final ChoiceList accessWhoChoices = choiceService.findChoiceList("dp-access-who-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find accessWhoIdentifiers Choices"));
        final ChoiceList accessCountChoices = choiceService.findChoiceList("dp-access-count-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find accessWhoIdentifiers Choices"));
        final ChoiceList storageDurationChoices = choiceService.findChoiceList("dp-person-storage-duration-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find storageDurationIdentifiers Choices"));
        final ChoiceList personCategories = choiceService.findChoiceList("dp-person-categories-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCategoriesInformationIdentifiers1 Choices"));
        final ChoiceList sensitivePersonCategories = choiceService.findChoiceList("dp-person-categories-sensitive-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCategoriesInformationIdentifiers2 Choices"));
        final ChoiceList registeredPersonCategories = choiceService.findChoiceList("dp-categories-list")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find personCategoriesRegisteredIdentifiers Choices"));
        final String selectedAccessWhoTitles = accessWhoChoices.getValues().stream().filter(c -> dataProcessing.getAccessWhoIdentifiers().contains(c.getIdentifier())).map(ChoiceValue::getCaption).collect(Collectors.joining(", "));
        final String selectedAccessCountTitle = !StringUtils.hasLength(dataProcessing.getAccessCountIdentifier()) ? "" : accessCountChoices.getValues().stream().filter(c -> Objects.equals(dataProcessing.getAccessCountIdentifier(), c.getIdentifier())).findAny().get().getCaption();
        final String howLongTitle = !StringUtils.hasLength(dataProcessing.getStorageTimeIdentifier()) ? "" : storageDurationChoices.getValues().stream().filter(c -> Objects.equals(dataProcessing.getStorageTimeIdentifier(), c.getIdentifier())).findAny().get().getCaption();
        final Set<String> personalDataTypesTitles = new HashSet<>();
        final Set<String> categoriesOfRegisteredTitles = new HashSet<>();
        for (DataProcessingCategoriesRegistered registeredCategory : dataProcessing.getRegisteredCategories()) {
            personCategories.getValues().stream().filter(c -> Objects.equals(c.getIdentifier(), registeredCategory.getPersonCategoriesRegisteredIdentifier())).findAny().ifPresent(choiceValue -> categoriesOfRegisteredTitles.add(choiceValue.getCaption()));
            personalDataTypesTitles.addAll(sensitivePersonCategories.getValues().stream().filter(c -> registeredCategory.getPersonCategoriesInformationIdentifiers().contains(c.getIdentifier())).map(ChoiceValue::getCaption).collect(Collectors.toSet()));
            personalDataTypesTitles.addAll(registeredPersonCategories.getValues().stream().filter(c -> registeredCategory.getPersonCategoriesInformationIdentifiers().contains(c.getIdentifier())).map(ChoiceValue::getCaption).collect(Collectors.toSet()));
        }
        return new PlaceholderInfo(dataProcessing.getTypesOfPersonalInformationFreetext() == null ? "" : dataProcessing.getTypesOfPersonalInformationFreetext(), selectedAccessWhoTitles, selectedAccessCountTitle, howLongTitle, personalDataTypesTitles, categoriesOfRegisteredTitles);
    }

    public boolean isInUseOnAssets(Long assetTypeId) {
        return assetDao.countByAssetType_Id(assetTypeId) > 0;
    }

    record DPIAQuestionDTO(String question, String templateAnswer, String response) {}
    record DPIASectionDTO(String sectionIdentifier, String heading, String explainer, List<DPIAQuestionDTO> questions) {}
    record DPIAThreatAssessmentDTO(long threatAssessmentId, String threatAssessmentName, String date, boolean signed, RiskAssessment status) {}
    public byte[] getDPIAPdf(DPIA dpia) throws IOException {
        String html = getDPIAHTML(dpia);
        return convertHtmlToPdf(html);
    }

    public byte[] getDPIAScreeningPdf(DPIA dpia) throws IOException {
        String html = getDPIAScreeningHTML(dpia);
        return convertHtmlToPdf(html);
    }

    private String getDPIAHTML(DPIA dpia) {
        final List<Asset> assets = dpia.getAssets();
        final List<Relatable> allRelatedTo = assets.stream().flatMap(a -> relationService.findAllRelatedTo(a).stream()).toList();
        final List<ThreatAssessment> threatAssessments = allRelatedTo.stream()
				.filter(r -> r.getRelationType() == RelationType.THREAT_ASSESSMENT)
				.map(ThreatAssessment.class::cast)
				.sorted(Comparator.comparing(Relatable::getCreatedAt)
						.reversed())
				.collect(Collectors.toList());

		var context = new Context();

		List<DPIASectionDTO> sections =  buildDPIASections(dpia);
        context.setVariable("dpiaSections",sections);
        context.setVariable("dpiaThreatAssesments", buildDPIAThreatAssessments(dpia, threatAssessments));
        context.setVariable("conclusion", dpia.getConclusion());
		context.setVariable("assetNames", String.join(", ", dpia.getAssets().stream().map(Asset::getName).toList()));
		context.setVariable("assetTypeNames", String.join(", ", dpia.getAssets().stream().map(a->a.getAssetType().getCaption()).toList()));
        context.setVariable("responsibleUserNames", String.join(", ", assets.stream().flatMap(a -> a.getResponsibleUsers().stream().map(u -> u.getName() + " (" +u.getUserId()+")")).toList()));
        context.setVariable("managerNames", String.join(", ", assets.stream().flatMap( a -> a.getManagers().stream().map(u -> u.getName() + " (" +u.getUserId()+")")).toList()));
        context.setVariable("supplierName", String.join(", ", assets.stream().map( a -> a.getSupplier().getName()).filter(n -> n != null && n.isBlank()).toList()));

        return templateEngine.process("reports/dpia/dpia_pdf", context);
    }


    public record ScreeningQuestionDTO(String question, String answer,  boolean dangerous) {}
    public record ScreeningCategoryDTO(String title, long dangerousValueCount, List<ScreeningQuestionDTO> questions) {}
public record ScreeningDTO(Long dpiaId, List<ScreeningCategoryDTO> categories, String recommendation, EstimationDTO recommendedEstimation) {
}

    private String getDPIAScreeningHTML(DPIA dpia) {
        List<String> dangerousValues = listOf("dpia-yes", "dpia-partially", "dpia-dont-know");
        List<String> alwaysRed = listOf("dpia-7");

        final List<DataProtectionImpactScreeningAnswer> assetDPIADTOs = new ArrayList<>();
        final List<ChoiceDPIA> choiceDPIA = choiceDPIADao.findAll();
        for (final ChoiceDPIA choice : choiceDPIA) {
            final DataProtectionImpactScreeningAnswer defaultAnswer = new DataProtectionImpactScreeningAnswer();
            defaultAnswer.setAssessment(dpia.getDpiaScreening());
            defaultAnswer.setChoice(choice);
            defaultAnswer.setAnswer(null);
            defaultAnswer.setId(0);
            final DataProtectionImpactScreeningAnswer dpiaAnswer = dpia.getDpiaScreening().getDpiaScreeningAnswers().stream()
                .filter(m -> Objects.equals(m.getChoice().getId(), choice.getId()))
                .findAny().orElse(defaultAnswer);
            assetDPIADTOs.add(dpiaAnswer);
        }

        var context = new Context();
        context.setVariable("questions", assetDPIADTOs);

        Map<String, List<DataProtectionImpactScreeningAnswer>> categories = assetDPIADTOs.stream().collect(groupingBy(dto -> dto.getChoice().getCategory(), toList()));

        boolean alwaysRedPresent = false;

        Map<String, List<ScreeningQuestionDTO>> questionDtos = new HashMap<>();
        for (var entrySet : categories.entrySet()) {
            if (entrySet.getValue().stream().anyMatch(answ -> alwaysRed.contains(answ.getChoice().getIdentifier()))) {
                alwaysRedPresent = true;
            }
            questionDtos.put(entrySet.getKey(), entrySet.getValue().stream().map(answer -> {
                Optional<ChoiceValue> answerChoice = answer.getChoice().getValues().stream().filter(val -> val.getIdentifier().equalsIgnoreCase(answer.getAnswer())).findAny();
                return new ScreeningQuestionDTO(
                    answer.getChoice().getName(),
                    answerChoice.isPresent() ? answerChoice.get().getCaption() : "",
                    alwaysRed.contains(answer.getAnswer())
                        || dangerousValues.contains(answer.getAnswer()));
            }).toList());
        }

        List<ScreeningCategoryDTO> categoryDtos = questionDtos.entrySet().stream().map(entrySet -> new ScreeningCategoryDTO(
            entrySet.getKey(),
            entrySet.getValue().stream().filter(question -> question.dangerous).count(),
            entrySet.getValue()
        )).toList();

        Map<String, Integer> orderMapping = new HashMap<>();
        orderMapping.put("Personoplysninger", 0);
        orderMapping.put("Behandling", 1);
        orderMapping.put("Nye teknologier", 2);
        List<ScreeningCategoryDTO> categoryDtosSorted = new ArrayList<>(categoryDtos);
        categoryDtosSorted.sort(Comparator.comparingInt(a -> orderMapping.get(a.title) != null ? orderMapping.get(a.title) : 999));


        String recommendation = "Du skal ikke udføre konsekvensanalyse da behandlingen sandsynligvis ikke indebærer risiko for de registrerede";
        EstimationDTO recommendedEstimation = EstimationDTO.BLANK;
        if (alwaysRedPresent || categoryDtosSorted.stream().anyMatch(cat -> cat.dangerousValueCount > 1)){
            recommendation ="Du skal udføre konsekvensanalyse da behandlingen sandsynligvis indebærer en høj risiko for de registrerede (se røde bekymringer)";
            recommendedEstimation = EstimationDTO.DANGER;
        } else if (categoryDtosSorted.stream().anyMatch(cat -> cat.dangerousValueCount > 0)) {
            recommendation = "Du skal ikke udføre konsekvensanalyse da behandlingen sandsynligvis indebærer risiko for de registrerede, men ikke en høj risiko (se gule bekymringer)";
            recommendedEstimation = EstimationDTO.WARNING;
        }

        ScreeningDTO screeningDTO = new ScreeningDTO(
            dpia.getId(),
            categoryDtosSorted,
            recommendation,
            recommendedEstimation
        );

        context.setVariable("screening", screeningDTO);
        return templateEngine.process("reports/dpia/screening_pdf", context);
    }

    private List<DPIASectionDTO> buildDPIASections(DPIA dpia) {
        List<DPIASectionDTO> sections = new ArrayList<>();
        List<DPIATemplateSection> allSections = dpiaTemplateSectionService.findAll().stream()
            .sorted(Comparator.comparing(DPIATemplateSection::getSortKey))
            .toList();

        for (DPIATemplateSection templateSection : allSections) {
            if (templateSection.isHasOptedOut()) {
                continue;
            }

            List<DPIAQuestionDTO> questionDTOS = new ArrayList<>();
            DPIAResponseSection matchSection = dpia.getDpiaResponseSections().stream().filter(s -> s.getDpiaTemplateSection().getId() == templateSection.getId()).findAny().orElse(null);

            if (matchSection != null && !matchSection.isSelected()) {
                continue;
            }

            List<DPIATemplateQuestion> questions = templateSection.getDpiaTemplateQuestions().stream()
                .sorted(Comparator.comparing(DPIATemplateQuestion::getSortKey))
                .toList();

            for (DPIATemplateQuestion templateQuestion : questions) {

                DPIAResponseSectionAnswer matchAnswer = matchSection == null ? null : matchSection.getDpiaResponseSectionAnswers().stream().filter(s -> s.getDpiaTemplateQuestion().getId() == templateQuestion.getId()).findAny().orElse(null);

                String templateAnswer = templateQuestion.getAnswerTemplate() == null ? "" : templateQuestion.getAnswerTemplate();
                if (matchAnswer == null) {
                    questionDTOS.add(new DPIAQuestionDTO(templateQuestion.getQuestion(), templateAnswer, ""));
                } else {
					String imgParsedResponse = handleResponseImg(matchAnswer.getResponse());
                    questionDTOS.add(new DPIAQuestionDTO(templateQuestion.getQuestion(), templateAnswer, imgParsedResponse));
                }
            }

			sections.add(new DPIASectionDTO(templateSection.getIdentifier(), templateSection.getHeading(), templateSection.getExplainer(), questionDTOS));

        }
        return sections;
    }

    private List<DPIAThreatAssessmentDTO> buildDPIAThreatAssessments(DPIA dpia, List<ThreatAssessment> threatAssessments) {
        List<DPIAThreatAssessmentDTO> result = new ArrayList<>();
        Set<String> selectedThreatAssessments = dpia.getCheckedThreatAssessmentIds() == null ? new HashSet<>() : Arrays.stream(dpia.getCheckedThreatAssessmentIds().split(",")).collect(Collectors.toSet());
        for (ThreatAssessment threatAssessment : threatAssessments) {
            boolean selected = selectedThreatAssessments.contains(threatAssessment.getId().toString());
            if (selected) {
                String date = threatAssessment.getCreatedAt().format(Constants.DK_DATE_FORMATTER);
                DPIAThreatAssessmentDTO dto = new DPIAThreatAssessmentDTO(threatAssessment.getId(), threatAssessment.getName(), date, threatAssessment.getThreatAssessmentReportApprovalStatus().equals(ThreatAssessmentReportApprovalStatus.SIGNED), threatAssessment.getAssessment());
                result.add(dto);
            }
        }
        return result;
    }

    private byte[] convertHtmlToPdf(String html) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        var renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream);
        var result = outputStream.toByteArray();
        outputStream.close();
        return result;
    }

	private String handleResponseImg(String response) {
		Document doc = Jsoup.parse(response);
		Elements imgTags = doc.select("img[src]");

		for (Element img : imgTags) {
			String src = img.attr("src");
			try {
				String processedSrc = convertImgSrcToBase64(src);
				img.attr("src", processedSrc);
				img = setImgResizedStyle(img);
			} catch (IOException | InterruptedException ioE) {
				System.out.println("Failed to convert image: " + src);
			}
		}

		doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
		doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
		doc.outputSettings().charset(StandardCharsets.UTF_8);

		return doc.body().html();
	}


	private String convertImgSrcToBase64(String imageUrl) throws IOException, InterruptedException {
		String contentType = "image/png";
		String result = "";

		int keyIndex = imageUrl.indexOf("key=");
		if (imageUrl.contains("key=")) {
			String key = imageUrl.substring(keyIndex + "key=".length());
			String base64 = Base64.getEncoder().encodeToString(s3Service.downloadBytes(key));
			if (key.endsWith(".jpg") || key.endsWith(".jpeg")) {
				contentType = "image/jpeg";
			}
			else if (key.endsWith(".gif")) {
				contentType = "image/gif";
			}
			else if (key.endsWith(".svg")) {
				contentType = "image/svg";
			}
			result="data:" + contentType + ";base64," + base64;
		} else {
			result = getExternalImgAsBase64(imageUrl);
		}
		return result;
	}

	private String getExternalImgAsBase64(String imageUrl) {
		String contentType = "image/png";
		String imageBytes = "";
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(imageUrl))
					.GET()
					.build();

			HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

			if (response.statusCode() != 200) {
				throw new IOException("Failed to fetch image from external URL: " + imageUrl);
			}

			imageBytes =  Base64.getEncoder().encodeToString(response.body());
			contentType = response.headers().firstValue("Content-Type").orElse("image/png");

			// Strip any charset info, just keep the MIME type
			int semicolonIndex = contentType.indexOf(";");
			if (semicolonIndex != -1) {
				contentType = contentType.substring(0, semicolonIndex);
			}
		} catch (IOException | InterruptedException ioE) {
			log.warn("Could not retrieve external image url for report");
		}
		return "data:" + contentType + ";base64," + imageBytes;
	}

	/**
	 * Dirty hack because aspect ratio is to new to be supported by flying saucer
	 * @param img
	 * @return
	 */
	private Element setImgResizedStyle(Element img) {
		String style = img.attr("style");

		Float aspectRatioA = null;
		Float aspectRatioB = null;
		Float widthPercent = null;

		Map<String, String> newProperties = new HashMap<>();
		String[] properties = style.split(";");
		for (String property : properties) {
			String[] keyValuePair = property.split(":");
			String key = keyValuePair[0];
			String value = keyValuePair[1];
			newProperties.put(key, value);
		}

		if (!newProperties.containsKey("aspect-ratio") || !newProperties.containsKey("width")) {
			return img;
		}
		String[] aspectRatioSet = newProperties.get("aspect-ratio").split("/");
		try {
			aspectRatioA = (aspectRatioSet[0] == null || aspectRatioSet[0].trim().isEmpty()) ? null : Float.parseFloat(aspectRatioSet[0].trim());

			aspectRatioB = (aspectRatioSet[1] == null || aspectRatioSet[1].trim().isEmpty()) ? null : Float.parseFloat(aspectRatioSet[1].trim());

			String widthPercentString = newProperties.get("width");
			widthPercent = widthPercentString.replace("%", "").trim().isEmpty() ? null : Float.parseFloat(widthPercentString.replace("%", "").trim());
		} catch (NumberFormatException e) {
			return img;
		}

		if (widthPercent != null && aspectRatioA != null && aspectRatioB != null) {
			Float ratio = aspectRatioA / aspectRatioB;
			Float heightPercent =  widthPercent * ratio;
			newProperties.put("height", heightPercent + "%");
		}

		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, String> entry : newProperties.entrySet()) {
			builder
					.append(entry.getKey())
					.append(":")
					.append(entry.getValue())
					.append(";");
		}
		System.out.println(builder.toString());
		img.attr("style", builder.toString());
		return img;
	}
}
