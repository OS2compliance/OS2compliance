package dk.digitalidentity.service;

import dk.digitalidentity.Constants;
import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.AssetOversightDao;
import dk.digitalidentity.dao.DataProcessingDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DPIAResponseSection;
import dk.digitalidentity.model.entity.DPIAResponseSectionAnswer;
import dk.digitalidentity.model.entity.DPIATemplateQuestion;
import dk.digitalidentity.model.entity.DPIATemplateSection;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.DataProtectionImpactAssessmentScreening;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.TransferImpactAssessment;
import dk.digitalidentity.model.entity.enums.DPIAAnswerPlaceholder;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.service.model.PlaceholderInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.ASSOCIATED_ASSET_DPIA_PROPERTY;

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

    public Optional<AssetOversight> getOversight(final Long oversightId) {
        return assetOversightDao.findById(oversightId);
    }

    public Optional<Asset> get(final Long id) {
        return assetDao.findByIdAndDeletedFalse(id);
    }

    public Page<Asset> getPaged(final int pageSize, final int page) {
        return assetDao.findAll(Pageable.ofSize(pageSize).withPage(page));
    }

    public List<Asset> findAllById(final Collection<Long> ids) {
        return assetDao.findAllByIdInAndDeletedFalse(ids);
    }

    public Optional<Asset> findById(final Long id) {
        return assetDao.findByIdAndDeletedFalse(id);
    }

    public Asset create(final Asset asset) {
        final Asset saved = assetDao.save(asset);
        if (saved.getDpiaScreening() == null) {
            saved.setDpiaScreening(new DataProtectionImpactAssessmentScreening());
            saved.getDpiaScreening().setAsset(saved);
        }
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
            case DBS -> date;
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
    public void updateNextRevisionAssociatedTask(final Asset asset) {
        if (asset.getDpia() == null) {
            DPIA dpia = new DPIA();
            dpia.setAsset(asset);
            asset.setDpia(dpia);
        }

        findAssociatedCheck(asset)
            .ifPresent(t -> asset.getDpia().setNextRevision(t.getNextDeadline()));
    }

    public Optional<Task> findAssociatedCheck(final Asset asset) {
        final List<Task> tasks = taskService.findTaskWithProperty(ASSOCIATED_ASSET_DPIA_PROPERTY, "" + asset.getId());
        if (tasks == null || tasks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tasks.get(0));
    }

    @Transactional
    public Task createOrUpdateAssociatedCheck(final Asset asset) {
        final LocalDate deadline = asset.getDpia().getNextRevision();
        if (deadline != null && asset.getDpia().getRevisionInterval() != null) {
            final Task task = findAssociatedCheck(asset).orElseGet(() -> createAssociatedCheck(asset));
            task.setName("DPIA for " + asset.getName());
            task.setNextDeadline(asset.getDpia().getNextRevision());
            task.setResponsibleUser(asset.getResponsibleUsers() != null ? asset.getResponsibleUsers().get(0) : userService.currentUser());
            task.setDescription("Revider DPIA for " + asset.getName());
            setTaskRevisionInterval(asset, task);
            return task;
        }
        return null;
    }

    private Task createAssociatedCheck(final Asset asset) {
        final Task task = new Task();
        task.setName("DPIA for " + asset.getName());
        task.setCreatedAt(LocalDateTime.now());
        task.getProperties().add(Property.builder()
            .entity(task)
            .key(ASSOCIATED_ASSET_DPIA_PROPERTY)
            .value("" + asset.getId())
            .build()
        );
        task.setTaskType(TaskType.CHECK);
        task.setResponsibleUser(asset.getResponsibleUsers() != null ? asset.getResponsibleUsers().get(0) : userService.currentUser());
        task.setNextDeadline(asset.getDpia().getNextRevision());
        task.setNotifyResponsible(true);
        final Task savedTask = taskService.saveTask(task);
        relationService.addRelation(asset, task);
        return savedTask;
    }

    private static void setTaskRevisionInterval(final Asset asset, final Task task) {
        switch(asset.getDpia().getRevisionInterval()) {
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

    record DPIAQuestionDTO(String question, String templateAnswer, String response) {}
    record DPIASectionDTO(String sectionIdentifier, String heading, String explainer, List<DPIAQuestionDTO> questions) {}
    record DPIAThreatAssessmentDTO(long threatAssessmentId, String threatAssessmentName, String date, boolean signed) {}
    public byte[] getDPIAPdf(Asset asset) throws IOException {
        String html = getDPIAHTML(asset);
        return convertHtmlToPdf(html);
    }

    private String getDPIAHTML(Asset asset) {
        final List<Relatable> allRelatedTo = relationService.findAllRelatedTo(asset);
        final List<ThreatAssessment> threatAssessments = allRelatedTo.stream()
            .filter(r -> r.getRelationType() == RelationType.THREAT_ASSESSMENT)
            .map(ThreatAssessment.class::cast)
            .collect(Collectors.toList());
        threatAssessments.sort(Comparator.comparing(Relatable::getCreatedAt).reversed());

        var context = new Context();
        context.setVariable("asset", asset);
        context.setVariable("dpiaSections", buildDPIASections(asset));
        context.setVariable("dpiaThreatAssesments", buildDPIAThreatAssessments(asset, threatAssessments));
        context.setVariable("conclusion", asset.getDpia().getConclusion());
        context.setVariable("responsibleUserNames", asset.getResponsibleUsers().stream().map(u -> u.getName() + "(" + u.getUserId() + ")").collect(Collectors.joining(", ")));
        context.setVariable("managerNames", asset.getManagers().stream().map(u -> u.getName() + "(" + u.getUserId() + ")").collect(Collectors.joining(", ")));
        context.setVariable("supplierName", asset.getSupplier() == null ? "" : asset.getSupplier().getName());

        return templateEngine.process("reports/dpia_pdf", context);
    }

    private List<DPIASectionDTO> buildDPIASections(Asset asset) {
        List<DPIASectionDTO> sections = new ArrayList<>();
        List<DPIATemplateSection> allSections = dpiaTemplateSectionService.findAll().stream()
            .sorted(Comparator.comparing(DPIATemplateSection::getSortKey))
            .collect(Collectors.toList());

        // needed dataprocessing fields
        PlaceholderInfo placeholderInfo = getDPIAResponsePlaceholderInfo(asset);

        for (DPIATemplateSection templateSection : allSections) {
            if (templateSection.isHasOptedOut()) {
                continue;
            }

            List<DPIAQuestionDTO> questionDTOS = new ArrayList<>();
            DPIAResponseSection matchSection = asset.getDpia().getDpiaResponseSections().stream().filter(s -> s.getDpiaTemplateSection().getId() == templateSection.getId()).findAny().orElse(null);

            if (matchSection != null && !matchSection.isSelected()) {
                continue;
            }

            List<DPIATemplateQuestion> questions = templateSection.getDpiaTemplateQuestions().stream()
                .sorted(Comparator.comparing(DPIATemplateQuestion::getSortKey))
                .collect(Collectors.toList());

            for (DPIATemplateQuestion templateQuestion : questions) {
                DPIAResponseSectionAnswer matchAnswer = matchSection == null ? null : matchSection.getDpiaResponseSectionAnswers().stream().filter(s -> s.getDpiaTemplateQuestion().getId() == templateQuestion.getId()).findAny().orElse(null);

                String templateAnswer = templateQuestion.getAnswerTemplate() == null ? "" : templateQuestion.getAnswerTemplate();
                templateAnswer = templateAnswer
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_WHO.getPlaceholder(), placeholderInfo.getSelectedAccessWhoTitles())
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_HOW_MANY.getPlaceholder(), placeholderInfo.getSelectedAccessCountTitle())
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_TYPES.getPlaceholder(), String.join(", ", placeholderInfo.getPersonalDataTypesTitles()))
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_DATA_TYPES_FREETEXT.getPlaceholder(), placeholderInfo.getTypesOfPersonalInformationFreetext())
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_CATEGORIES_OF_REGISTERED.getPlaceholder(), String.join(", ", placeholderInfo.getCategoriesOfRegisteredTitles()))
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_PERSONAL_HOW_LONG.getPlaceholder(), placeholderInfo.getHowLongTitle())
                    .replace(DPIAAnswerPlaceholder.DATA_PROCESSING_DELETE_LINK.getPlaceholder(), asset.getDataProcessing().getDeletionProcedureLink() == null ? "" : "<a href=\"" + asset.getDataProcessing().getDeletionProcedureLink() + "\">" + asset.getDataProcessing().getDeletionProcedureLink() + "</a>");

                if (matchAnswer == null) {
                    questionDTOS.add(new DPIAQuestionDTO(templateQuestion.getQuestion(), templateAnswer, ""));
                } else {
                    questionDTOS.add(new DPIAQuestionDTO(templateQuestion.getQuestion(), templateAnswer, matchAnswer.getResponse()));
                }
            }

            if (matchSection == null) {
                sections.add(new DPIASectionDTO(templateSection.getIdentifier(), templateSection.getHeading(), templateSection.getExplainer(), questionDTOS));
            } else {
                sections.add(new DPIASectionDTO(templateSection.getIdentifier(), templateSection.getHeading(), templateSection.getExplainer(), questionDTOS));
            }

        }
        return sections;
    }

    private List<DPIAThreatAssessmentDTO> buildDPIAThreatAssessments(Asset asset, List<ThreatAssessment> threatAssessments) {
        List<DPIAThreatAssessmentDTO> result = new ArrayList<>();
        Set<String> selectedThreatAssessments = asset.getDpia().getCheckedThreatAssessmentIds() == null ? new HashSet<>() : Arrays.stream(asset.getDpia().getCheckedThreatAssessmentIds().split(",")).collect(Collectors.toSet());
        for (ThreatAssessment threatAssessment : threatAssessments) {
            boolean selected = selectedThreatAssessments.contains(threatAssessment.getId().toString());
            if (selected) {
                String date = threatAssessment.getCreatedAt().format(Constants.DK_DATE_FORMATTER);
                DPIAThreatAssessmentDTO dto = new DPIAThreatAssessmentDTO(threatAssessment.getId(), threatAssessment.getName(), date, threatAssessment.getThreatAssessmentReportApprovalStatus().equals(ThreatAssessmentReportApprovalStatus.SIGNED));
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
}
