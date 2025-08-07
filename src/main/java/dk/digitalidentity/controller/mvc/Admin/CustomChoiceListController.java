package dk.digitalidentity.controller.mvc.Admin;

import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.security.annotations.sections.RequireAdmin;
import dk.digitalidentity.security.Roles;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequireAdmin
@RequestMapping("admin/choicelists")
@RequiredArgsConstructor
public class CustomChoiceListController {

    private final ChoiceService choiceService;
    private final AssetService assetService;
    private final RegisterService registerService;

    record CustomChoiceListDTO(Long id, String name, boolean multipleSelect) {}
    @GetMapping()
    public String customChoiceListsIndex(Model model) {

        List<ChoiceList> customChoiceLists = choiceService.getAllCustomizableChoiceLists();
        model.addAttribute("choiceLists", customChoiceLists.stream().map(choiceList -> new CustomChoiceListDTO(choiceList.getId(), choiceList.getName(), choiceList.getMultiSelect())).toList() );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("isSuperuser", authentication.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals(Roles.SUPERUSER)));
        return "admin/custom_choice_lists";
    }

    record ChoiceListValueDTO(long id, String caption, String description, boolean removable){}
    record EditableCustomChoiceList(long id, String name, boolean multiSelectable, List<ChoiceListValueDTO> values){}
    @GetMapping("{id}/edit")
    public String editChoiceListFragment (Model model, @PathVariable long id) {
        ChoiceList choiceList = choiceService.findChoiceList(id)
            .orElseThrow();


        model.addAttribute("choiceList", new EditableCustomChoiceList(
            choiceList.getId(),
            choiceList.getName(),
            choiceList.getMultiSelect(),
            choiceList.getValues().stream().map(choiceValue -> new ChoiceListValueDTO(choiceValue.getId(), choiceValue.getCaption(), choiceValue.getDescription(), !isInUse(choiceValue))).toList()
        ));
        return "admin/fragments/custom_choice_list_edit :: customChoiceListEditModal";
    }

	private boolean isInUse(ChoiceValue choiceValue) {
		return assetService.isInUseOnAssets(choiceValue.getId()) || registerService.isInUseOnConsequenceAssessment(choiceValue.getId());
	}
}
