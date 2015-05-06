package alexp.blog.controller;

import alexp.blog.model.User;
import alexp.blog.service.AuthException;
import alexp.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
public class UsersController {

    @Autowired
    private UserService userService;

    @Autowired
    private Validator userValidator;

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String showRegistrationForm(ModelMap model, HttpServletRequest request, HttpSession session) {
        model.addAttribute("user", new User());

        if (userService.isAuthenticated()) {
            return "redirect:posts";
        }

        String ref = request.getHeader("referer");

        if (ref != null && !ref.contains("/register"))
            session.setAttribute("regRef", ref);

        return "registration";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String registerUser(@Validated({User.CreateValidationGroup.class}) @ModelAttribute(value = "user") User user, BindingResult result, HttpSession session) {
        user.setUsername(StringUtils.trimWhitespace(user.getUsername()));
        user.setEmail(StringUtils.trimWhitespace(user.getEmail()));

        userValidator.validate(user, result);

        if (result.hasErrors())
        {
            return "registration";
        }

        userService.register(user);

        userService.authenticate(user);

        Object regRef = session.getAttribute("regRef");

        return "redirect:" + (StringUtils.isEmpty(regRef) ? "posts" : regRef.toString());
    }

    @RequestMapping(value = "/check_email", method = RequestMethod.GET)
    public @ResponseBody String checkEmail(@RequestParam("email") String email) {
        return userService.emailExists(email) ? "false" : "true";
    }

    @RequestMapping(value = "/check_username", method = RequestMethod.GET)
    public @ResponseBody String checkUsername(@RequestParam("username") String username) {
        return userService.usernameExists(username) ? "false" : "true";
    }

    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    public String showEditSettingsPage(ModelMap model) {
        if (!model.containsAttribute("user")) {
            User user = userService.currentUser();

            if (user == null) {
                return "redirect:posts";
            }

            model.addAttribute("user", user);
        }

        return "settings";
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "/change_email", method = RequestMethod.POST)
    public String changeEmail(@Validated({User.ChangeEmailValidationGroup.class}) @ModelAttribute(value = "user") User user, BindingResult result,
                              @RequestParam("currentPassword") String currentPassword, RedirectAttributes redirectAttributes, ModelMap model) {
        model.addAttribute("isEmailForm", true);

        userValidator.validate(user, result);

        if (!result.hasErrors()) {
            try {
                userService.changeEmail(user.getEmail(), currentPassword);
            } catch (AuthException e) {
                result.rejectValue("password", "NotMatchCurrent");
            }
        }

        if (result.hasErrors()) {
            return "settings";
        }

        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("isEmailForm", true);

        return "redirect:/settings";
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "/change_password", method = RequestMethod.POST)
    public String changePassword(@Validated({User.ChangePasswordValidationGroup.class}) @ModelAttribute(value = "user") User user, BindingResult result,
                              @RequestParam("currentPassword") String currentPassword, RedirectAttributes redirectAttributes, ModelMap model) {
        model.addAttribute("isPasswordForm", true);

        if (!result.hasErrors()) {
            try {
                userService.changePassword(user.getPassword(), currentPassword);
            } catch (AuthException e) {
                result.rejectValue("password", "NotMatchCurrent");
            }
        }

        user.setEmail(userService.currentUser().getEmail()); // quick workaround to show e-mail in the e-mail form

        if (result.hasErrors()) {
            return "settings";
        }

        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("isPasswordForm", true);

        return "redirect:/settings";
    }


}