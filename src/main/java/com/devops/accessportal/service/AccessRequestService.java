package com.devops.accessportal.service;

import com.devops.accessportal.entity.AccessRequest;
import com.devops.accessportal.entity.User;
import com.devops.accessportal.repository.AccessRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessRequestService {

    private final AccessRequestRepository accessRequestRepository;

    public AccessRequestService(
            AccessRequestRepository accessRequestRepository) {

        this.accessRequestRepository = accessRequestRepository;
    }

    public AccessRequest createRequest(AccessRequest request) {

        request.setStatus("PENDING");

        return accessRequestRepository.save(request);
    }

    public List<AccessRequest> getRequestsByUser(User user) {

        return accessRequestRepository.findByUser(user);
    }

    public List<AccessRequest> getPendingRequests() {

        return accessRequestRepository.findByStatus("PENDING");
    }

    public AccessRequest getRequestById(Long requestId) {

        return accessRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Access request not found"));
    }

    public AccessRequest updateStatus(
            Long requestId,
            String status,
            String remarks) {

        AccessRequest request = getRequestById(requestId);

        request.setStatus(status);
        request.setRemarks(remarks);

        return accessRequestRepository.save(request);
    }
}