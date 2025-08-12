package dk.digitalidentity.bootstrap;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.dao.ChoiceValueDao;
import dk.digitalidentity.dao.StandardTemplateSectionDao;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.integration.kitos.KitosConstants;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.enums.NotificationSetting;
import dk.digitalidentity.model.entity.enums.RegisterSetting;
import dk.digitalidentity.model.entity.enums.ReportSetting;
import dk.digitalidentity.service.CatalogService;
import dk.digitalidentity.service.ChoiceListImporter;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.DPIAService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.importer.DPIATemplateSectionImporter;
import dk.digitalidentity.service.importer.RegisterImporter;
import dk.digitalidentity.service.importer.StandardTemplateImporter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

import static dk.digitalidentity.Constants.DATA_MIGRATION_VERSION_SETTING;

/**
 * This class handles data bootstrapping/seeding.
 * Since OS2compliance comes with a lot of data baked in, we need some way of updating it when we make a new release,
 * this class does that by keeping track of what data version is the current one and then updating the data incrementally.
 * Much like flyway but for the actual database content and not structure.
 */
@Slf4j
@Order(100)
@Component
@RequiredArgsConstructor
public class DataBootstrap implements ApplicationListener<ApplicationReadyEvent> {
    private final ChoiceListImporter choiceImporter;
    private final StandardTemplateImporter templateImporter;
    private final TagDao tagDao;
    private final RegisterImporter registerImporter;
    private final OS2complianceConfiguration config;
    private final SettingsService settingsService;
    private final StandardTemplateSectionDao standardTemplateSectionDao;
    private final CatalogService catalogService;
    private final ChoiceValueDao valueDao;
    private final PlatformTransactionManager transactionManager;
    private final DPIATemplateSectionImporter dpiaTemplateSectionImporter;
	private final DPIAService dpiaService;
	private final ChoiceService choiceService;
	private final RegisterService registerService;

	@Value("classpath:data/registers/*.json")
    private Resource[] registers;

    @Override
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
        incrementAndPerformIfVersion(12, this::seedV12);
        incrementAndPerformIfVersion(13, this::seedV13);
        incrementAndPerformIfVersion(14, this::seedV14);
        incrementAndPerformIfVersion(15, this::seedV15);
        incrementAndPerformIfVersion(16, this::seedV16);
        incrementAndPerformIfVersion(17, this::seedV17);
        incrementAndPerformIfVersion(18, this::seedV18);
        incrementAndPerformIfVersion(19, this::seedV19);
        incrementAndPerformIfVersion(20, this::seedV20);
        incrementAndPerformIfVersion(21, this::seedV21);
        incrementAndPerformIfVersion(22, this::seedV22);
        incrementAndPerformIfVersion(23, this::seedV23);
        incrementAndPerformIfVersion(24, this::seedV24);
        incrementAndPerformIfVersion(25, this::seedV25);
        incrementAndPerformIfVersion(26, this::seedV26);
    }

    private void incrementAndPerformIfVersion(final int version, final Runnable applier) {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            final int currentVersion = settingsService.getInt(DATA_MIGRATION_VERSION_SETTING, 0);
            if (currentVersion == version) {
                applier.run();
                settingsService.setInt(DATA_MIGRATION_VERSION_SETTING, version + 1);
            }
            return 0;
        });
    }

	private void seedV26 () {
		for (ReportSetting setting : ReportSetting.values()) {
			settingsService.createSetting(setting.getKey(), "", "report", true);
		}
	}

	private void seedV25 () {
		// Update each of these specific lists to be editable
		Set<String> listIdentifiers = Set.of("dp-access-who-list", "dp-access-count-list", "dp-count-processing-list", "dp-categories-list","dp-person-categories-list",  "dp-person-storage-duration-list", "dp-receiver-list");
		for (String identifier : listIdentifiers) {
			Optional<ChoiceList> list = choiceService.findChoiceList(identifier);
			if (list.isEmpty()) {
				continue;
			}

			list.get().setCustomizable(true);
		}
		settingsService.createSetting(KitosConstants.KITOS_OWNER_ROLE_SETTING_INPUT_FIELD_NAME, "Systemejer" , "asset", true);
		settingsService.createSetting(KitosConstants.KITOS_RESPONSIBLE_ROLE_SETTING_INPUT_FIELD_NAME, "Systemansvarlig" , "asset", true);
		settingsService.createSetting(KitosConstants.KITOS_OPERATION_RESPONSIBLE_ROLE_SETTING_INPUT_FIELD_NAME, "Driftsansvarlig" , "asset", true);
	}

	private void seedV24 () {
		ChoiceList choiceList = choiceService.saveChoiceList(ChoiceList.builder()
						.identifier("record-of-processing-activity-regarding")
						.name("Fortegnelse over behandlingsaktivitet angående")
						.multiSelect(true)
						.customizable(true)
						.values(new ArrayList<>())
				.build());

		// Migrate data from existing column to choicelists
		registerService.findAll().stream()
				.filter(r -> r.getOldRegisterRegarding() != null && !r.getOldRegisterRegarding().isEmpty())
				.forEach(r -> {
					String oldValue = r.getOldRegisterRegarding();
					ChoiceValue oldValueChoice = ChoiceValue.builder()
							.caption(oldValue)
							.description(oldValue)
							.identifier(UUID.randomUUID().toString())
							.build();

					choiceList.getValues().add(oldValueChoice);
					r.setRegisterRegarding(Set.of(oldValueChoice));
				});
	}

	private void seedV23 () {
		settingsService.createSetting(RegisterSetting.CUSTOMRESPONSIBLEUSERFIELDNAME.getValue(), "Ansvarlig for udfyldelse" , "register", true);
	}

	private void seedV22() {
		dpiaService.findAll()
				.forEach(dpia -> {
					if (dpia.getDpiaScreening() != null) {
						dpia.getDpiaScreening().setConclusion(dpiaService.calculateScreeningConclusion(dpia.getDpiaScreening().getDpiaScreeningAnswers()));
						dpiaService.save(dpia);
					}
				});
	}

    private void seedV21() {
        settingsService.createSetting(NotificationSetting.SEVENDAYSBEFORE.getValue(), "true" , "notification", true);
        settingsService.createSetting(NotificationSetting.ONEDAYBEFORE.getValue(), "false" , "notification", true);
        settingsService.createSetting(NotificationSetting.ONDAY.getValue(), "false" , "notification", true);
        settingsService.createSetting(NotificationSetting.EVERYSEVENDAYSAFTER.getValue(), "false" , "notification", true);
    }

	@SneakyThrows
	private void seedV20() {

	}

    private void seedV19() {
        try {
            dpiaTemplateSectionImporter.importDPIATemplateSections("./data/dpia/dpia_template_sections.json");
            choiceImporter.importValues("./data/choices/dpia-quality-values.json");
            choiceImporter.importList("./data/choices/dpia-quality-list.json");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void seedV18() {
        settingsService.createSetting("inactiveResponsibleEmail","" , "general", true);
    }

    private void seedV15() {
        valueDao.findByIdentifier("register-gdpr-p6-f")
            .ifPresent(c -> c.setDescription("Behandling er nødvendig for, at den dataansvarlige eller en tredjemand kan forfølge en legitim interesse, medmindre den registreredes interesser eller grundlæggende rettigheder og frihedsrettigheder, der kræver beskyttelse af personoplysninger, går forud herfor, navnlig hvis den registrerede er et barn. <b>Første afsnit, litra f), gælder ikke for behandling, som offentlige myndigheder foretager som led i udførelsen af deres opgaver.</b>"));
    }

    private void seedV16() {
        // No longer needed
    }

    private void seedV17() {
		// No longer needed
    }

    private void seedV14() {
        // No longer needed
    }

    private void seedV13() {
        // No longer needed
    }

    private void seedV12() {
        // No longer needed
    }

    private void seedV11() {
        // No longer needed
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
        // No longer needed
    }

    private void seedV8() {
        // No longer needed
    }

    private void seedV7() {
        // All non iso 27002 should be selected by default
        standardTemplateSectionDao.findByIdentifierStartsWith("nsis").stream()
            .map(StandardTemplateSection::getStandardSection)
            .filter(Objects::nonNull)
            .forEach(s -> s.setSelected(true));
    }

    private void seedV6() {
        // No longer needed
    }

    private void seedV5() {
        // No longer needed
    }

    private void seedV4() {
        // No longer needed
    }

    private void seedV3() {
        // No longer needed
    }

    private void seedV2() {
        // No longer needed
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
