package com.example.taskmanager.controller;

import com.example.taskmanager.model.Role;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthWebController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthWebController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           Model model) {
        if (email == null || email.isBlank() || password == null || password.length() < 6) {
            model.addAttribute("error", "Email обязателен, пароль минимум 6 символов");
            model.addAttribute("email", email);
            return "auth/register";
        }

        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Пользователь с таким email уже существует");
            model.addAttribute("email", email);
            return "auth/register";
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        return "redirect:/login?registered";
    }
}