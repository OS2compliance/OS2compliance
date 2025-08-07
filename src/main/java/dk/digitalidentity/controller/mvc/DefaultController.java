package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DefaultController implements ErrorController {
	private final ErrorAttributes errorAttributes = new DefaultErrorAttributes();
    private final UserService userService;
	private final AssetService assetService;

	@Transactional
    @GetMapping("/dashboard")
    @RequireReadOwnerOnly
	public String index(final Model model) {
        if (SecurityUtil.isLoggedIn()) {
            final var userUuid = SecurityUtil.getLoggedInUserUuid();
            if(userUuid != null) {
                final User user = userService.findByUuid(userUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                model.addAttribute("user", user);
            }


			model.addAttribute("isSystemOwner", assetService.isSystemOwnerAnywhere(userUuid));

            return "dashboard";
        }
        return "index";
    }

	@RequestMapping(value = "/error", produces = "text/html")
	public String errorPage(final Model model, final HttpServletRequest request) {
		final Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));
        model.addAllAttributes(body);
        if (model.containsAttribute("EXCEPTION")) {
            final Exception ex = (Exception) model.getAttribute("EXCEPTION");
            if (ex instanceof UsernameNotFoundException) {
                return "errors/userNotFound";
            }
        }

		return "error";
	}

	@RequestMapping(value = "/error", produces = "application/json")
	public ResponseEntity<Map<String, Object>> errorJSON(final HttpServletRequest request) {
		final Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		try {
			status = HttpStatus.valueOf((int) body.get("status"));
		} catch (final Exception ignored) { }

		return new ResponseEntity<>(body, status);
	}

	private Map<String, Object> getErrorAttributes(final WebRequest request) {
		return errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
	}
}
