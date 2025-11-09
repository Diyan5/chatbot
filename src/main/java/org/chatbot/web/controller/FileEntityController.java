package org.chatbot.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chatbot.configuration.FlowConfigService;
import org.chatbot.fileEntity.model.FileEntity;
import org.chatbot.fileEntity.service.FileEntityService;
import org.chatbot.flow.Flow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class FileEntityController {

    private final FileEntityService fileEntityService;
    private final FlowConfigService flowConfigService;
    private final ObjectMapper objectMapper;

    @Autowired
    public FileEntityController(FileEntityService fileEntityService,
                                FlowConfigService flowConfigService,
                                ObjectMapper objectMapper) {
        this.fileEntityService = fileEntityService;
        this.flowConfigService = flowConfigService;
        this.objectMapper = objectMapper;
    }

    // Accepts a new JSON file and saves it as the active chatbot configuration.
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadChatMessageConfiguration(@RequestBody JsonNode config) throws JsonProcessingException {
        String name = config.path("meta").path("name").asText(null);
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Field meta.name is required in the JSON"
            ));
        }

        FileEntity saved = fileEntityService.saveMessage(config, name);

        // FlowConfigService вече може сам да се справи с валидирането.
        Flow flow = objectMapper.treeToValue(config, Flow.class);
        flowConfigService.setFlow(flow);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Configuration saved successfully",
                "name", name,
                "id", saved.getId()
        ));
    }

    //returns the active JSON
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getChatMessageConfiguration() {
        return fileEntityService.getActiveJson()
                .<ResponseEntity<?>>map(json ->
                        ResponseEntity
                                .ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(json)
                )
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "status", "not_found",
                        "message", "No active configuration found"
                )));
    }

}
