package com.devops.accessportal.service;

import com.devops.accessportal.entity.AccessRequest;
import com.devops.accessportal.entity.ReviewHistory;
import com.devops.accessportal.entity.User;
import com.devops.accessportal.repository.ReviewHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewHistoryRepository reviewHistoryRepository;
    private final AccessRequestService accessRequestService;

    public ReviewService(
            ReviewHistoryRepository reviewHistoryRepository,
            AccessRequestService accessRequestService) {

        this.reviewHistoryRepository = reviewHistoryRepository;
        this.accessRequestService = accessRequestService;
    }

    public ReviewHistory reviewRequest(
            Long requestId,
            User reviewer,
            String action,
            String remarks) {

        if (!action.equals("APPROVED") &&
                !action.equals("REJECTED")) {

            throw new IllegalArgumentException(
                    "Invalid review action");
        }

        AccessRequest request =
                accessRequestService.getRequestById(requestId);

        request.setStatus(action);
        request.setRemarks(remarks);

        ReviewHistory review = new ReviewHistory(
                request,
                reviewer,
                action,
                remarks
        );

        return reviewHistoryRepository.save(review);
    }

    public List<ReviewHistory> getReviewHistory(
            AccessRequest request) {

        return reviewHistoryRepository
                .findByAccessRequest(request);
    }
}