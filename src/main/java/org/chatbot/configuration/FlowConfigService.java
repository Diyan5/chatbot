package org.chatbot.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.chatbot.fileEntity.service.FileEntityService;
import org.chatbot.flow.Flow;
import org.springframework.stereotype.Service;

import java.util.Optional;

//Service that keeps the active flow in memory.
//On startup, it loads the JSON configuration from the database, deserializes it into a Flow object, and caches it.
//Using the setFlow(...) method, the flow can be changed at runtime.
@Service
@Slf4j
public class FlowConfigService {

    private final FileEntityService fileEntityService;
    private final ObjectMapper mapper;

    // Active configuration of the chatbot.
    // volatile ensures that all threads see the latest version of Flow.
    private volatile Flow current;

    public FlowConfigService(FileEntityService fileEntityService) {
        this.fileEntityService = fileEntityService;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Flow getFlow() {
        return current;
    }

    public void setFlow(Flow flow) {
        this.current = flow;
        String startId = (flow != null) ? flow.getStartBlockId() : null;
        int blocks = (flow != null && flow.getBlocks() != null) ? flow.getBlocks().size() : 0;
        log.info("Active chatbot flow set. startBlockId={}, blocks={}", startId, blocks);
    }

    //Loads the active Flow at application startup.
    @PostConstruct
    public void loadActiveOnStartup() {
        try {
            Optional<String> activeJson = fileEntityService.getActiveJson();
            if (activeJson.isEmpty()) {
                log.warn("No active chatbot flow found on startup.");
                return;
            }
            Flow flow = mapper.readValue(activeJson.get(), Flow.class);
            setFlow(flow);
        } catch (Exception e) {
            log.error("Failed to load active chatbot flow on startup", e);
        }
    }
}
