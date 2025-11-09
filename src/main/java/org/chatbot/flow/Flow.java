package org.chatbot.flow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;


// Represents the entire conversational flow of the chatbot.
// Contains the initial block and all other blocks from the JSON file.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Flow {

    private String startBlockId;
    private List<Block> blocks;

    public Block byId(String id) {
        if (blocks == null || id == null) {
            return null;
        }
        for (Block b : blocks) {
            if (id.equals(b.id())) {
                return b;
            }
        }
        return null;
    }
}