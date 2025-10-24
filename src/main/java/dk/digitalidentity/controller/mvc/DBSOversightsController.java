package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireAsset;
import dk.digitalidentity.service.ChoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Controller
@RequireAsset
@RequestMapping("dbs/oversight")
@RequiredArgsConstructor
public class DBSOversightsController {

	private final ChoiceService choiceService;

	@RequireReadOwnerOnly
    @GetMapping
    public String assetsList(Model model) {
		ChoiceList list = choiceService.findChoiceList("supervision-model").orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find Supervision Model Choices"));
		List<ChoiceValue> values = list.getValues().stream().toList();
		model.addAttribute("supervisions", values);
        return "dbs/oversight/index";
    }

}
