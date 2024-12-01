package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.util.StringUtils;

import java.util.Optional;

@Controller
@RequestMapping()
@RequiredArgsConstructor
class LoginController {
    private final UserService userService;

    @GetMapping("login")
    String login() {
        return "index";
    }

    @GetMapping("forgotten")
    String forgotten() {
        return "forgotten";
    }

    @GetMapping("reset/{resetToken}")
    String reset(final Model model, @PathVariable final String resetToken) {
        Optional<User> user = userService.findNonExpiredByPasswordResetToken(resetToken);
        if (user.isPresent()) {
            model.addAttribute("user", user.get());
        } else {
            return "reset_password_expired";
        }
        return "reset_password";
    }

    public record PasswordChangeRequest(String password1, String password2) {}
    @PostMapping("reset/{resetToken}")
    String resetForm(final Model model, @PathVariable final String resetToken, final PasswordChangeRequest passwordChangeRequest) {
        final Optional<User> user = userService.findNonExpiredByPasswordResetToken(resetToken);
        if (user.isEmpty()) {
            return "reset_password_expired";
        }
        model.addAttribute("user", user.get());
        if (!StringUtils.equals(passwordChangeRequest.password1, passwordChangeRequest.password2)) {
            model.addAttribute("mismatch", true);
            return "reset_password";
        }
        if (!userService.isValidPassword(passwordChangeRequest.password1)) {
            model.addAttribute("policy", true);
            return "reset_password";
        }
        userService.setPassword(user.get(), passwordChangeRequest.password1);
        return "redirect:/";
    }

    public record ResetRequest(@NotEmpty String email) {}
    @PostMapping("forgotten")
    String forgottenForm(@Valid ResetRequest request) {
        userService.findByEmail(request.email)
            .ifPresent(userService::sendForgottenPasswordMail);
        return "forgotten_sent";
    }
}
