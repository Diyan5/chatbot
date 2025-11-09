package org.chatbot.fileEntity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.chatbot.fileEntity.model.FileEntity;
import org.chatbot.fileEntity.repository.FileEntityRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileEntityServiceTest {

    @Test
    void testSaveMessageSetsActiveAndDeactivatesPrevious() {
        FileEntityRepository mockRepo = mock(FileEntityRepository.class);
        FileEntityService service = new FileEntityService(mockRepo);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode().put("meta", "test");

        FileEntity inactive = FileEntity.builder()
                .id(1L).active(true).createdAt(Instant.now()).build();

        when(mockRepo.findByActiveTrue()).thenReturn(Optional.of(inactive));
        when(mockRepo.save(Mockito.any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FileEntity result = service.saveMessage(json, "TestFlow");
        assertTrue(result.getActive());
        verify(mockRepo, times(1)).save(Mockito.any(FileEntity.class));
        assertFalse(inactive.getActive());
    }
}
