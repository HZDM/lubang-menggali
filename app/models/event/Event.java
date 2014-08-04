package models.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.mvc.WebSocket;

import javax.validation.constraints.NotNull;

/**
 * Base class for describing client-server messaging in JSON.
 */
abstract public class Event {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    public void write(@NotNull WebSocket.Out<JsonNode> out) {
        out.write(objectMapper.valueToTree(this));
    }

}
