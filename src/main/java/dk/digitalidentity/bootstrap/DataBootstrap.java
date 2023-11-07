package dk.digitalidentity.bootstrap;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.service.ChoiceListImporter;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.importer.RegisterImporter;
import dk.digitalidentity.service.importer.StandardTemplateImporter;
import dk.digitalidentity.service.importer.ThreatCatalogImporter;
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

import static dk.digitalidentity.Constants.SEED_VERSION_SETTING;

@Order(100)
@Component
public class DataBootstrap implements ApplicationListener<ApplicationReadyEvent> {
    final ChoiceListImporter choiceImporter;
    final ThreatCatalogImporter threatCatalogImporter;
    final StandardTemplateImporter templateImporter;
    final TagDao tagDao;
    final RegisterImporter registerImporter;
    final OS2complianceConfiguration config;
    final SettingsService settingsService;

    @Value("classpath:data/registers/*.json")
    private Resource[] registers;

    public DataBootstrap(final ChoiceListImporter choiceImporter, final ThreatCatalogImporter threatCatalogImporter, final StandardTemplateImporter templateImporter, final TagDao tagDao, final RegisterImporter registerImporter, final OS2complianceConfiguration config, final SettingsService settingsService) {
        this.choiceImporter = choiceImporter;
        this.threatCatalogImporter = threatCatalogImporter;
        this.templateImporter = templateImporter;
        this.tagDao = tagDao;
        this.registerImporter = registerImporter;
        this.config = config;
        this.settingsService = settingsService;
    }

    @Override
    @Transactional
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        if (!config.isSeedData()) {
            return;
        }
        try {
            final int currentVersion = settingsService.getInt(SEED_VERSION_SETTING, 0);
            if (currentVersion == 0) {
                seedV0();
                settingsService.setInt(SEED_VERSION_SETTING, 1);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void seedV0() throws IOException {
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
