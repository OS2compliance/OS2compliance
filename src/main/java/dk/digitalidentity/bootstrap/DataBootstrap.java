package dk.digitalidentity.bootstrap;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.dao.StandardTemplateSectionDao;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.dao.ThreatAssessmentResponseDao;
import dk.digitalidentity.dao.ThreatAssessmentResponseOldDao;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.service.CatalogService;
import dk.digitalidentity.service.ChoiceListImporter;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.importer.RegisterImporter;
import dk.digitalidentity.service.importer.StandardTemplateImporter;
import dk.digitalidentity.service.importer.ThreatCatalogImporter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static dk.digitalidentity.Constants.DATA_MIGRATION_VERSION_SETTING;

/**
 * This class handles data bootstrapping/seeding.
 * Since OS2compliance comes with a lot of data baked in, we need some way of updating it when we make a new release,
 * this class does that by keeping track of what data version is the current one and then updating the data incrementally.
 * Much like flyway but for the actual database content and not structure.
 */
@Order(100)
@Component
@RequiredArgsConstructor
public class DataBootstrap implements ApplicationListener<ApplicationReadyEvent> {
    private final ChoiceListImporter choiceImporter;
    private final ThreatCatalogImporter threatCatalogImporter;
    private final StandardTemplateImporter templateImporter;
    private final TagDao tagDao;
    private final RegisterService registerService;
    private final RegisterImporter registerImporter;
    private final OS2complianceConfiguration config;
    private final SettingsService settingsService;
    private final StandardTemplateSectionDao standardTemplateSectionDao;
    private final ThreatAssessmentResponseDao threatAssessmentResponseDao;
    private final ThreatAssessmentResponseOldDao threatAssessmentResponseOldDao;
    private final ThreatAssessmentService threatAssessmentService;
    private final CatalogService catalogService;
    private final TaskService taskService;

    @Value("classpath:data/registers/*.json")
    private Resource[] registers;

    @Override
    @Transactional
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        if (!config.isSeedData()) {
            return;
        }
        incrementAndPerformIfVersion(0, this::seedV0);
        incrementAndPerformIfVersion(1, this::seedV1);
        incrementAndPerformIfVersion(2, this::seedV2);
        incrementAndPerformIfVersion(3, this::seedV3);
        incrementAndPerformIfVersion(4, this::seedV4);
        incrementAndPerformIfVersion(5, this::seedV5);
        incrementAndPerformIfVersion(6, this::seedV6);
        incrementAndPerformIfVersion(7, this::seedV7);
        incrementAndPerformIfVersion(8, this::seedV8);
        incrementAndPerformIfVersion(9, this::seedV9);
        incrementAndPerformIfVersion(10, this::seedV10);
        incrementAndPerformIfVersion(11, this::seedV11);
    }

    private void incrementAndPerformIfVersion(final int version, final Runnable applier) {
        final int currentVersion = settingsService.getInt(DATA_MIGRATION_VERSION_SETTING, 0);
        if (currentVersion == version) {
            applier.run();
            settingsService.setInt(DATA_MIGRATION_VERSION_SETTING, version + 1);
        }
    }

    private void seedV11() {
        // Touch all tasks to regenerate the localized enums field
        taskService.saveAll(taskService.findAll().stream().peek(t -> t.setVersion(t.getVersion()+1)).collect(Collectors.toList()));
    }

    /*
    * Add sort key by extracting digits from the threats identifier.
    */
    private void seedV10() {
        final List<ThreatCatalog> catalogList = catalogService.findAll();
        for (final ThreatCatalog catalog : catalogList) {
            catalog.getThreats()
                .forEach(threat -> {
                    final String identifierDigits = StringUtils.getDigits(threat.getIdentifier());
                    if (identifierDigits != null && !identifierDigits.isEmpty()) {
                        threat.setSortKey(Long.parseLong(identifierDigits));
                    }
                });
        }
    }

    private void seedV9() {
        threatAssessmentResponseOldDao.findAll().forEach(
            old -> {
                final ThreatAssessmentResponse response = new ThreatAssessmentResponse();
                response.setNotRelevant(old.isNotRelevant());
                response.setProbability(old.getProbability());
                response.setConfidentialityRegistered(old.getConfidentialityRegistered());
                response.setConfidentialityOrganisation(old.getConfidentialityOrganisation());
                response.setIntegrityRegistered(old.getIntegrityRegistered());
                response.setIntegrityOrganisation(old.getIntegrityOrganisation());
                response.setAvailabilityRegistered(old.getAvailabilityRegistered());
                response.setAvailabilityOrganisation(old.getAvailabilityOrganisation());
                response.setProblem(old.getProblem());
                response.setExistingMeasures(old.getExistingMeasures());
                response.setMethod(old.getMethod());
                response.setElaboration(old.getElaboration());
                response.setResidualRiskProbability(old.getResidualRiskProbability());
                response.setResidualRiskConsequence(old.getResidualRiskConsequence());
                response.setThreatAssessment(old.getThreatAssessment());
                response.setThreatCatalogThreat(old.getThreatCatalogThreat());
                response.setCustomThreat(old.getCustomThreat());

                if (old.getThreatAssessment() != null) {
                    response.setName(StringUtils.truncate(old.getThreatAssessment().getName(), 768));
                }
                if (old.getCustomThreat() != null) {
                    response.setName(StringUtils.truncate(old.getCustomThreat().getDescription(), 768));
                }
                threatAssessmentResponseDao.save(response);
            }
        );

        final List<ThreatAssessmentResponse> responses = threatAssessmentResponseDao.findAll();
        for (final ThreatAssessmentResponse response : responses) {
            if (response.getCustomThreat() != null) {
                response.setName(response.getCustomThreat().getDescription());
            } else if (response.getThreatCatalogThreat() != null) {
                response.setName(response.getThreatCatalogThreat().getDescription());
            }
        }
        threatAssessmentResponseDao.saveAll(responses);
    }

    private void seedV8() {
        // Prefix registers with 0 if they only have one digit
        registerService.findAllArticle30().stream()
            .filter(r -> {
                final String[] split = StringUtils.split(r.getName(), ".");
                if (split.length > 1) {
                    final String digits = StringUtils.getDigits(split[0]);
                    return digits.length() == 1;
                }
                return false;
            })
            .forEach(r -> r.setName('0' + r.getName()));
    }

    private void seedV7() {
        // All non iso 27002 should be selected by default
        standardTemplateSectionDao.findByIdentifierStartsWith("nsis").stream()
            .map(StandardTemplateSection::getStandardSection)
            .filter(Objects::nonNull)
            .forEach(s -> s.setSelected(true));
    }

    private void seedV6() {
        // subsections weren't displaying their association with parent. Changed format from 1 to parent.1 (ex 3.1.1 section 1 becomes 3.1.1.1)
        try {
            templateImporter.updateStandardSections("./data/standards/nsis2_sections.json");
        } catch (final IOException e) {
            throw  new RuntimeException(e);
        }
    }

    private void seedV5() {
        // Article 30 registers had mistakenly been marked with $10 they should have had $8
        registerService.findAllArticle30().stream()
            .filter(r -> r.getGdprChoices().contains("register-gdpr-valp10"))
            .forEach(r -> {
                r.getGdprChoices().remove("register-gdpr-valp10");
                r.getGdprChoices().add("register-gdpr-valp8");
            });
    }

    private void seedV4() {
        // Reimport measures as new options was added.
        try {
            choiceImporter.importValues("./data/choices/measures-values.json");
            // Delete and reimport the measure choices
            choiceImporter.importMeasuresList("./data/choices/measures-list.json");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void seedV3() {
        // Threat assessment status field, needs update (again)
        threatAssessmentService.findAll().forEach(threatAssessmentService::setThreatAssessmentColor);
    }

    private void seedV2() {
        // Threat assessment status field, we need to update them all
        threatAssessmentService.findAll().forEach(threatAssessmentService::setThreatAssessmentColor);
    }

    private void seedV1() {
        // NSIS 4.1.4 was missing in the initial release
        try {
            templateImporter.importStandardSections("./data/standards/nsis2_missing_section.json");
            // And 5+ from 4.1.3 should actually have been in 4.1.4
            final StandardTemplateSection newParent = standardTemplateSectionDao.findById("nsis_2_0_2a_414").orElseThrow();
            standardTemplateSectionDao.findById("nsis_2_0_2a_413_5").ifPresent(s -> {
                s.setSection("1");
                s.setSortKey(1);
                s.setParent(newParent);
                newParent.getChildren().add(s);
            });
            standardTemplateSectionDao.findById("nsis_2_0_2a_413_6").ifPresent(s -> {
                s.setSection("2");
                s.setSortKey(2);
                s.setParent(newParent);
                newParent.getChildren().add(s);
            });
            standardTemplateSectionDao.findById("nsis_2_0_2a_413_7").ifPresent(s -> {
                s.setSection("3");
                s.setSortKey(3);
                s.setParent(newParent);
                newParent.getChildren().add(s);
            });
            standardTemplateSectionDao.findById("nsis_2_0_2a_413_Q").ifPresent(s -> {
                s.setSection("4");
                s.setSortKey(4);
                s.setParent(newParent);
                newParent.getChildren().add(s);
            });
            standardTemplateSectionDao.findById("nsis_2_0_2a_413_QQ").ifPresent(s -> {
                s.setSection("5");
                s.setSortKey(5);
                s.setParent(newParent);
                newParent.getChildren().add(s);
            });
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void seedV0() {
        try {
            choiceImporter.importValues("./data/choices/measures-values.json");
            choiceImporter.importMeasuresList("./data/choices/measures-list.json");

            choiceImporter.importValues("./data/choices/dpia-values.json");
            choiceImporter.importDPIAList("./data/choices/dpia-questions.json");

            choiceImporter.importValues("./data/choices/register-values.json");
            choiceImporter.importList("./data/choices/register-gdpr.json");
            choiceImporter.importList("./data/choices/register-gdpr-p6.json");
            choiceImporter.importList("./data/choices/register-gdpr-p7.json");

            choiceImporter.importValues("./data/choices/data-processing-values.json");
            choiceImporter.importList("./data/choices/data-processing-access-count-list.json");
            choiceImporter.importList("./data/choices/data-processing-access-who-list.json");
            choiceImporter.importList("./data/choices/data-processing-categories-list.json");
            choiceImporter.importList("./data/choices/data-processing-person-categories-list.json");
            choiceImporter.importList("./data/choices/data-processing-person-categories-sensitive-list.json");
            choiceImporter.importList("./data/choices/data-processing-person-count-list.json");
            choiceImporter.importList("./data/choices/data-processing-person-storage-duration-list.json");
            choiceImporter.importList("./data/choices/data-processing-receiver-list.json");
            choiceImporter.importList("./data/choices/data-processing-supplier-accept-list.json");

            threatCatalogImporter.importCatalog("./data/threats/catalog_none.json");
            threatCatalogImporter.importCatalog("./data/threats/catalog_a.json");
            threatCatalogImporter.importThreats("./data/threats/catalog_a_values.json");
            threatCatalogImporter.importCatalog("./data/threats/catalog_b.json");
            threatCatalogImporter.importThreats("./data/threats/catalog_b_values.json");
            templateImporter.importStandardTemplate("./data/standards/iso27001.json");
            templateImporter.importStandardSections("./data/standards/iso27001_sections.json");
            templateImporter.importStandardTemplate("./data/standards/iso27002.json");
            templateImporter.importStandardSections("./data/standards/iso27002_sections.json");
            templateImporter.importStandardTemplate("./data/standards/iso27002_2017.json");
            templateImporter.importStandardSections("./data/standards/iso27002_2017_sections.json");
            templateImporter.importStandardTemplate("./data/standards/nsis2.json");
            templateImporter.importStandardSections("./data/standards/nsis2_sections.json");

            addRegistersV0();
            addTagsV0();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addRegistersV0() throws IOException {
        final List<Resource> sortedResources = new ArrayList<>(Arrays.asList(registers));
        sortedResources.sort(Comparator.comparing(Resource::getFilename));
        for (final Resource register : sortedResources) {
            registerImporter.importRegister(register);
        }
    }

    private void addTagsV0() {
        if (tagDao.findByValue("NSIS").isEmpty()) {
            final Tag nsis = new Tag();
            nsis.setValue("NSIS");
            tagDao.save(nsis);
        }

        if (tagDao.findByValue("RA-revision").isEmpty()) {
            final Tag ra = new Tag();
            ra.setValue("RA-revision");
            tagDao.save(ra);
        }

        if (tagDao.findByValue("IT-revision").isEmpty()) {
            final Tag it = new Tag();
            it.setValue("IT-revision");
            tagDao.save(it);
        }

        if (tagDao.findByValue("NIS2").isEmpty()) {
            final Tag nis2 = new Tag();
            nis2.setValue("NIS2");
            tagDao.save(nis2);
        }
    }
}
