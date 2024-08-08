package dk.digitalidentity.report;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.StandardTemplateDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
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
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalog;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.InformationObligationStatus;
import dk.digitalidentity.model.entity.enums.InformationPassedOn;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThirdCountryTransfer;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.report.replacers.Article30Replacer;
import dk.digitalidentity.report.replacers.CommonPropertiesReplacer;
import dk.digitalidentity.report.replacers.ISO27001Replacer;
import dk.digitalidentity.report.replacers.ISO27002Replacer;
import dk.digitalidentity.report.replacers.ThreatAssessmentReplacer;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.ScaleService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.UserService;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;
import org.thymeleaf.TemplateEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.digitalidentity.report.DocxService.PARAM_RISK_ASSESSMENT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * Unit test for {@link DocxService}
 * And replacers...
 */
@SpringBootTest
@ContextConfiguration(classes = {DocxService.class, DocsReportGeneratorComponent.class,
    CommonPropertiesReplacer.class, Article30Replacer.class, ISO27001Replacer.class, ISO27002Replacer.class, DocsReportGeneratorComponent.class,
    ThreatAssessmentReplacer.class, ScaleService.class, ThreatAssessmentService.class, TemplateEngine.class})
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
    private AssetService assetServiceMock;
    @MockBean
    private RelationService relationServiceMock;
    @MockBean
    private RelationDao relationDaoMock;
    @MockBean
    private RegisterDao registerDaoMock;
    @MockBean
    private ThreatAssessmentDao threatAssessmentDaoMock;
    @MockBean
    private TaskService taskServiceMock;
    @SpyBean
    private ThreatAssessmentService threatAssessmentServiceMock;
    @MockBean
    private SettingsService settingsServiceMock;
    @MockBean
    private UserService userService;

    @BeforeEach
    public void setup() {
        mockStandards();
        mockThreatAssessment();
    }

    private void mockThreatAssessment() {
        doReturn(Optional.of(createDummyThreatAssessment())).when(threatAssessmentServiceMock).findById(any());
        doReturn(Optional.of(createDummyAssets().get(0))).when(assetServiceMock).get(any());
        doReturn("scale-1-4").when(settingsServiceMock).getString(eq("scale"), any());
        doReturn(List.of(createDummyTask(), createAsset(0))).when(relationServiceMock).findAllRelatedTo(any());
        doReturn(false).when(taskServiceMock).isTaskDone(any());
    }

    private void mockStandards() {
        // This giant wall of mock, makes sure all choices, assets and registers needed for generating a report is present
        // when the service needs them.
        doReturn(createDummyAssets()).when(assetServiceMock).findRelatedTo(any());
        final Register dummyRegister = createDummyRegister(2);
        dummyRegister.setDataProcessing(null);
        doReturn(Arrays.asList(createDummyRegister(1), dummyRegister, createDummyRegister(3)))
            .when(registerServiceMock).findAllOrdered();
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
            documentService.replacePlaceHolders(doc, Collections.emptyMap());
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
            documentService.replacePlaceHolders(doc, Collections.emptyMap());
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
            documentService.replacePlaceHolders(doc, Collections.emptyMap());
            final FileOutputStream out = new FileOutputStream("test-result-article30.docx");
            doc.write(out);
            out.close();
        }
    }

    @Test
    public void canReplaceThreatAssessment() throws IOException {
        try (final XWPFDocument doc = documentService.readDocument("reports/risk/main.docx")) {
            documentService.replacePlaceHolders(doc, Map.of(PARAM_RISK_ASSESSMENT_ID, "1"));
            final FileOutputStream out = new FileOutputStream("test-result-threat-assessment.docx");
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
        register.setResponsibleUsers(List.of(User.builder().name("Test Testtrup #" + num).build()));
        register.setResponsibleOus(List.of(OrganisationUnit.builder().name("Enhed #" + num).build()));
        register.setDepartments(List.of(OrganisationUnit.builder().name("Afdeling #" + num).build()));
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

    private Task createDummyTask() {
        final Task t = new Task();
        t.setId(3L);
        t.setTaskType(TaskType.TASK);
        t.setNextDeadline(LocalDate.now());
        t.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris convallis augue non lectus eleifend, eget sagittis nisl iaculis. Nam in mi at eros maximus mattis. Donec tempus congue diam eu pellentesque.");
        t.setName("Et opgave navn");
        t.setResponsibleUser(User.builder().name("En Bruger").build());
        t.setResponsibleOu(OrganisationUnit.builder().name("En afdeling").build());
        return t;
    }

    private static ThreatAssessment createDummyThreatAssessment() {
        final ThreatCatalogThreat tct1 = new ThreatCatalogThreat();
        final ThreatCatalogThreat tct2 = new ThreatCatalogThreat();
        final ThreatCatalog catalog = new ThreatCatalog();
        final ThreatAssessment assessment = new ThreatAssessment();
        assessment.setName("IT-system dummy");
        assessment.setAssessment(RiskAssessment.RED);
        assessment.setThreatAssessmentType(ThreatAssessmentType.ASSET);
        tct1.setThreatCatalog(catalog);
        tct1.setIdentifier("t1");
        tct1.setThreatType("En type");
        tct1.setDescription("Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit..");
        tct2.setThreatType("En anden type");
        tct2.setThreatCatalog(catalog);
        tct2.setIdentifier("t2");
        tct2.setDescription("There is no one who loves pain itself, who seeks after it and wants to have it, simply because it is pain..");
        catalog.setIdentifier("test");
        catalog.setThreats(Arrays.asList(tct1, tct2));
        assessment.setThreatCatalog(catalog);
        assessment.setThreatAssessmentResponses(
            List.of(createResponseWithResidual(tct1), createResponse(tct2))
        );
        assessment.setCustomThreats(Collections.emptyList());
        return assessment;
    }

    private static ThreatAssessmentResponse createResponseWithResidual(final ThreatCatalogThreat tct) {
        final ThreatAssessmentResponse response = new ThreatAssessmentResponse();
        response.setId(1L);
        response.setName(tct.getDescription());
        response.setMethod(ThreatMethod.MITIGER);
        response.setProbability(3);
        response.setResidualRiskProbability(2);
        response.setIntegrityOrganisation(3);
        response.setResidualRiskConsequence(1);
        response.setThreatCatalogThreat(tct);
        return response;
    }
    private static ThreatAssessmentResponse createResponse(final ThreatCatalogThreat tct) {
        final ThreatAssessmentResponse response = new ThreatAssessmentResponse();
        response.setId(2L);
        response.setName(tct.getDescription());
        response.setMethod(ThreatMethod.ACCEPT);
        response.setProbability(4);
        response.setIntegrityOrganisation(3);
        response.setThreatCatalogThreat(tct);
        return response;
    }

    private Asset createAsset(final int idx) {
        final Asset asset = new Asset();
        asset.setName("Asset name " + idx);
        asset.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.YES);
        final DataProcessing dataProcessing = new DataProcessing();
        asset.setDataProcessing(dataProcessing);
        asset.setCriticality(Criticality.CRITICAL);
        asset.setEmergencyPlanLink("https://aarhusbryghus.dk/");

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
