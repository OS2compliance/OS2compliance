package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.ChoiceDPIADao;
import dk.digitalidentity.model.dto.DataProtectionImpactDTO;
import dk.digitalidentity.model.dto.DataProtectionImpactScreeningAnswerDTO;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.ChoiceDPIA;
import dk.digitalidentity.model.entity.DPIA;
import dk.digitalidentity.model.entity.DataProtectionImpactScreeningAnswer;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.DPIAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Slf4j
@Controller
@RequestMapping("dpia")
@RequireUser
@RequiredArgsConstructor
public class DPIAController {
    private final DPIAService dpiaService;
    private final ChoiceDPIADao choiceDPIADao;

    @GetMapping
    public String dpiaList(final Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("superuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));
        return "dpia/index";
    }

    public record DPIADetailDTO (long id, long assetId, String assetName) {}
    @GetMapping("{id}")
    public String dpiaList(final Model model, @PathVariable Long id) {
        DPIA dpia = dpiaService.find(id);
        Asset asset = dpia.getAsset();

        // DPIA
        final List<DataProtectionImpactScreeningAnswerDTO> assetDPIADTOs = new ArrayList<>();
        final List<ChoiceDPIA> choiceDPIA = choiceDPIADao.findAll();
        for (final ChoiceDPIA choice : choiceDPIA) {
            final DataProtectionImpactScreeningAnswer defaultAnswer = new DataProtectionImpactScreeningAnswer();
            defaultAnswer.setAssessment(asset.getDpiaScreening());
            defaultAnswer.setChoice(choice);
            defaultAnswer.setAnswer(null);
            defaultAnswer.setId(0);
            final DataProtectionImpactScreeningAnswer dpiaAnswer = asset.getDpiaScreening().getDpiaScreeningAnswers().stream()
                    .filter(m -> Objects.equals(m.getChoice().getId(), choice.getId()))
                    .findAny().orElse(defaultAnswer);
            final DataProtectionImpactScreeningAnswerDTO dpiaDTO = new DataProtectionImpactScreeningAnswerDTO();
            dpiaDTO.setAssetId(asset.getId());
            dpiaDTO.setAnswer(dpiaAnswer.getAnswer());
            dpiaDTO.setChoice(choice);
            assetDPIADTOs.add(dpiaDTO);
        }
        final DataProtectionImpactDTO dpiaForm = DataProtectionImpactDTO.builder()
                .assetId(asset.getId())
                .optOut(asset.isDpiaOptOut())
                .questions(assetDPIADTOs)
                .consequenceLink(asset.getDpiaScreening().getConsequenceLink())
                .dpiaQuality(asset.getDpia() == null ? new HashSet<>() : asset.getDpia().getChecks())
                .comment(asset.getDpia() == null ? "" : asset.getDpia().getComment())
                .build();

        model.addAttribute("dpia", new DPIADetailDTO(dpia.getId(), asset.getId(), asset.getName()));
        model.addAttribute("asset", asset);


        model.addAttribute("dpiaForm", dpiaForm);

        return "dpia/details";
    }
}
