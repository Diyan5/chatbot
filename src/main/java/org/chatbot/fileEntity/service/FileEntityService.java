package org.chatbot.fileEntity.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import org.chatbot.fileEntity.model.FileEntity;
import org.chatbot.fileEntity.repository.FileEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class FileEntityService {

    private final FileEntityRepository fileEntityRepository;

    @Autowired
    public FileEntityService(FileEntityRepository fileEntityRepository) {
        this.fileEntityRepository = fileEntityRepository;
    }

    @Transactional
    public FileEntity saveMessage(JsonNode config, String name) {
        fileEntityRepository.findByActiveTrue()
                .ifPresent(prev -> prev.setActive(false));

        FileEntity fileEntity = FileEntity.builder()
                .name(name)
                .active(true)
                .json(config.toString())
                .createdAt(Instant.now())
                .build();

        return fileEntityRepository.save(fileEntity);
    }

    public Optional<String> getActiveJson() {
        return fileEntityRepository.findByActiveTrue()
                .map(FileEntity::getJson);
    }
}
