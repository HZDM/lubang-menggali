package models.player;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.WebSocket;

/**
 * Represents a pending (that is, unpaired) player.
 */
public class ConnectedPlayer extends Player {

    protected final WebSocket.In<JsonNode> inputSocket;
    protected final WebSocket.Out<JsonNode> outputSocket;

    public ConnectedPlayer(WebSocket.In<JsonNode> inputSocket, WebSocket.Out<JsonNode> outputSocket) {
        this.inputSocket = inputSocket;
        this.outputSocket = outputSocket;
    }

    public WebSocket.In<JsonNode> getInputSocket() { return inputSocket; }

    public WebSocket.Out<JsonNode> getOutputSocket() { return outputSocket; }

    public PairedPlayer upgrade(String opponentId) {
        PairedPlayer pp = new PairedPlayer(inputSocket, outputSocket, opponentId);
        pp.id = this.id;
        return pp;
    }

    @Override
    public String toString() { return String.format("ConnectedPlayer[%s]", id); }

}
