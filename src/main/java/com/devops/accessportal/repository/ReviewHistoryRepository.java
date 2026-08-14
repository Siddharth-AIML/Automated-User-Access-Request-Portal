package com.devops.accessportal.repository;

import com.devops.accessportal.entity.AccessRequest;
import com.devops.accessportal.entity.ReviewHistory;
import com.devops.accessportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewHistoryRepository
        extends JpaRepository<ReviewHistory, Long> {

    List<ReviewHistory> findByAccessRequest(AccessRequest accessRequest);

    List<ReviewHistory> findByReviewer(User reviewer);
}