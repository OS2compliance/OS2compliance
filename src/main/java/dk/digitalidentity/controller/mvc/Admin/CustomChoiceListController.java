package dk.digitalidentity.controller.mvc.Admin;

import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireAdmin;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.ChoiceValueService;
import dk.digitalidentity.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequireAdmin
@RequestMapping("admin/choicelists")
@RequiredArgsConstructor
public class CustomChoiceListController {

    private final ChoiceService choiceService;
    private final AssetService assetService;
    private final RegisterService registerService;
	private final ChoiceValueService choiceValueService;

	record CustomChoiceListDTO(Long id, String name, boolean multipleSelect) {}
	@RequireReadAll
    @GetMapping()
    public String customChoiceListsIndex(Model model) {

        List<ChoiceList> customChoiceLists = choiceService.getAllCustomizableChoiceLists();
        model.addAttribute("choiceLists", customChoiceLists.stream().map(choiceList -> new CustomChoiceListDTO(choiceList.getId(), choiceList.getName(), choiceList.getMultiSelect())).toList() );

        model.addAttribute("isSuperuser",SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL));
        return "admin/choicelist/custom_choice_lists";
    }

	public record ChoiceValueDTO(long id, String caption, String description, boolean editable) {}
	@RequireReadAll
	@GetMapping("/choice/view/{id}")
	public String customChoiceList(Model model, @PathVariable long id) {
		ChoiceList list = choiceService.findChoiceList(id).orElse(null);
		Set<ChoiceValueDTO> collect = list.getValues().stream().map(choiceValue -> {
			return new ChoiceValueDTO(choiceValue.getId(), choiceValue.getCaption(), choiceValue.getDescription(), !isInUse(choiceValue));
		}).collect(Collectors.toSet());
		model.addAttribute("choiceList", list);

		model.addAttribute("choiceValues", collect);
		return "admin/choicelist/choice_list_view";
	}

	@RequireUpdateAll
	@PostMapping("/{choiceValueId}/delete/{choiceListId}")
	public String deleteChoiceValue(@PathVariable Long choiceValueId, @PathVariable Long choiceListId) {

		ChoiceValue choiceValue = choiceValueService.findById(choiceValueId).orElse(null);
		if (choiceValue == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not find choiceValue");
		}

		choiceValueService.delete(choiceValue);

		return "redirect:/admin/choicelists/" + choiceListId;
	}

	private boolean isInUse(ChoiceValue choiceValue) {
		if (!choiceValue.isEditable()) {
			return true;
		}
		return assetService.isInUseOnAssets(choiceValue.getId()) || registerService.isInUseOnConsequenceAssessment(choiceValue.getId()) || registerService.isInUseByChoiceValue(choiceValue.getId());
	}
}
