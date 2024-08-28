package dk.digitalidentity.controller.rest;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.StandardSection;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.view.ResponsibleUserView;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.DocumentService;
import dk.digitalidentity.service.RegisterService;
import dk.digitalidentity.service.RelatableService;
import dk.digitalidentity.service.ResponsibleUserViewService;
import dk.digitalidentity.service.StandardSectionService;
import dk.digitalidentity.service.SupplierService;
import dk.digitalidentity.service.TaskService;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("rest/admin")
@RequireAdminstrator
@RequiredArgsConstructor
public class AdminRestController {
    private final UserService userService;
    private final ResponsibleUserViewService responsibleUserViewService;
    private final RelatableService relatableService;
    private final AssetService assetService;
    private final DocumentService documentService;
    private final RegisterService registerService;
    private final StandardSectionService standardSectionService;
    private final SupplierService supplierService;
    private final TaskService taskService;
    private final ThreatAssessmentService threatAssessmentService;

    public record TransferResponsibilityDTO(String transferFrom, String transferTo) {}
    @Transactional
    @PostMapping("transferresponsibility")
    public ResponseEntity<?> mailReportToSystemOwner(@RequestBody final TransferResponsibilityDTO dto) {
        User userTo = userService.findByUuid(dto.transferTo).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ResponsibleUserView userFrom = responsibleUserViewService.findByUserUuid(dto.transferFrom);

        // null means the person has no responsibilities
        if (userFrom == null) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        List<Long> ids = userFrom.getResponsibleRelatableIds().stream().map(Long::parseLong).collect(Collectors.toList());
        List<Relatable> relatables = relatableService.findAllById(ids);

        for (Relatable responsibleFor : relatables) {
            switch (responsibleFor.getRelationType()) {
                case ASSET:
                    Asset asset = (Asset) responsibleFor;
                    asset.getResponsibleUsers().removeIf(u -> u.getUuid().equals(dto.transferFrom));
                    asset.getResponsibleUsers().add(userTo);
                    assetService.save(asset);
                    break;
                case DOCUMENT:
                    Document document = (Document) responsibleFor;
                    document.setResponsibleUser(userTo);
                    documentService.update(document);
                    break;
                case REGISTER:
                    Register register = (Register) responsibleFor;
                    register.getResponsibleUsers().removeIf(u -> u.getUuid().equals(dto.transferFrom));
                    register.getResponsibleUsers().add(userTo);
                    registerService.save(register);
                    break;
                case STANDARD_SECTION:
                    StandardSection standardSection = (StandardSection) responsibleFor;
                    standardSection.setResponsibleUser(userTo);
                    standardSectionService.save(standardSection);
                    break;
                case SUPPLIER:
                    Supplier supplier = (Supplier) responsibleFor;
                    supplier.setResponsibleUser(userTo);
                    supplierService.save(supplier);
                    break;
                case TASK:
                    Task task = (Task) responsibleFor;
                    task.setResponsibleUser(userTo);
                    taskService.saveTask(task);
                    break;
                case THREAT_ASSESSMENT:
                    ThreatAssessment threatAssessment = (ThreatAssessment) responsibleFor;
                    threatAssessment.setResponsibleUser(userTo);
                    threatAssessmentService.save(threatAssessment);
                    break;
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
