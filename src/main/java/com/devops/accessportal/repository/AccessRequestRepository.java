package com.devops.accessportal.repository;

import com.devops.accessportal.entity.AccessRequest;
import com.devops.accessportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessRequestRepository
        extends JpaRepository<AccessRequest, Long> {

    List<AccessRequest> findByUser(User user);

    List<AccessRequest> findByStatus(String status);
}