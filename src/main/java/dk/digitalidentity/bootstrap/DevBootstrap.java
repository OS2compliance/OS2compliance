package dk.digitalidentity.bootstrap;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.dao.ApiClientDao;
import dk.digitalidentity.dao.ContactDao;
import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.OrganisationUnitDao;
import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.StandardSectionDao;
import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.dao.ThreatCatalogDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.ApiClient;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.model.entity.Contact;
import dk.digitalidentity.model.entity.DataProcessing;
import dk.digitalidentity.model.entity.DataProcessingCategoriesRegistered;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Position;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.AssetStatus;

import dk.digitalidentity.model.entity.enums.Criticality;
import dk.digitalidentity.model.entity.enums.DataProcessingAgreementStatus;
import dk.digitalidentity.model.entity.enums.DeletionProcedure;
import dk.digitalidentity.model.entity.enums.DocumentRevisionInterval;
import dk.digitalidentity.model.entity.enums.DocumentStatus;
import dk.digitalidentity.model.entity.enums.DocumentType;
import dk.digitalidentity.model.entity.enums.InformationObligationStatus;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.SupplierStatus;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.SettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Order(200)
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Slf4j
@Component
public class DevBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private SupplierDao supplierDao;
    @Autowired
    private OS2complianceConfiguration config;
    @Autowired
    private TaskDao taskDao;
    @Autowired
    private ContactDao contactDao;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private OrganisationUnitDao organisationUnitDao;
    @Autowired
    private RegisterDao registerDao;
    @Autowired
    private AssetService assetService;
    @Autowired
    private ThreatAssessmentDao threatAssessmentDao;
    @Autowired
    private StandardSectionDao standardSectionDao;
    @Autowired
    private ThreatCatalogDao threatCatalogDao;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private ApiClientDao apiClientDao;
    @Autowired
    private ChoiceService choiceService;

    @Override
    @Transactional
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        if (config.isDevelopmentMode()) {
            if (supplierDao.findAll().isEmpty()) {
                ///////////////////////////////////
                // FK
                OrganisationUnit hjelmOu = new OrganisationUnit();
                hjelmOu.setActive(true);
                hjelmOu.setName("Hjelm Kommune");
                hjelmOu.setUuid(UUID.randomUUID().toString());
                hjelmOu = organisationUnitDao.save(hjelmOu);

                OrganisationUnit diOu = new OrganisationUnit();
                diOu.setUuid(UUID.randomUUID().toString());
                diOu.setActive(true);
                diOu.setParentUuid(hjelmOu.getUuid());
                diOu.setName("Digital Identity ApS");
                diOu = organisationUnitDao.save(diOu);

                OrganisationUnit nibisOu = new OrganisationUnit();
                nibisOu.setUuid(UUID.randomUUID().toString());
                nibisOu.setActive(true);
                nibisOu.setParentUuid(hjelmOu.getUuid());
                nibisOu.setName("NIBIS Konsulenterne ApS");
                nibisOu = organisationUnitDao.save(nibisOu);

                OrganisationUnit plejeOu = new OrganisationUnit();
                plejeOu.setUuid(UUID.randomUUID().toString());
                plejeOu.setActive(true);
                plejeOu.setParentUuid(hjelmOu.getUuid());
                plejeOu.setName("Hjemmeplejen");
                plejeOu = organisationUnitDao.save(plejeOu);

                OrganisationUnit itOu = new OrganisationUnit();
                itOu.setUuid(UUID.randomUUID().toString());
                itOu.setActive(true);
                itOu.setParentUuid(hjelmOu.getUuid());
                itOu.setName("It-administration");
                itOu = organisationUnitDao.save(itOu);

                OrganisationUnit gentofte = new OrganisationUnit();
                gentofte.setUuid(UUID.randomUUID().toString());
                gentofte.setActive(true);
                gentofte.setName("Gentofte Kommune");
                gentofte = organisationUnitDao.save(gentofte);

                OrganisationUnit svendborg = new OrganisationUnit();
                svendborg.setUuid(UUID.randomUUID().toString());
                svendborg.setActive(true);
                svendborg.setName("Svendborg Kommune");
                svendborg = organisationUnitDao.save(svendborg);

                OrganisationUnit ishoj = new OrganisationUnit();
                ishoj.setUuid(UUID.randomUUID().toString());
                ishoj.setActive(true);
                ishoj.setName("Ishøj Kommune");
                ishoj = organisationUnitDao.save(ishoj);

                User user1 = new User();
                user1.setActive(true);
                user1.setUserId("user1");
                user1.setName("Test User 1");
                user1.setEmail("user1@digital-identity.dk");
                user1.setRoles(Set.of(Roles.ADMINISTRATOR, Roles.SUPERUSER, Roles.USER));
                user1.setPositions(Set.of(
                    Position.builder()
                        .name("Tester")
                        .ouUuid(plejeOu.getUuid())
                        .build(),
                    Position.builder()
                        .name("Supporter")
                        .ouUuid(itOu.getUuid())
                        .build()
                ));
                user1 = userDao.save(user1);

                ///////////////////////////////////
                // Suppliers
                Supplier supplier1 = new Supplier();
                supplier1.setName("NIBIS Konsulenterne");
                supplier1.setVersion(1);
                supplier1.setCity("Holstebro");
                supplier1.setCountry("Danmark");
                supplier1.setZip("7500");
                supplier1.setStatus(SupplierStatus.READY);
                supplier1.getProperties().add(Property.builder()
                    .key("prop1")
                    .value("val")
                    .entity(supplier1)
                    .build());
                supplier1.setCreatedBy("");
                supplier1 = supplierDao.save(supplier1);

                Supplier supplier2 = new Supplier();
                supplier2.setName("Digital Identity");
                supplier2.setVersion(1);
                supplier2.setUpdatedAt(LocalDateTime.now().plusMinutes(5));
                supplier2.setStatus(SupplierStatus.READY);
                supplier2 = supplierDao.save(supplier2);
                supplier1.setCreatedBy("");

                Supplier supplier3 = new Supplier();
                supplier3.setName("456 IT");
                supplier3.setVersion(1);
                supplier3.setCreatedBy("");
                supplier3.setStatus(SupplierStatus.IN_PROGRESS);
                supplier3 = supplierDao.save(supplier3);

                Contact contact1 = new Contact();
                contact1.setRole("Test");
                contact1.setPhone("11 22 33 44");
                contact1.setMail("hjælp_det_brænder@devnull");
                contact1.setName("Anders And");
                contact1 = contactDao.save(contact1);

                final Relation sup1con1 = new Relation();
                sup1con1.setRelationAId(supplier1.getId());
                sup1con1.setRelationAType(RelationType.SUPPLIER);
                sup1con1.setRelationBId(contact1.getId());
                sup1con1.setRelationBType(RelationType.CONTACT);
                relationDao.save(sup1con1);

                ///////////////////////////////////
                // Documents

                Document doc1 = new Document();
                doc1.setDescription("Et langt og fint document");
                doc1.setLink("https://google.dk");
                doc1.setName("google.docx");
                doc1.setDocumentType(DocumentType.OTHER);
                doc1.setStatus(DocumentStatus.NOT_STARTED);
                doc1.setRevisionInterval(DocumentRevisionInterval.EVERY_SECOND_YEAR);
                doc1.setNextRevision(LocalDate.now());
                doc1.setResponsibleUser(user1);
                doc1 = documentDao.save(doc1);

                Document doc2 = new Document();
                doc2.setDescription("Noget tekst");
                doc2.setLink("https://tv2.dk");
                doc2.setName("læsmig.docx");
                doc2.setDocumentType(DocumentType.GUIDE);
                doc2.setStatus(DocumentStatus.READY);
                doc2.setRevisionInterval(DocumentRevisionInterval.NONE);
                doc2.setResponsibleUser(user1);
                doc2 = documentDao.save(doc2);


                ///////////////////////////////////
                // Tasks

                final Task t1 = new Task();
                t1.setDescription("Regndans udføres jævnligt");
                t1.setRepetition(TaskRepetition.MONTHLY);
                t1.setNextDeadline(LocalDate.now().plusDays(10));
                t1.setTaskType(TaskType.TASK);
                t1.setResponsibleOu(nibisOu);
                t1.setResponsibleUser(user1);
                t1.setNotifyResponsible(false);
                t1.setName("Regndans");
                t1.setIncludeInReport(false);
                taskDao.save(t1);

                final Task t2 = new Task();
                t2.setDescription("Medbring kage ofte");
                t2.setRepetition(TaskRepetition.MONTHLY);
                t2.setNextDeadline(LocalDate.now().plusDays(1));
                t2.setTaskType(TaskType.TASK);
                t2.setResponsibleOu(diOu);
                t2.setResponsibleUser(user1);
                t2.setName("Kageordning");
                t2.setNotifyResponsible(false);
                t2.setIncludeInReport(false);
                taskDao.save(t2);

                final Task t3 = new Task();
                t3.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam maximus nisl in vestibulum consequat");
                t3.setRepetition(TaskRepetition.HALF_YEARLY);
                t3.setNextDeadline(LocalDate.now().plusDays(3));
                t3.setTaskType(TaskType.CHECK);
                t3.setName("Opdater dokument");
                t3.setResponsibleOu(hjelmOu);
                t3.setResponsibleUser(user1);
                t3.setNotifyResponsible(false);
                t3.setIncludeInReport(false);
                taskDao.save(t3);

                final Relation taskDoc = new Relation();
                taskDoc.setRelationAId(t2.getId());
                taskDoc.setRelationAType(RelationType.TASK);
                taskDoc.setRelationBId(doc2.getId());
                taskDoc.setRelationBType(RelationType.DOCUMENT);
                relationDao.save(taskDoc);

                ///////////////////////////////////
                // Register

                final Register r1 = new Register();
                r1.setName("1. Behandling af personoplysninger i forbindelse med ydelser vedr.  jobafklaring, jobsøgning og uddannelsesforløb for ledige");
                r1.setPurpose("Behandling af personoplysninger sker med henblik på at hjælpe ledige borgere i uddannelse eller job, hjælpe sygemeldte borgere tilbage på arbejdsmarkedet samt godkendelse af arbejdsmiljø mv. på private erhvervirksomheder, der beskæftiger ledige midlertidigt.");
                r1.setInformationObligation(InformationObligationStatus.NO);
                r1.setResponsibleOus(List.of(hjelmOu));
                r1.setGdprChoices(Set.of("register-gdpr-valp10", "register-gdpr-valp11", "register-gdpr-valp7", "register-gdpr-p7-f", "register-gdpr-valp6", "register-gdpr-p6-e"));
                r1.setCreatedBy("");
                r1.setDataProcessing(new DataProcessing());
                r1.setStatus(RegisterStatus.READY);
                registerDao.save(r1);

                final Register r2 = new Register();
                r2.setName("2. Behandling af personoplysninger i forbindelse med fleksjob, løntillæg, jobrotation, virksomhedspraktik, mentorordning og voksenlærling, ressourceforløb og revalidering");
                r2.setPurpose("Behandling af personoplysninger sker med henblik på at vurdere og følge op på sager i forbindelse med fleksjob, løntillæg, jobrotation, virksomhedspraktik, mentorordning og voksenlærling samt vurdere og følge op på sager om revalidering og ressourceforløb, herunder udarbejdelse af jobplaner og aktiviteter");
                r2.setInformationObligation(InformationObligationStatus.NO);
                r2.setResponsibleOus(List.of(hjelmOu));
                r2.setGdprChoices(Set.of("register-gdpr-valp10", "register-gdpr-valp11", "register-gdpr-valp7", "register-gdpr-p7-f", "register-gdpr-valp6", "register-gdpr-p6-e"));
                r2.setCreatedBy("");
                r2.setDataProcessing(new DataProcessing());
                r2.setStatus(RegisterStatus.IN_PROGRESS);
                registerDao.save(r2);

                final Register r3 = new Register();
                r3.setName("3. Behandling af personoplysninger i forbindelse med dagpenge, efterløn/feriedagpenge og seniorjob");
                r3.setPurpose("Behandling af personoplysninger sker med henblik på at vurdere og følge op på sager i forbindelse med dagpenge, efterløn, feriedagpenge og seniorjob, herunder dagpenge i forbindelse med sygdom, barsel og pasning af alvorligt syge børn");
                r3.setInformationObligation(InformationObligationStatus.YES);
                r3.setResponsibleOus(List.of(hjelmOu));
                r3.setCreatedBy("");
                r3.setResponsibleUsers(List.of(user1));
                r3.setGdprChoices(Set.of("register-gdpr-valp10", "register-gdpr-valp11", "register-gdpr-valp7", "register-gdpr-p7-a", "register-gdpr-p7-f", "register-gdpr-valp6", "register-gdpr-p6-a", "register-gdpr-p6-e"));
                r3.setDataProcessing(new DataProcessing());
                r3.setStatus(RegisterStatus.NOT_STARTED);
                registerDao.save(r3);


                final Relation regDoc = new Relation();
                regDoc.setRelationAId(r1.getId());
                regDoc.setRelationAType(RelationType.REGISTER);
                regDoc.setRelationBId(doc2.getId());
                regDoc.setRelationBType(RelationType.DOCUMENT);
                relationDao.save(regDoc);
                final Relation regTask = new Relation();
                regTask.setRelationAId(r1.getId());
                regTask.setRelationAType(RelationType.REGISTER);
                regTask.setRelationBId(t1.getId());
                regTask.setRelationBType(RelationType.TASK);
                relationDao.save(regTask);


                ///////////////////////////////////
                // Assets
                final DataProcessing dataProcessing = new DataProcessing();
                dataProcessing.setDeletionProcedure(DeletionProcedure.YES);
                dataProcessing.setDeletionProcedureLink("https://WeDeleteEverythinAllTheTime.nu");
                dataProcessing.setAccessWhoIdentifiers(Set.of("dp-access-who-leaders", "dp-access-who-ext"));
                dataProcessing.setAccessCountIdentifier("dp-access-count-1-10");
                dataProcessing.setStorageTimeIdentifier("dp-storage-duration-1mth");
                dataProcessing.getRegisteredCategories().add(DataProcessingCategoriesRegistered.builder()
                    .personCategoriesRegisteredIdentifier("dp-categories-registered-vuln-children")
                    .dataProcessing(dataProcessing)
                    .personCategoriesInformationIdentifiers(Set.of("dp-person-categories-regular", "dp-person-categories-sensitive", "dp-person-categories-sensitive-religion", "dp-person-categories-sensitive-union"))
                    .build()
                );

                dataProcessing.getRegisteredCategories().add(DataProcessingCategoriesRegistered.builder()
                    .dataProcessing(dataProcessing)
                    .personCategoriesRegisteredIdentifier("dp-categories-registered-employees")
                    .personCategoriesInformationIdentifiers(Set.of("dp-person-categories-regular", "dp-person-categories-sensitive", "dp-person-categories-sensitive-religion", "dp-person-categories-sensitive-union"))
                    .build()
                );
                dataProcessing.setPersonCountIdentifier("dp-person-cnt-10-100");

                ChoiceList assetTypeList = choiceService.getAssetTypeChoiceList();
                ChoiceValue itsystemAssetType = assetTypeList.getValues().stream().filter(value -> value.getCaption().equalsIgnoreCase("it-system")).findAny().get();

                Asset os2Compliance = new Asset();
                os2Compliance.setSupplier(supplier2);
                os2Compliance.setAssetType(itsystemAssetType);
                os2Compliance.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.ON_GOING);
                os2Compliance.setDescription("En beskrivelse");
                os2Compliance.setResponsibleUsers(List.of(user1));
                os2Compliance.setName("OS2compliance");
                os2Compliance.setAssetStatus(AssetStatus.NOT_STARTED);
                os2Compliance.setCriticality(Criticality.CRITICAL);
                os2Compliance.setEmergencyPlanLink("https://google.com");
                os2Compliance.setProductLink("https://os2compliance.dk");
                os2Compliance.setReEstablishmentPlanLink("https://os2compliance.dk/reboot");
                os2Compliance.setDataProcessing(dataProcessing);

                os2Compliance = assetService.create(os2Compliance);

                final Relation regAsset = new Relation();
                regAsset.setRelationAId(r1.getId());
                regAsset.setRelationAType(RelationType.REGISTER);
                regAsset.setRelationBId(os2Compliance.getId());
                regAsset.setRelationBType(RelationType.ASSET);
                relationDao.save(regAsset);

                Asset asset1 = new Asset();
                asset1.setSupplier(supplier2);
                asset1.setAssetType(itsystemAssetType);
                asset1.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.NOT_RELEVANT);
                asset1.setDescription("En beskrivelse");
                asset1.setResponsibleUsers(List.of(user1));
                asset1.setName("Asset 1");
                asset1.setAssetStatus(AssetStatus.NOT_STARTED);
                asset1.setCriticality(Criticality.CRITICAL);
                asset1.setEmergencyPlanLink("https://google.com");
                asset1.setProductLink("https://os2compliance.dk");
                asset1.setReEstablishmentPlanLink("https://os2compliance.dk/reboot");
                asset1.setDataProcessing(new DataProcessing());
                asset1 = assetService.create(asset1);

                final Relation taskAsset = new Relation();
                taskAsset.setRelationAId(t2.getId());
                taskAsset.setRelationAType(RelationType.TASK);
                taskAsset.setRelationBId(asset1.getId());
                taskAsset.setRelationBType(RelationType.ASSET);
                relationDao.save(taskAsset);

                final Asset asset2 = new Asset();
                asset2.setSupplier(supplier2);
                asset2.setAssetType(itsystemAssetType);
                asset2.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.YES);
                asset2.setDescription("En beskrivelse");
                asset2.setResponsibleUsers(List.of(user1));
                asset2.setName("Asset 2");
                asset2.setAssetStatus(AssetStatus.ON_GOING);
                asset2.setCriticality(Criticality.CRITICAL);
                asset2.setEmergencyPlanLink("https://google.com");
                asset2.setProductLink("https://os2compliance.dk");
                asset2.setReEstablishmentPlanLink("https://os2compliance.dk/reboot");
                asset2.setDataProcessing(new DataProcessing());
                assetService.create(asset2);

                final Asset asset3 = new Asset();
                asset3.setSupplier(supplier2);
                asset3.setAssetType(itsystemAssetType);
                asset3.setDataProcessingAgreementStatus(DataProcessingAgreementStatus.YES);
                asset3.setDescription("En beskrivelse");
                asset3.setResponsibleUsers(List.of(user1));
                asset3.setName("Asset 3");
                asset3.setAssetStatus(AssetStatus.READY);
                asset3.setCriticality(Criticality.NON_CRITICAL);
                asset3.setEmergencyPlanLink("https://google.com");
                asset3.setProductLink("https://os2compliance.dk");
                asset3.setReEstablishmentPlanLink("https://os2compliance.dk/reboot");
                asset3.setDataProcessing(new DataProcessing());
                assetService.create(asset3);

                ///////////////////////////////////
                // Other relations
                final StandardSection section = standardSectionDao.findSectionsForStandardTemplate("iso27001").get(1);
                final Relation sectionRelation = new Relation();
                sectionRelation.setRelationAId(section.getId());
                sectionRelation.setRelationAType(RelationType.STANDARD_SECTION);
                sectionRelation.setRelationBId(doc2.getId());
                sectionRelation.setRelationBType(RelationType.DOCUMENT);
                relationDao.save(sectionRelation);

                ///////////////////////////////////
                // Settings
                settingsService.createSetting("TestSettingString", "En stor værdi");
                settingsService.createSetting("TestSettingString2", "En større værdi");
                settingsService.createSetting("TekstPåEngelsk", "Text");

                settingsService.createSetting("TestSettingInt", 200);
                settingsService.createSetting("TestSettingIntWithStringVal", "400");
                //risk scale
                settingsService.createSetting("scale", "", "risk", true);

                ///////////////////////////////////
                // api clients
                ApiClient client = new ApiClient();
                client.setApplicationIdentifier("dev");
                client.setName("Development");
                client.setApiKey("6f856f0a-fc37-407e-bbe6-783c541b0261");
                client = apiClientDao.save(client);
            }
        }
    }
}
