package ai.typesafe.voicebrowser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ai.typesafe.voicebrowser.model.User;
import ai.typesafe.voicebrowser.service.DatabaseService;
import jakarta.servlet.http.HttpSession;

/**
 * Spring MVC Controller providing endpoints for JSP-rendered authentication views.
 */
@Controller
public class AuthController {

    private final DatabaseService dbService;

    public AuthController(DatabaseService dbService) {
        this.dbService = dbService;
    }

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "logout", required = false) String logout,
            Model model
    ) {
        if ("true".equalsIgnoreCase(registered)) {
            model.addAttribute("successMessage", "Account created successfully! Please sign in.");
        }
        if ("true".equalsIgnoreCase(logout)) {
            model.addAttribute("infoMessage", "You have been signed out.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session,
            Model model
    ) {
        if (username.isBlank() || password.isBlank()) {
            model.addAttribute("errorMessage", "Username and password cannot be empty.");
            return "login";
        }

        User user = dbService.authenticate(username, password);
        if (user != null) {
            session.setAttribute("currentUser", user);
            return "redirect:/portal";
        } else {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
            model.addAttribute("username", username);
            return "login";
        }
    }

    @GetMapping("/signup")
    public String showSignupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String handleSignup(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model
    ) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            model.addAttribute("errorMessage", "All fields are required.");
            return "signup";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "signup";
        }

        if (dbService.userExists(username)) {
            model.addAttribute("errorMessage", "Username '" + username + "' is already taken.");
            model.addAttribute("email", email);
            return "signup";
        }

        boolean created = dbService.registerUser(username, email, password);
        if (created) {
            return "redirect:/login?registered=true";
        } else {
            model.addAttribute("errorMessage", "Failed to create account. Please try again.");
            return "signup";
        }
    }

    @GetMapping("/portal")
    public String showPortal(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", currentUser);
        model.addAttribute("allUsers", dbService.getAllUsers());
        return "portal";
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
