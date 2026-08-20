package com.devops.accessportal.controller;

import com.devops.accessportal.entity.AccessRequest;
import com.devops.accessportal.service.AccessRequestService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ReviewerController {

    private final AccessRequestService accessRequestService;

    public ReviewerController(
            AccessRequestService accessRequestService) {

        this.accessRequestService = accessRequestService;
    }

    // Reviewer dashboard
    @GetMapping("/reviewer/dashboard")
    public String reviewerDashboard(Model model) {

        List<AccessRequest> pendingRequests =
                accessRequestService.getPendingRequests();

        model.addAttribute("requests", pendingRequests);

        return "reviewer-dashboard";
    }

    // Show details of a particular request
    @GetMapping("/reviewer/request")
    public String reviewRequest(
            @RequestParam Long requestId,
            Model model) {

        AccessRequest request =
                accessRequestService.getRequestById(requestId);

        model.addAttribute("request", request);

        return "review-request";
    }

    // Approve or reject request
    @PostMapping("/reviewer/request/update")
    public String updateRequest(
            @RequestParam Long requestId,
            @RequestParam String status,
            @RequestParam(required = false) String remarks) {

        accessRequestService.updateStatus(
                requestId,
                status,
                remarks
        );

        return "redirect:/reviewer/dashboard";
    }
}