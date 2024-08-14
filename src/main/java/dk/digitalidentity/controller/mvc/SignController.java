package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.ThreatAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Slf4j
@Controller
@RequireUser
@RequestMapping("sign")
@RequiredArgsConstructor
public class SignController {
    private final ThreatAssessmentService threatAssessmentService;

    @GetMapping("{id}")
    public String signThreatAssessment(final Model model, @PathVariable("id") final long id) {
        final ThreatAssessment threatAssessment = threatAssessmentService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String approverUuid = threatAssessment.getThreatAssessmentReportApprover() != null ? threatAssessment.getThreatAssessmentReportApprover().getUuid() : null;
        if (!Objects.equals(approverUuid, SecurityUtil.getLoggedInUserUuid())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("risk", threatAssessment);
        return "sign/sign_threat";
    }
}
