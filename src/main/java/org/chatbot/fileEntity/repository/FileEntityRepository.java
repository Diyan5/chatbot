package org.chatbot.fileEntity.repository;

import org.chatbot.fileEntity.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileEntityRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByActiveTrue();
}
