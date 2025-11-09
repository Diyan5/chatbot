package org.chatbot.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.chatbot.configuration.FlowConfigService;
import org.chatbot.fileEntity.model.FileEntity;
import org.chatbot.fileEntity.service.FileEntityService;
import org.chatbot.flow.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FileEntityControllerTest {

    private FileEntityService fileService;
    private FlowConfigService flowConfigService;
    private ObjectMapper mapper;
    private FileEntityController controller;

    @BeforeEach
    void setUp() {
        fileService = mock(FileEntityService.class);
        flowConfigService = mock(FlowConfigService.class);
        mapper = new ObjectMapper();
        controller = new FileEntityController(fileService, flowConfigService, mapper);
    }

    @Test
    void testUploadReturnsErrorWhenMetaNameMissing() throws Exception {
        ObjectNode json = mapper.createObjectNode();
        ObjectNode meta = mapper.createObjectNode();
        json.set("meta", meta);
        ResponseEntity<?> response = controller.uploadChatMessageConfiguration(json);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(((Map<?, ?>) response.getBody()).get("message").toString().contains("meta.name"));
    }

    @Test
    void testUploadSuccess() throws Exception {
        ObjectNode json = mapper.createObjectNode();
        ObjectNode meta = mapper.createObjectNode();
        meta.put("name", "TestFlow");
        json.set("meta", meta);

        FileEntity saved = FileEntity.builder().id(1L).name("TestFlow").build();
        when(fileService.saveMessage(any(), any())).thenReturn(saved);
        when(flowConfigService.getFlow()).thenReturn(new Flow());

        ResponseEntity<?> response = controller.uploadChatMessageConfiguration(json);

        assertEquals(200, response.getStatusCodeValue());
        verify(fileService).saveMessage(json, "TestFlow");
        assertEquals("success", ((Map<?, ?>) response.getBody()).get("status"));
    }

    @Test
    void testGetConfigWhenNotFound() {
        when(fileService.getActiveJson()).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getChatMessageConfiguration();

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testGetConfigWhenFound() {
        String json = "{\"meta\":{\"name\":\"TestFlow\"}}";
        when(fileService.getActiveJson()).thenReturn(Optional.of(json));

        ResponseEntity<?> response = controller.getChatMessageConfiguration();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(json, response.getBody());
    }
}
