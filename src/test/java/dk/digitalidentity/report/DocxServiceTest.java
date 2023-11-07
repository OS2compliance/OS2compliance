package dk.digitalidentity.report;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetSupplierMapping;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.StandardTemplate;
import dk.digitalidentity.model.entity.StandardTemplateSection;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.InformationObligationStatus;
import dk.digitalidentity.model.entity.enums.InformationPassedOn;
import dk.digitalidentity.model.entity.enums.ThirdCountryTransfer;
import dk.digitalidentity.report.replacers.Article30Replacer;
import dk.digitalidentity.report.replacers.CommonPropertiesReplacer;
import dk.digitalidentity.report.replacers.ISO27001Replacer;
import dk.digitalidentity.report.replacers.ISO27002Replacer;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.RelationService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@ContextConfiguration(classes = {DocxService.class, DocsReportGeneratorComponent.class,
    CommonPropertiesReplacer.class, Article30Replacer.class, ISO27001Replacer.class, ISO27002Replacer.class, DocsReportGeneratorComponent.class})
@EnableConfigurationProperties(value = OS2complianceConfiguration.class)
@TestPropertySource("/application-test.properties")
@ActiveProfiles("test")
public class DocxServiceTest {
    @Autowired
    private DocxService documentService;
    @MockBean
    private RegisterService registerServiceMock;
    @MockBean
    private ChoiceService choiceServiceMock;
    @MockBean
    private StandardTemplateDao standardTemplateDaoMock;
    @MockBean
    private AssetService assetService;
    @MockBean
    private RelationService relationServiceMock;

    @BeforeEach
    public void setup() {
        // This giant wall of mock, makes sure all choices, assets and registers needed for generating a report is present
        // when the service needs them.
        doReturn(createDummyAssets()).when(assetService).findRelatedTo(any());
        doReturn(Arrays.asList(createDummyRegister(1), createDummyRegister(2)))
            .when(registerServiceMock).findAll();
        doReturn(Optional.of(ChoiceValue.builder().caption("10-100").build())).when(choiceServiceMock).getValue("dp-access-count-10-100");
        doReturn(Optional.of(ChoiceValue.builder().caption("100-1.000").build())).when(choiceServiceMock).getValue("dp-person-cnt-100-1000");
        doReturn(Optional.of(ChoiceValue.builder().caption("12 måneder efter modtagelse").build())).when(choiceServiceMock).getValue("dp-storage-duration-12mth");
        doReturn(Optional.of(ChoiceValue.builder().caption("Borgere over 18 år").build())).when(choiceServiceMock).getValue("dp-categories-registered-adult");
        doReturn(Optional.of(ChoiceValue.builder().caption("Ansatte").build())).when(choiceServiceMock).getValue("dp-categories-registered-employees");
        doReturn(Optional.of(ChoiceValue.builder().caption("Følsomme personoplysninger").build())).when(choiceServiceMock).getValue("dp-person-categories-sensitive");
        doReturn(Optional.of(ChoiceValue.builder().caption("Oplysninger om Straffedomme eller lovovertrædelser").build())).when(choiceServiceMock).getValue("dp-person-categories-offences");
        doReturn(Optional.of(ChoiceValue.builder().caption("Fortrolige personoplysninger (CPR nr.)").build())).when(choiceServiceMock).getValue("dp-person-categories-classified");
        doReturn(Optional.of(ChoiceValue.builder().caption("Etnisk oprindelse").build())).when(choiceServiceMock).getValue("dp-person-categories-sensitive-etnicity");
        doReturn(Optional.of(ChoiceValue.builder().caption("Religiøs eller filosofisk overbevisning").build())).when(choiceServiceMock).getValue("dp-person-categories-sensitive-religion");
        doReturn(Optional.of(ChoiceValue.builder().caption("Fagforeningsmæssige tilhørsforhold").build())).when(choiceServiceMock).getValue("dp-person-categories-sensitive-union");
        doReturn(Optional.of(ChoiceValue.builder().caption("Almindelige personoplysninger").build())).when(choiceServiceMock).getValue("dp-person-categories-regular");
        doReturn(Optional.of(ChoiceValue.builder().caption("Advokater").build())).when(choiceServiceMock).getValue("dp-receiver-lawyers");
        doReturn(Optional.of(ChoiceValue.builder().caption("Borger").build())).when(choiceServiceMock).getValue("dp-receiver-citizens");
        doReturn(Optional.of(createGdprChoices())).when(choiceServiceMock).findChoiceList("register-gdpr");
        doReturn(Optional.of(createGdprP6Choices())).when(choiceServiceMock).findChoiceList("register-gdpr-p6");
        doReturn(Optional.of(createGdprP7Choices())).when(choiceServiceMock).findChoiceList("register-gdpr-p7");
        doReturn(Optional.of(createAccessWhoChoices())).when(choiceServiceMock).findChoiceList("dp-access-who-list");

        doReturn(StandardTemplate.builder()
            .identifier("iso27001")
            .name("ISO27001")
            .standardTemplateSections(Collections.singletonList(buildISO27001section()))
            .build())
            .when(standardTemplateDaoMock).findByIdentifier("iso27001");

        doReturn(StandardTemplate.builder()
            .identifier("iso27002_2022")
            .name("ISO27002-2022")
            .standardTemplateSections(Collections.singletonList(buildISO27002section()))
            .build())
            .when(standardTemplateDaoMock).findByIdentifier("iso27002_2022");
    }

    /**
     * Check that we can generate an ISO27001 report.
     * No assertions are actually performed, so it just tests the code actually runs without exceptions.
     * To check the output open the Word document created in the working directory
     */
    @Test
    public void canReplaceISO27001() throws IOException {
        try (final XWPFDocument doc = documentService.readDocument("reports/ISO27001/ISO27001.docx")) {
            documentService.replacePlaceHolders(doc);
            final FileOutputStream out = new FileOutputStream("test-result-iso27001.docx");
            doc.write(out);
            out.close();
        }
    }

    /**
     * Check that we can generate an ISO27002 report.
     * No assertions are actually performed, so it just tests the code actually runs without exceptions.
     * To check the output open the Word document created in the working directory
     */
    @Test
    public void canReplaceISO27002() throws IOException {
        try (final XWPFDocument doc = documentService.readDocument("reports/ISO27002/ISO27002.docx")) {
            documentService.replacePlaceHolders(doc);
            final FileOutputStream out = new FileOutputStream("test-result-iso27002.docx");
            doc.write(out);
            out.close();
        }
    }

    /**
     * Check that we can generate an Article 30 report.
     * No assertions are actually performed, so it just tests the code actually runs without exceptions.
     * To check the output open the Word document created in the working directory
     */
    @Test
    public void canReplaceArticle30() throws IOException {
        try (final XWPFDocument doc = documentService.readDocument("reports/article30/main.docx")) {
            documentService.replacePlaceHolders(doc);
            final FileOutputStream out = new FileOutputStream("test-result-article30.docx");
            doc.write(out);
            out.close();
        }
    }


    @SneakyThrows
    private static StandardTemplateSection buildISO27002section() {
        final File file = ResourceUtils.getFile("classpath:fixtures/test_ISO27001_section_description.html");
        final String description = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        final StandardTemplateSection section1 = createStandardTemplateSection(description, "4.1 Forståelse af organisationen og dens kontekst");
        final StandardTemplateSection section2 = createStandardTemplateSection(description, "4.2 Forståelse af interessenters behov og forventninger");
        return StandardTemplateSection.builder()
            .section("4")
            .description("Organisationens kontekst")
            .children(Set.of(section1, section2))
            .build();
    }

    @NotNull
    private static StandardTemplateSection createStandardTemplateSection(final String description, final String name) {
        final StandardSection section = StandardSection.builder()
            .reason("some note")
            .selected(true)
            .description(description)
            .build();
        final StandardTemplateSection standardTemplateSection = StandardTemplateSection.builder().section("4.1").description("Forståelse af organisationen og dens kontekst")
            .standardSection(section)
            .build();
        section.setTemplateSection(standardTemplateSection);
        section.setName(name);
        return standardTemplateSection;
    }

    @SneakyThrows
    private static StandardTemplateSection buildISO27001section() {
        final File file = ResourceUtils.getFile("classpath:fixtures/test_ISO27001_section_description.html");
        final String description = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return StandardTemplateSection.builder()
            .section("4")
            .description("Organisationens kontekst")
            .children(Collections
                .singleton(
                    StandardTemplateSection.builder()
                        .section("4.1")
                        .description("Forståelse af organisationen og dens kontekst")
                        .standardSection(
                            StandardSection.builder()
                                .description(description)
                                .build()
                        )
                        .build()
                ))
            .build();
    }


    private Register createDummyRegister(final int num) {
        final Register register = new Register();
        register.setName("Behandlingsaktivitet #" + num);
        register.setResponsibleUser(User.builder().name("Test Testtrup #" + num).build());
        register.setResponsibleOu(OrganisationUnit.builder().name("Enhed #" + num).build());
        register.setDepartment(OrganisationUnit.builder().name("Afdeling #" + num).build());
        register.setInformationResponsible("Kommunen er dataansvarlig for behandlingen af personoplysningerne");
        register.setRegisterRegarding("Plappe");
        register.setPurpose("Behandling af personoplysninger sker med henblik på at hjælpe ledige borgere i uddannelse eller job, hjælpe sygemeldte borgere tilbage på arbejdsmarkedet samt godkendelse af arbejdsmiljø mv. på private erhvervsvirksomheder, der beskæftiger ledige midlertidigt.");
        register.setInformationObligation(InformationObligationStatus.YES);
        register.setInformationObligationDesc("En beskrivelse her");
        register.setGdprChoices(Set.of("register-gdpr-valp6","register-gdpr-valp10","register-gdpr-valp11",
            "register-gdpr-valp7","register-gdpr-p7-f","register-gdpr-p6-e"));
        final DataProcessing dataProcessing = new DataProcessing();
        dataProcessing.setAccessWhoIdentifiers(Set.of("dp-access-who-empl","dp-access-who-ext","dp-access-who-citizens"));
        dataProcessing.setAccessCountIdentifier("dp-access-count-10-100");
        dataProcessing.setPersonCountIdentifier("dp-person-cnt-100-1000");
        dataProcessing.setStorageTimeIdentifier("dp-storage-duration-12mth");
        dataProcessing.setElaboration("En uddybning her");
        dataProcessing.setRegisteredCategories(createRegisteredCategories());
        register.setDataProcessing(dataProcessing);
        return register;
    }

    private List<DataProcessingCategoriesRegistered> createRegisteredCategories() {
        return List.of(
            DataProcessingCategoriesRegistered.builder()
                .personCategoriesInformationIdentifiers(Set.of("dp-person-categories-sensitive","dp-person-categories-offences","dp-person-categories-classified","dp-person-categories-sensitive-etnicity"))
                .personCategoriesRegisteredIdentifier("dp-categories-registered-adult")
                .informationPassedOn(InformationPassedOn.YES)
                .informationReceivers(Set.of("dp-receiver-lawyers","dp-receiver-citizens"))
                .build(),
            DataProcessingCategoriesRegistered.builder()
                .personCategoriesInformationIdentifiers(Set.of("dp-person-categories-sensitive","dp-person-categories-sensitive-religion","dp-person-categories-sensitive-union","dp-person-categories-regular"))
                .personCategoriesRegisteredIdentifier("dp-categories-registered-employees")
                .informationPassedOn(InformationPassedOn.NO)
                .build()
        );
    }

    private ChoiceList createGdprChoices() {
        final ChoiceList choiceList = new ChoiceList();
        final List<ChoiceValue> values = new ArrayList<>();
        values.add(ChoiceValue.builder().identifier("register-gdpr-valp6").caption("§6").description("Behandling af personoplysninger efter Artikel 6 stk. 1").build());
        values.add(ChoiceValue.builder().identifier("register-gdpr-valp7").caption("§7").description("Behandling af personoplysninger efter Artikel 9 stk. 2 (særlige kategorier af personoplysninger)").build());
        values.add(ChoiceValue.builder().identifier("register-gdpr-valp8").caption("§8").description("Nødvendig behandling af strafbare forhold").build());
        values.add(ChoiceValue.builder().identifier("register-gdpr-valp10").caption("§10").description("Retsinformationssystemer af væsentlige samfundsmæssig betydning").build());
        values.add(ChoiceValue.builder().identifier("register-gdpr-valp11").caption("§11").description("Myndigheders behandling af personnummer").build());
        choiceList.setValues(values);
        return choiceList;
    }

    private ChoiceList createGdprP6Choices() {
        final ChoiceList choiceList = new ChoiceList();
        final List<ChoiceValue> values = new ArrayList<>();
        values.add(ChoiceValue.builder().identifier("register-gdpr-p6-e").caption("e").description("Behandling er nødvendig af hensyn til udførelse af en opgave i samfundets interesse eller som henhører under offentlig myndighedsudøvelse, som den dataansvarlige har fået pålagt.").build());
        choiceList.setValues(values);
        return choiceList;
    }

    private ChoiceList createGdprP7Choices() {
        final ChoiceList choiceList = new ChoiceList();
        final List<ChoiceValue> values = new ArrayList<>();
        values.add(ChoiceValue.builder().identifier("register-gdpr-p7-f").caption("f").description("Behandling er nødvendig af hensyn til en opgave i samfundets interesse eller offentlig myndighedsudøvelse").build());
        choiceList.setValues(values);
        return choiceList;
    }

    private ChoiceList createAccessWhoChoices() {
        final ChoiceList choiceList = new ChoiceList();
        final List<ChoiceValue> values = new ArrayList<>();
        values.add(ChoiceValue.builder().identifier("dp-access-who-empl").caption("Medarbejdere").build());
        values.add(ChoiceValue.builder().identifier("dp-access-who-ext").caption("Eksterne konsulenter/leverandører").build());
        values.add(ChoiceValue.builder().identifier("dp-access-who-citizens").caption("Borgere").build());
        choiceList.setValues(values);
        return choiceList;
    }

    private List<Asset> createDummyAssets() {
        return IntStream.range(1, 4).mapToObj(this::createAsset).collect(Collectors.toList());
    }

    private Asset createAsset(final int idx) {
        final Asset asset = new Asset();
        asset.setName("Asset name " + idx);
        asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.YES);
        final DataProcessing dataProcessing = new DataProcessing();
        asset.setDataProcessing(dataProcessing);

        final Supplier supplier = new Supplier();
        supplier.setName("Supplier " + idx);
        supplier.setCountry("Danmark");
        asset.setSupplier(supplier);

        asset.setSuppliers(Collections.singletonList(AssetSupplierMapping.builder()
            .acceptanceBasis("Plappe")
            .thirdCountryTransfer(ThirdCountryTransfer.UNDER_CLARIFICATION)
            .supplier(supplier)
            .asset(asset)
            .build()));
        return asset;
    }


}
