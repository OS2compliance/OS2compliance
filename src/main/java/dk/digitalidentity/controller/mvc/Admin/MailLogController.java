package dk.digitalidentity.controller.mvc.Admin;

import dk.digitalidentity.security.annotations.crud.RequireReadAll;
import dk.digitalidentity.security.annotations.sections.RequireConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequireConfiguration
@RequestMapping("admin/log/mail")
@RequiredArgsConstructor
public class MailLogController {
	enum PAGE {
		MAIL_LOG("admin/log/mail/list") ;

		String path;
		PAGE(String path) {
			this.path = path;
		}
	}

	@RequireReadAll
	@GetMapping("list")
	public String editChoiceListFragment (Model model) {

		return PAGE.MAIL_LOG.path;
	}
}
