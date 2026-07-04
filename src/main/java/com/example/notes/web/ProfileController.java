package com.example.notes.web;

import com.example.notes.model.User;
import com.example.notes.repo.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;

@Controller
public class ProfileController {

    private final UserRepository users;
    public ProfileController(UserRepository users) { this.users = users; }

    private User current(Principal principal) {
        return users.findByEmail(principal.getName()).orElseThrow();
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("user", current(principal));
        return "profile";
    }

    @PostMapping("/profile")
    public String upload(@RequestParam("avatar") MultipartFile avatar, Principal principal) throws Exception {
        User u = current(principal);
        if (avatar != null && !avatar.isEmpty()) {
            // [OWASP A02/A08] brak walidacji typu/rozszerzenia i uzycie oryginalnej nazwy pliku - celowo
            Path dir = Paths.get("uploads");
            Files.createDirectories(dir);
            Path target = dir.resolve(avatar.getOriginalFilename());
            avatar.transferTo(target.toAbsolutePath());
            u.setAvatarPath("/avatars/" + avatar.getOriginalFilename());
            users.save(u);
        }
        return "redirect:/profile";
    }
}
