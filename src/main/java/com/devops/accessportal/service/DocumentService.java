package com.devops.accessportal.service;

import com.devops.accessportal.entity.AccessRequest;
import com.devops.accessportal.entity.Document;
import com.devops.accessportal.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document saveDocument(Document document) {

        return documentRepository.save(document);
    }

    public List<Document> getDocumentsForRequest(
            AccessRequest accessRequest) {

        return documentRepository.findByAccessRequest(accessRequest);
    }

    public boolean validateDocument(Document document) {

        if (document.getFileName() == null ||
                document.getFileName().isBlank()) {

            return false;
        }

        if (document.getFileType() == null ||
                document.getFileType().isBlank()) {

            return false;
        }

        document.setValidated(true);

        documentRepository.save(document);

        return true;
    }
}