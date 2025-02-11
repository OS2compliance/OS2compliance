package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.Precaution;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.security.RequireAdministrator;
import dk.digitalidentity.service.PrecautionService;
import dk.digitalidentity.service.RelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("precautions")
@RequireAdministrator
@RequiredArgsConstructor
public class PrecautionController {
    private final PrecautionService precautionService;
    private final RelationService relationService;

    record AssetRelationDTO(long precautionId, String assetName, long assetId) {}
    @GetMapping
    public String precautionList(final Model model) {
        final List<Precaution> precautions = precautionService.findAll();
        model.addAttribute("precautions", precautions);

        final List<AssetRelationDTO> allRelatedToPrecaution = new ArrayList<>();
        for (Precaution precaution : precautions) {
            allRelatedToPrecaution.addAll(relationService.findAllRelatedTo(precaution).stream().filter(r -> r.getRelationType().equals(RelationType.ASSET)).map( r -> new AssetRelationDTO(precaution.getId(), r.getName(), r.getId())).collect(Collectors.toList()));
        }
        model.addAttribute("relatedAssets", allRelatedToPrecaution);

        return "precautions/index";
    }

    @GetMapping("form")
    public String form(final Model model, @RequestParam(name = "id", required = false) final Long id) {
        model.addAttribute("action", "precautions/form");
        if (id == null) {
            model.addAttribute("precaution", new Precaution());
            model.addAttribute("formId", "createForm");
            model.addAttribute("formTitle", "Ny foranstaltning");
        } else {
            final Precaution precaution = precautionService.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("precaution", precaution);
            model.addAttribute("formId", "editForm");
            model.addAttribute("formTitle", "Rediger foranstaltning");
        }
        return "precautions/form";
    }

    @Transactional
    @PostMapping("form")
    public String formPost(@ModelAttribute final Precaution precaution) {
        if (precaution.getId() != null) {
            final Precaution existingPrecaution = precautionService.get(precaution.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            existingPrecaution.setName(precaution.getName());
            existingPrecaution.setDescription(precaution.getDescription());
            precautionService.save(existingPrecaution);
        } else {
            precautionService.save(precaution);
        }
        return "redirect:/precautions";
    }
}
