package dk.digitalidentity.controller.mvc.Admin;

import dk.digitalidentity.model.dto.enums.AllowedAction;
import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
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

	public record ChoiceValueDTO(long id, String caption, String description, Set<AllowedAction> allowedActions) {}
	@RequireReadAll
	@GetMapping("/choice/view/{id}")
	public String customChoiceList(Model model, @PathVariable long id) {
		ChoiceList list = choiceService.findChoiceList(id).orElse(null);
		if (list == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ChoiceList not found");
		}
		Set<AllowedAction> allowedActions = setAllowedActions();

		Set<ChoiceValueDTO> collect = list.getValues().stream().map(choiceValue -> {
			return new ChoiceValueDTO(choiceValue.getId(), choiceValue.getCaption(), choiceValue.getDescription(), allowedActions);
		}).collect(Collectors.toSet());
		model.addAttribute("choiceList", list);

		model.addAttribute("choiceValues", collect);
		return "admin/choicelist/choice_list_view";
	}

	private boolean isInUse(ChoiceValue choiceValue) {
		if (!choiceValue.isEditable()) {
			return true;
		}
		return assetService.isInUseOnAssets(choiceValue.getId()) || registerService.isInUseOnConsequenceAssessment(choiceValue.getId()) || registerService.isInUseByChoiceValue(choiceValue.getId());
	}
	private Set<AllowedAction> setAllowedActions() {
		Set<AllowedAction> allowedActions = new HashSet<>();
		if (SecurityUtil.isOperationAllowed(Roles.UPDATE_ALL)) {
			allowedActions.add(AllowedAction.UPDATE);
		}
		if (SecurityUtil.isOperationAllowed(Roles.DELETE_ALL)) {
			allowedActions.add(AllowedAction.DELETE);
		}
		return allowedActions;
	}
}
