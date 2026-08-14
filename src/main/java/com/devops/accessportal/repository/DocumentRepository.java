package com.devops.accessportal.repository;

import com.devops.accessportal.entity.AccessRequest;
import com.devops.accessportal.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository
        extends JpaRepository<Document, Long> {

    List<Document> findByAccessRequest(AccessRequest accessRequest);
}