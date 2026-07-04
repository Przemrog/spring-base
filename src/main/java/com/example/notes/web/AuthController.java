package com.example.notes.web;

import com.example.notes.model.User;
import com.example.notes.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    // POST /login obsluguje Spring Security (loginProcessingUrl) - tu tylko strona.
    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String registerForm() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String email, @RequestParam String password, Model model) {
        // [OWASP A07] brak polityki zlozonosci hasla - akceptowane dowolne haslo (wariant bazowy)
        if (users.existsByEmail(email)) {
            model.addAttribute("error", "Konto o tym adresie juz istnieje.");
            return "register";
        }
        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setRole("USER");
        users.save(u);
        return "redirect:/login";
    }
}
