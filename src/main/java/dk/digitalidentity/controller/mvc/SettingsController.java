package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.integration.kitos.KitosConstants;
import dk.digitalidentity.model.dto.SettingsDTO;
import dk.digitalidentity.model.dto.enums.KitosField;
import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import dk.digitalidentity.service.KitosService;
import dk.digitalidentity.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("settings")
@RequireConfiguration
@RequiredArgsConstructor
public class SettingsController {
    private final SettingsService settingsService;
    private final HttpServletRequest httpServletRequest;
    private final KitosService kitosService;
    private final OS2complianceConfiguration configuration;

	@RequireReadAll
    @Transactional
    @GetMapping("form")
    public String form(final Model model) {
        final SettingsDTO settings = new SettingsDTO();
        settings.addList(settingsService.getByEditable());
        model.addAttribute("settings", settings);
        model.addAttribute("page", getParentType(httpServletRequest.getHeader("Referer")));

		boolean kitosEnabled = configuration.getIntegrations().getKitos().isEnabled();
		model.addAttribute("kitosEnabled", kitosEnabled);
		if (kitosEnabled) {
			model.addAttribute("kitosRoles", kitosService.kitosRoles());

			List<KitosField> fields = Arrays.asList(KitosField.values());
			model.addAttribute("kitosFieldsForAssetLinkSource", fields.stream().filter(f -> f.getForField().equals(KitosConstants.KITOS_FIELDS_ASSET_LINK_SOURCE)).collect(Collectors.toSet()));
			model.addAttribute("kitosFieldsForContractDate", fields.stream().filter(f -> f.getForField().equals(KitosConstants.KITOS_FIELDS_CONTRACT_DATE)).collect(Collectors.toSet()));
			model.addAttribute("kitosFieldsForContractEnd", fields.stream().filter(f -> f.getForField().equals(KitosConstants.KITOS_FIELDS_CONTRACT_END)).collect(Collectors.toSet()));
		}


        return "fragments/settings";
    }

    @RequireUpdateAll
    @Transactional
    @PostMapping("update")
    public String update(@ModelAttribute final SettingsDTO settings) {
        //For notifications, change null values to "false", to ensure changes are not dropped
        settings.getSettingsList()
            .forEach(setting -> {
                if (setting.getSettingValue() == null && setting.getAssociation() != null && setting.getAssociation().equals("notification")) {
                    setting.setSettingValue("false");
                }
            });

        if (!settings.settingsList.isEmpty()) {
            settings.settingsList.removeIf(x -> Objects.isNull(x.getSettingValue()) || x.getSettingValue().isEmpty());
            final var res = settingsService.saveAll(settings.settingsList);
        }
        return "redirect:" + httpServletRequest.getHeader("Referer");
    }

    private String getParentType(final String url) {
        if (url.contains("standards")) return "standards";
        if (url.contains("registers")) return "registers";
        if (url.contains("assets")) return "assets";
        if (url.contains("suppliers")) return "suppliers";
        if (url.contains("risks")) return "risks";
        if (url.contains("documents")) return "documents";
        if (url.contains("tasks")) return "tasks";
        if (url.contains("reports")) return "tasks";

        return "";
    }
}
