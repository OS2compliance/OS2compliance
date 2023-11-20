package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.DocumentDao;
import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequireUser
public class DefaultController implements ErrorController {
	private final ErrorAttributes errorAttributes = new DefaultErrorAttributes();
    private final UserDao userDao;
    private final TaskService taskService;
    private final DocumentDao documentDao;

    public DefaultController(final UserDao userDao, final TaskService taskService, final DocumentDao documentDao) {
        this.userDao = userDao;
        this.taskService = taskService;
        this.documentDao = documentDao;
    }

    @Transactional
    @GetMapping("/")
	public String defaultPage(final Model model) {
		if (!SecurityUtil.isLoggedIn()) {
			return "redirect:/saml/login";
		}
        final var userUuid = SecurityUtil.getLoggedInUserUuid();
        if(userUuid != null) {
            final User user = userDao.findByUuidAndActiveIsTrue(userUuid);
            final List<Task> taskList = taskService.findTasksNearingDeadlines(user).stream().sorted((o1, o2) -> o1.getNextDeadline().compareTo(o2.getNextDeadline())).collect(Collectors.toList());
            model.addAttribute("tasks", taskList);
            final List<Document> documentList = documentDao.findAllByResponsibleUserAndNextRevisionBefore(user, LocalDate.now().plusDays(7)).stream().sorted((o1, o2) -> o1.getNextRevision().compareTo(o2.getNextRevision())).collect(Collectors.toList());
            model.addAttribute("documents", documentList);
            model.addAttribute("user", user);
        }

		return "index";
	}

	@RequestMapping(value = "/error", produces = "text/html")
	public String errorPage(final Model model, final HttpServletRequest request) {
		final Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));

		model.addAllAttributes(body);

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
