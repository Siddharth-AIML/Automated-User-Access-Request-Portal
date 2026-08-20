package com.devops.accessportal.controller;

import com.devops.accessportal.entity.AccessRequest;
import com.devops.accessportal.entity.User;
import com.devops.accessportal.repository.UserRepository;
import com.devops.accessportal.service.AccessRequestService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AccessRequestController {

    private final AccessRequestService accessRequestService;
    private final UserRepository userRepository;

    public AccessRequestController(
            AccessRequestService accessRequestService,
            UserRepository userRepository) {

        this.accessRequestService = accessRequestService;
        this.userRepository = userRepository;
    }

    @GetMapping("/employee/requests/new")
    public String showRequestForm() {
        return "submit-request";
    }

    @PostMapping("/employee/requests")
    public String submitRequest(
            @RequestParam String resourceName,
            @RequestParam String accessType,
            @RequestParam String justification,
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found"));

        AccessRequest request =
                accessRequestService.createRequest(
                        resourceName,
                        accessType,
                        justification,
                        user
                );

        model.addAttribute("request", request);

        return "request-success";
    }
}