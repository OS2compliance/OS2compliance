package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.AssetDao;
import dk.digitalidentity.dao.RegisterDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.TaskDao;
import dk.digitalidentity.dao.ThreatAssessmentDao;
import dk.digitalidentity.dao.ThreatCatalogDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.CustomThreat;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Relation;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.ThreatAssessmentResponse;
import dk.digitalidentity.model.entity.ThreatCatalogThreat;
import dk.digitalidentity.model.entity.enums.DocumentType;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentType;
import dk.digitalidentity.model.entity.enums.ThreatMethod;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.MailService;
import dk.digitalidentity.service.RelationService;
import dk.digitalidentity.service.RiskService;
import dk.digitalidentity.service.ScaleService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.model.RiskDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("risks")
@RequireUser
public class RiskController {
    @Autowired
    private ThreatCatalogDao threatCatalogDao;
    @Autowired
    private ThreatAssessmentDao threatAssessmentDao;
    @Autowired
    private TaskDao taskDao;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private MailService mailService;
    @Autowired
    private Environment environment;
    @Autowired
    private AssetDao assetDao;
    @Autowired
    private RegisterDao registerDao;
    @Autowired
    private RiskService riskService;
    @Autowired
    private ScaleService scaleService;
    @Autowired
    private RelationService relationService;
    @Autowired
    private TaskService taskService;

    @GetMapping
    public String riskList(final Model model) {
        model.addAttribute("risk", new ThreatAssessment());
        model.addAttribute("threatCatalogs", threatCatalogDao.findAll());
        return "risks/index";
    }

    @Transactional
    @PostMapping("create")
    public String formCreate(@Valid @ModelAttribute final ThreatAssessment threatAssessment,
            @RequestParam(name = "sendEmail", required = false) final boolean sendEmail,
            @RequestParam(name = "selectedRegister", required = false) final Long selectedRegister,
            @RequestParam(name = "selectedAssets", required = false) final Set<Long> selectedAsset) {

        if (!threatAssessment.isRegistered() && !threatAssessment.isOrganisation()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges minimum en af de to vurderinger.");
        }

        if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET)) {
            if (selectedAsset == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges et aktiv, når typen aktiv er valgt.");
            }
        }
        else if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.REGISTER)) {
            if (selectedRegister == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en behandlingsaktivitet, når typen behandlingsaktivitet er valgt.");
            }
        }

        if (threatAssessment.getThreatAssessmentResponses() == null) {
            threatAssessment.setThreatAssessmentResponses(new ArrayList<>());
        }

        final ThreatAssessment savedThreatAssessment = threatAssessmentDao.save(threatAssessment);

        if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET)) {
            final List<Asset> relatedAssets = assetDao.findAllById(selectedAsset);
            relatedAssets.forEach(asset -> createRelation(savedThreatAssessment.getId(), RelationType.ASSET, asset.getId()));
            if (savedThreatAssessment.isInherit()) {
                inheritRisk(savedThreatAssessment, relatedAssets);
            }
        }
        else if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.REGISTER)) {
            final Register register = registerDao.findById(selectedRegister).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der skal vælges en behandlingsaktivitet, når typen behandlingsaktivitet er valgt."));
            createRelation(savedThreatAssessment.getId(), RelationType.REGISTER, register.getId());
        }

        if (sendEmail && savedThreatAssessment.getResponsibleUser() != null) {
            Task task = new Task();
            task.setName("Udfyld risikovurdering: " + savedThreatAssessment.getName());
            task.setTaskType(TaskType.TASK);
            task.setResponsibleUser(savedThreatAssessment.getResponsibleUser());
            task.setNextDeadline(LocalDate.now().plusMonths(1));
            task.setRepetition(TaskRepetition.NONE);
            task = taskDao.save(task);

            createRelation(savedThreatAssessment.getId(), RelationType.TASK, task.getId());

            if (!StringUtils.isEmpty(task.getResponsibleUser().getEmail())) {
                final String message = getMessage(task);
                mailService.sendMessage(task.getResponsibleUser().getEmail(), "Påmindelse om at udfylde risikovurdering", message);
            }
        }

        riskService.setThreatAssessmentColor(savedThreatAssessment);

        return "redirect:/risks/" + savedThreatAssessment.getId();
    }

    @GetMapping("{id}")
    public String risk(final Model model, @PathVariable final long id) {
        final ThreatAssessment threatAssessment = threatAssessmentDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("risk", threatAssessment);
        model.addAttribute("threats", riskService.buildThreatList(threatAssessment));
        final Relatable relatable = findElement(threatAssessment);
        model.addAttribute("elementName", relatable == null ? null : relatable.getName());
        model.addAttribute("customThreat", new CustomThreat());
        model.addAttribute("scale", new TreeMap<>(scaleService.getScale()));
        model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());
        model.addAttribute("probabilityExplainer", scaleService.getScaleProbabilityNumberExplainer());
        model.addAttribute("consequenceExplainer", scaleService.getScaleConsequenceNumberExplainer());
        model.addAttribute("riskScoreExplainer", scaleService.getScaleRiskScoreExplainer());
        model.addAttribute("tasks", taskService.buildRelatedTasks(threatAssessment, false));

        final Document document = new Document();
        document.setDocumentType(DocumentType.PROCEDURE);
        model.addAttribute("document", document);
        return "risks/view";
    }

    @GetMapping("{id}/profile")
    public String riskProfile(final Model model, @PathVariable final long id) {
        final ThreatAssessment threatAssessment = threatAssessmentDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("risk", threatAssessment);
        final Relatable relatable = findElement(threatAssessment);
        model.addAttribute("elementName", relatable == null ? null : relatable.getName());
        model.addAttribute("reversedScale", scaleService.getScale().keySet().stream().sorted(Collections.reverseOrder()).collect(Collectors.toList()));
        model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());
        model.addAttribute("riskProfiles", riskService.buildRiskProfileDTOs(threatAssessment));
        model.addAttribute("riskScoreColorMap", scaleService.getScaleRiskScoreColorMap());

        return "risks/profile";
    }

    @DeleteMapping("{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional
    public void riskDelete(@PathVariable final String id) {
        final Long lid = Long.valueOf(id);
        relationService.deleteRelatedTo(lid);
        threatAssessmentDao.deleteById(lid);
    }

    @Transactional
    @PostMapping("{id}/customthreats/create")
    public String formCreateCustomThreat(@PathVariable final long id, @Valid @ModelAttribute final CustomThreat customThreat) {
        final ThreatAssessment threatAssessment = threatAssessmentDao.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        customThreat.setThreatAssessment(threatAssessment);
        threatAssessment.getCustomThreats().add(customThreat);
        threatAssessmentDao.save(threatAssessment);

        return "redirect:/risks/" + id;
    }

    private void inheritRisk(final ThreatAssessment savedThreatAssesment, final List<Asset> assets) {
        final RiskDTO riskDTO = riskService.calculateRiskFromRegisters(assets.stream().map(Relatable::getId).collect(Collectors.toList()));
        savedThreatAssesment.setInheritedConfidentialityRegistered(riskDTO.getRf());
        savedThreatAssesment.setInheritedIntegrityRegistered(riskDTO.getRi());
        savedThreatAssesment.setInheritedAvailabilityRegistered(riskDTO.getRt());
        savedThreatAssesment.setInheritedConfidentialityOrganisation(riskDTO.getOf());
        savedThreatAssesment.setInheritedIntegrityOrganisation(riskDTO.getOi());
        savedThreatAssesment.setInheritedAvailabilityOrganisation(riskDTO.getOt());

        for (final ThreatCatalogThreat threat : savedThreatAssesment.getThreatCatalog().getThreats()) {
            final ThreatAssessmentResponse response = new ThreatAssessmentResponse();
            response.setConfidentialityRegistered(riskDTO.getRf());
            response.setIntegrityRegistered(riskDTO.getRi());
            response.setAvailabilityRegistered(riskDTO.getRt());
            response.setConfidentialityOrganisation(riskDTO.getOf());
            response.setIntegrityOrganisation(riskDTO.getOi());
            response.setAvailabilityOrganisation(riskDTO.getOt());
            response.setMethod(ThreatMethod.NONE);
            response.setThreatCatalogThreat(threat);
            response.setThreatAssessment(savedThreatAssesment);
            savedThreatAssesment.getThreatAssessmentResponses().add(response);
        }
        threatAssessmentDao.save(savedThreatAssesment);
    }

    private void createRelation(final Long threatAssesmentId, final RelationType type, final Long relationBId) {
        final Relation relation = new Relation();
        relation.setRelationAType(RelationType.THREAT_ASSESSMENT);
        relation.setRelationAId(threatAssesmentId);
        relation.setRelationBType(type);
        relation.setRelationBId(relationBId);
        relationDao.save(relation);
    }

    private Relatable findElement(final ThreatAssessment threatAssessment) {
        if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.ASSET)) {
            final List<Relation> relations = relationDao.findRelatedToWithType(threatAssessment.getId(), RelationType.ASSET);
            if (relations.size() != 1) {
                log.error("None or more than one relation with type ASSET related to threatAssessment with id " + threatAssessment.getId() + ". Expected exactly one.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der er ingen eller mere end et Aktiv knyttet til risikovurderingen. Der skal være præcis en tilknytning.");
            }

            final Relation relation = relations.get(0);
            long assetId = 0;
            if (relation.getRelationAType().equals(RelationType.ASSET)) {
                assetId = relation.getRelationAId();
            } else if (relation.getRelationBType().equals(RelationType.ASSET)) {
                assetId = relation.getRelationBId();
            }
            return assetDao.findById(assetId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kunne ikke finde det tilknyttede aktiv."));
        }
        else if (threatAssessment.getThreatAssessmentType().equals(ThreatAssessmentType.REGISTER)) {
            final List<Relation> relations = relationDao.findRelatedToWithType(threatAssessment.getId(), RelationType.REGISTER);
            if (relations.size() != 1) {
                log.error("None or more than one relation with type REGISTER related to threatAssessment with id " + threatAssessment.getId() + ". Expected exactly one.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der er ingen eller mere end en behandlingsaktivitet knyttet til risikovurderingen. Der skal være præcis en tilknytning.");
            }

            final Relation relation = relations.get(0);
            long registerId = 0;
            if (relation.getRelationAType().equals(RelationType.REGISTER)) {
                registerId = relation.getRelationAId();
            } else if (relation.getRelationBType().equals(RelationType.REGISTER)) {
                registerId = relation.getRelationBId();
            }
            return registerDao.findById(registerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kunne ikke finde den tilknyttede behandlingsaktivitet."));
        } else {
            return null;
        }
    }

    private String getMessage(final Task task) {
        final String url = environment.getProperty("di.saml.sp.baseUrl") + "/tasks/" +  task.getId();
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<p>Kære " + task.getResponsibleUser().getName() + "</p>");
        stringBuilder.append("<p>Du er blevet tildelt opgaven med navn: \"" + task.getName());
        stringBuilder.append("\", da du er risikoejer på en ny risikovurdering.</p>");
        stringBuilder.append("<p>Du kan finde opgaven her: <a href=\"" + url + "\">" + url + "</a>");
        return stringBuilder.toString();
    }
}
